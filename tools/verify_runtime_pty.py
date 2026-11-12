#!/usr/bin/env python3
"""Functional interactive-program tests in an existing disposable BootstrapFixture.

This host PTY harness is not an Android lifecycle or TerminalView test.
"""
import argparse
import fcntl
import json
import os
from pathlib import Path
import pty
import select
import signal
import struct
import subprocess
import termios
import time


def main(fixture):
    fixture = fixture.resolve()
    if not fixture.is_relative_to(Path('.local').resolve()):
        raise ValueError('Only disposable fixtures under .local are accepted')
    argv = (fixture / 'login.argv').read_text().split('\0')
    if argv[argv.index('-r') + 1] != str(fixture / 'termux'):
        raise ValueError('Fixture root mismatch')
    if '--kill-on-exit' not in argv:
        argv.insert(1, '--kill-on-exit')
    env = dict(x.split('=', 1) for x in (fixture / 'host.env').read_text().split('\0') if x)
    evidence = fixture / 'pty-acceptance'
    evidence.mkdir(exist_ok=False)
    results = []

    def record(name, ok, detail=''):
        results.append(dict(name=name, status='PASS' if ok else 'FAIL', detail=detail))
        (evidence / 'results.json').write_text(json.dumps(results, indent=2) + '\n')
        print(results[-1]['status'], name, detail, flush=True)

    def interactive(name, command, steps, expected=b'', resize=False):
        master, slave = pty.openpty()
        fcntl.ioctl(slave, termios.TIOCSWINSZ, struct.pack('HHHH', 24, 80, 0, 0))
        def child_tty():
            os.setsid()
            fcntl.ioctl(0, termios.TIOCSCTTY, 0)
        process = subprocess.Popen(argv + ['-c', command], env=env, stdin=slave, stdout=slave,
                                   stderr=slave, preexec_fn=child_tty)
        os.close(slave)
        output = bytearray()
        step = 0
        deadline = time.monotonic() + 45
        try:
            while time.monotonic() < deadline:
                ready, _, _ = select.select([master], [], [], .1)
                if ready:
                    try:
                        chunk = os.read(master, 16384)
                    except OSError:
                        chunk = b''
                    if not chunk:
                        break
                    output.extend(chunk)
                    if len(output) > 2 * 1024 * 1024:
                        raise ValueError('Interactive output exceeded evidence limit')
                if step < len(steps) and steps[step][0] in output:
                    # Allow the foreground program to finish setting raw mode/job control.
                    time.sleep(.3)
                    if resize:
                        fcntl.ioctl(master, termios.TIOCSWINSZ, struct.pack('HHHH', 12, 90, 0, 0))
                    os.write(master, steps[step][1])
                    step += 1
                if process.poll() is not None and not ready:
                    break
            # PTY EOF can precede waitpid readiness by a scheduler tick.
            try:
                process.wait(timeout=1)
            except subprocess.TimeoutExpired:
                pass
            if process.poll() is None:
                process.send_signal(signal.SIGQUIT)
                try: process.wait(timeout=5)
                except subprocess.TimeoutExpired:
                    os.killpg(process.pid, signal.SIGKILL); process.wait()
                record(name, False, 'Timed out')
            else:
                record(name, process.returncode == 0 and step == len(steps) and expected in output,
                       f'exit={process.returncode}, input_steps={step}/{len(steps)}')
        finally:
            if process.poll() is None:
                process.send_signal(signal.SIGQUIT)
                try: process.wait(timeout=5)
                except subprocess.TimeoutExpired:
                    os.killpg(process.pid, signal.SIGKILL); process.wait()
            os.close(master)
            (evidence / (name + '.terminal')).write_bytes(output)

    interactive('bash-read-unicode', 'printf READY; read -r value; printf "REPLY:%s" "$value"',
                [(b'READY', 'é 😀\n'.encode())], 'REPLY:é 😀'.encode())
    interactive('cat-eof', 'printf READY; cat; printf EOF-OK',
                [(b'READY', b'hello\n'), (b'hello', b'\x04')], b'EOF-OK')
    interactive('control-c', "trap 'printf INT-OK; exit 0' INT; printf READY; sleep 30",
                [(b'READY', b'\x03')], b'INT-OK')
    interactive('resize', 'stty size; read -r value; stty size',
                [(b'24 80', b'continue\n')], b'12 90', resize=True)
    interactive('job-control-z', 'bash --noprofile --norc -i',
                [(b'$ ', b'sleep 30\n'), (b'sleep 30', b'\x1a'),
                 (b'Stopped', b'fg\n'), (b'fg\r\n', b'\x03'),
                 (b'^C', b'exit 0\n')], b'Stopped')
    interactive('nano', 'nano --ignorercfiles "$TMPDIR/klyro-pty-nano-$$"',
                [(b'[ New File ]', b'\x18')], b'\x1b')
    interactive('vim', 'vim -u NONE -n "$TMPDIR/klyro-pty-vim-$$"',
                [(b'\x1b', b'\x1b:q!\r')], b'\x1b')
    interactive('less', 'seq 1 100 | less', [(b'\x1b', b'q')], b'\x1b')
    interactive('top', 'top -d 1', [(b'\x1b', b'q')], b'\x1b')
    interactive('ansi-truecolor-utf8', "printf '\033[38;2;12;34;56mCOLOUR-é😀\033[0m'", [],
                '\x1b[38;2;12;34;56mCOLOUR-é😀\x1b[0m'.encode())

    # A separate process group would survive kill(-proot_pid, SIGKILL) alone.
    pidfile = fixture / 'termux/home/.klyro-test-detached-pid'
    if pidfile.exists():
        raise ValueError('Stale detached-process test marker; review it first')
    command = 'setsid sh -c \'echo $$ > "$HOME/.klyro-test-detached-pid"; exec sleep 60\' & wait'
    process = subprocess.Popen(argv + ['-c', command], env=env, stdout=subprocess.DEVNULL,
                               stderr=subprocess.DEVNULL, start_new_session=True)
    detached = None
    identity = None
    def start_token(pid):
        try: return Path(f'/proc/{pid}/stat').read_text().rsplit(')', 1)[1].split()[19]
        except FileNotFoundError: return None
    try:
        deadline = time.monotonic() + 10
        while time.monotonic() < deadline and not pidfile.exists(): time.sleep(.05)
        detached = int(pidfile.read_text().strip())
        identity = start_token(detached)
        process.send_signal(signal.SIGQUIT)
        process.wait(timeout=5)
        deadline = time.monotonic() + 3
        while time.monotonic() < deadline and Path(f'/proc/{detached}').exists(): time.sleep(.05)
        record('close-detached-job', not Path(f'/proc/{detached}').exists())
    except Exception as error:
        record('close-detached-job', False, str(error))
    finally:
        if process.poll() is None:
            process.send_signal(signal.SIGQUIT)
            try: process.wait(timeout=5)
            except subprocess.TimeoutExpired: os.killpg(process.pid, signal.SIGKILL); process.wait()
        # This PID was created exclusively by this test, not by a user's session.
        if detached and identity is not None and start_token(detached) == identity:
            try: os.kill(detached, signal.SIGKILL)
            except ProcessLookupError: pass
        pidfile.unlink(missing_ok=True)
    parallel = []
    try:
        for index in range(8):
            master, slave = pty.openpty()
            command = f'read -r value; test "$value" = session-{index}; result=$?; printf "SESSION-{index}:%s" "$value"; exit "$result"'
            process = subprocess.Popen(argv + ['-c', command], env=env,
                                       stdin=slave, stdout=slave, stderr=slave, start_new_session=True)
            os.close(slave)
            parallel.append((master, process))
        for index, (master, process) in enumerate(parallel):
            os.write(master, f'session-{index}\n'.encode())
        codes = [process.wait(timeout=15) for _, process in parallel]
        record('eight-independent-input-streams', all(code == 0 for code in codes), str(codes))
    except Exception as error:
        record('eight-independent-input-streams', False, str(error))
    finally:
        for master, process in parallel:
            if process.poll() is None:
                process.send_signal(signal.SIGQUIT)
                try: process.wait(timeout=5)
                except subprocess.TimeoutExpired: os.killpg(process.pid, signal.SIGKILL); process.wait()
            os.close(master)
    return 1 if any(row['status'] == 'FAIL' for row in results) else 0


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('fixture', type=Path)
    raise SystemExit(main(parser.parse_args().fixture))
