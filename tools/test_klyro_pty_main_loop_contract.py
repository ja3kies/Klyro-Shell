"""Protect main-loop fairness and lossless PTY output under bursty subprocess traffic.

These source gates supplement real queue/PTY tests; Android on-device latency
still needs measuring on a physical Pixel.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
SESSION = (ROOT / "terminal-emulator/src/main/java/com/termux/terminal/TerminalSession.java").read_text()

class PtyMainLoopContract(unittest.TestCase):
    def test_reader_wakeup_is_deduplicated(self):
        reader = SESSION.split('final Thread reader = new Thread', 1)[1].split('reader.start();', 1)[0]
        self.assertIn('scheduleNewInput()', reader)
        self.assertNotIn('sendEmptyMessage(MSG_NEW_INPUT)', reader)
        self.assertIn('mInputMessagePending.compareAndSet(false, true)', SESSION)

    def test_output_is_drained_with_a_bounded_main_thread_budget(self):
        handler = SESSION.split('class MainThreadHandler extends Handler', 1)[1]
        self.assertIn('MAX_BYTES_PER_DISPATCH', handler)
        self.assertIn('bytesProcessed < MAX_BYTES_PER_DISPATCH', handler)
        self.assertIn('mProcessToTerminalIOQueue.available()', handler)
        self.assertIn('mInputMessagePending.set(false)', handler)
        self.assertIn('scheduleNewInput()', handler)

    def test_empty_stale_messages_do_not_redraw_and_exit_clears_pending(self):
        handler = SESSION.split('class MainThreadHandler extends Handler', 1)[1]
        self.assertIn('if (bytesProcessed > 0)', handler)
        self.assertIn('removeMessages(MSG_NEW_INPUT)', handler)
        self.assertIn('MSG_PROCESS_EXITED', handler)
        self.assertIn('mProcessToTerminalIOQueue.read(', handler)

    def test_backpressure_and_atomic_input_are_preserved(self):
        self.assertIn('if (!mProcessToTerminalIOQueue.write(buffer, 0, read)) return;', SESSION)
        self.assertIn('mTerminalToProcessIOQueue.offer(data, offset, count)', SESSION)
        self.assertIn('mClient.onInputRejected(this)', SESSION)

if __name__ == "__main__":
    unittest.main()
