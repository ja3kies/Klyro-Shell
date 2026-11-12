#!/usr/bin/env python3
"""Execute real package/development tests in a NEW BootstrapFixture, never the host prefix.

Usage: python3 tools/verify_runtime_fixture.py .local/production/guest
This mutates only that disposable fixture (installs/upgrades/removes real packages).
It does not validate APK SELinux policy, UI, background persistence or SAF.
"""
import argparse
import json
import os
from pathlib import Path
import signal
import subprocess
import time


def main(fixture):
    fixture = fixture.resolve()
    if not fixture.is_relative_to(Path(".local").resolve()):
        raise ValueError("Use a disposable fixture under the checkout's .local directory")
    argv = (fixture / "login.argv").read_text().split("\0")
    env = dict(entry.split("=", 1) for entry in (fixture / "host.env").read_text().split("\0") if entry)
    if argv[argv.index("-r") + 1] != str(fixture / "termux"):
        raise ValueError("Fixture root does not match launch argv")
    if "--kill-on-exit" not in argv:
        argv.insert(1, "--kill-on-exit")
    evidence = fixture / "acceptance"
    evidence.mkdir(exist_ok=False)
    results = []

    def record(name, code, elapsed, note=""):
        results.append(dict(name=name, status="PASS" if code == 0 else "FAIL", exit=code,
                            seconds=round(elapsed, 2), note=note))
        (evidence / "results.json").write_text(json.dumps(results, indent=2) + "\n")
        print(f"{results[-1]['status']} {name} ({elapsed:.1f}s)", flush=True)

    def run(name, script, timeout=600):
        started = time.monotonic()
        script = 'set -eo pipefail; export TERMUX_PKG_NO_MIRROR_SELECT=1; ' + script
        with (evidence / (name + ".log")).open("wb") as log:
            log.write(("COMMAND: " + script + "\n").encode()); log.flush()
            process = subprocess.Popen(argv + ["-c", script], env=env, stdout=log,
                                       stderr=subprocess.STDOUT, start_new_session=True)
            try:
                code = process.wait(timeout=timeout)
            except subprocess.TimeoutExpired:
                # SIGQUIT lets the PRoot tracer kill/reap jobs in separate process groups.
                os.kill(process.pid, signal.SIGQUIT)
                try:
                    process.wait(timeout=5)
                except subprocess.TimeoutExpired:
                    os.killpg(process.pid, signal.SIGKILL)
                    process.wait()
                code = 124
        record(name, code, time.monotonic() - started)
        return code == 0

    run("bootstrap-shell", '''test "$PREFIX" = /data/data/com.termux/files/usr
        test "$HOME" = /data/data/com.termux/files/home
        test "$TMPDIR" = "$PREFIX/tmp"
        test "$(id -u)" != 0
        work=$(mktemp -d "$TMPDIR/klyro-é-😀-XXXXXXXX")
        trap 'rm -rf -- "$work"' EXIT
        cd "$work"; mkdir 'space é'; touch 'space é/file 😀'
        echo works > sample; test "$(cat sample)" = works; ls; pwd
        printf 'pipeline\\n' | grep -qx pipeline
        read -r input < sample; test "$input" = works
        (exit 23) && exit 1 || test "$?" = 23''')
    # Configure only this disposable fixture, not any user's selected mirror.
    run("fixture-https-source", '''printf '%s\\n' 'deb https://packages.termux.dev/apt/termux-main stable main' > "$PREFIX/etc/apt/sources.list"''')
    run("pkg-update", "pkg update -y")
    run("pkg-upgrade", "pkg upgrade -y")
    run("pkg-search", "pkg search git | grep -i git")
    tools = ["bash", "coreutils", "findutils", "grep", "sed", "gawk", "tar", "gzip", "unzip",
             "zip", "rsync", "curl", "wget", "iputils", "openssh", "git", "python", "python-pip",
             "nodejs", "npm", "clang", "make", "pkg-config", "nano", "vim", "less", "procps"]
    for tool in tools:
        run("install-" + tool, "pkg install -y " + tool, timeout=900)
    run("pkg-uninstall-git", "pkg uninstall -y git; ! command -v git")
    run("pkg-reinstall-git", "pkg install -y git")
    run("apt-update", "apt update")
    run("apt-install-nano", "apt install -y nano")
    versions = {"bash":"--version", "ls":"--version", "find":"--version", "grep":"--version",
                "sed":"--version", "awk":"--version", "tar":"--version", "gzip":"--version",
                "unzip":"-v", "rsync":"--version", "curl":"--version", "wget":"--version",
                "ping":"-V", "ssh":"-V", "git":"--version", "python":"--version",
                "node":"--version", "npm":"--version", "clang":"--version", "make":"--version",
                "pkg-config":"--version", "nano":"--version", "vim":"--version", "less":"--version",
                "top":"-b -n 1"}
    for command, flags in versions.items():
        run("execute-" + command, command + " " + flags, timeout=60)
    work = '''work=$(mktemp -d "$TMPDIR/klyro-dev-XXXXXXXX"); trap 'rm -rf -- "$work"' EXIT; '''
    run("git-https-commit-branch", work + '''git clone --depth 1 https://github.com/octocat/Hello-World.git "$work/repo"
        cd "$work/repo"; git switch -c klyro-trial
        printf 'Klyro é 😀\\n' >> README
        git add README; git -c user.name=Klyro -c user.email=trial@example.invalid commit -m trial
        test "$(git branch --show-current)" = klyro-trial; git log -1 --oneline''')
    run("python-project-dependency", work + '''python -m pip install --disable-pip-version-check --target "$work/deps" packaging==24.2
        PYTHONPATH="$work/deps" python -c 'from packaging.version import Version; assert Version("2.0") > Version("1.0"); print("Python project OK")' ''')
    run("node-npm-project", work + '''mkdir "$work/node"
        npm --prefix "$work/node" install --ignore-scripts --no-audit --no-fund is-number@7.0.0
        node -e 'if (!require(process.argv[1])(42)) process.exit(1); console.log("Node project OK")' "$work/node/node_modules/is-number"''')
    run("clang-make-project", work + '''cd "$work"
        printf '#include <stdio.h>\\nint main(void) { puts("compiled"); return 0; }\\n' > hello.c
        printf 'all:\\n\\tclang hello.c -o hello\\n' > Makefile
        make; test "$(./hello)" = compiled; pkg-config --exists zlib''')
    run("archives-rsync-unicode", work + '''cd "$work"; mkdir 'source é'
        printf 'file 😀\\n' > 'source é/space 😀.txt'
        tar czf archive.tgz 'source é'; mkdir restored; tar xzf archive.tgz -C restored
        cmp 'source é/space 😀.txt' 'restored/source é/space 😀.txt'
        zip -r archive.zip 'source é'; unzip -q archive.zip -d unzipped
        rsync -a 'source é/' copy/; cmp 'source é/space 😀.txt' 'copy/space 😀.txt' ''')
    run("tls-curl-wget", work + '''curl --fail --location https://example.com/ -o "$work/curl.html"
        wget https://example.com/ -O "$work/wget.html"; test -s "$work/curl.html"; test -s "$work/wget.html"''')
    run("network-errors", '''if curl --fail --connect-timeout 2 --max-time 3 http://127.0.0.1:1/; then exit 1; fi
        if apt-get -o Acquire::Retries=0 -o Acquire::http::Timeout=2 -o APT::Update::Error-Mode=any \
            -o Dir::Etc::sourcelist=/dev/null -o Dir::Etc::sourceparts=- update; then
            # Empty sources aren't a network probe; use curl failure above as the checked error.
            printf 'APT source-free control completed; no repository error claim\\n'
        fi''')
    run("package-consistency", "dpkg --audit; test -z \"$(dpkg --audit)\"; apt-get check")
    print("NOT TESTED: APK isolation without Termux, Android lifecycle/IME/SAF, physical stability")
    return 1 if any(row["status"] == "FAIL" for row in results) else 0


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("fixture", type=Path)
    raise SystemExit(main(parser.parse_args().fixture))
