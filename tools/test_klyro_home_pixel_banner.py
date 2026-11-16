"""Home cover contract: a real, looping local GIF with lifecycle-aware Android playback."""
from pathlib import Path
import struct
import shutil
import subprocess
import unittest

ROOT = Path(__file__).resolve().parents[1]
HOME = (ROOT / "app/src/main/java/com/oryn/terminal/MainActivity.kt").read_text()
COVER = (ROOT / "app/src/main/java/com/oryn/terminal/KlyroHomePixelCover.kt").read_text()
GIF = ROOT / "app/src/main/res/drawable-nodpi/klyro_home_landscape_loop.gif"


def gif_frames(data):
    """Parse GIF block boundaries without requiring third-party PIL in CI."""
    if data[:6] not in (b"GIF87a", b"GIF89a"):
        raise ValueError("not a GIF")
    width, height, packed = struct.unpack_from("<HHB", data, 6)
    pos = 13 + (3 * (1 << ((packed & 7) + 1)) if packed & 0x80 else 0)
    frames, delays = [], []
    while pos < len(data):
        marker = data[pos]
        pos += 1
        if marker == 0x3B:
            break
        if marker == 0x21:
            label = data[pos]
            pos += 1
            if label == 0xF9:
                if data[pos] != 4:
                    raise ValueError("invalid graphic control extension")
                delays.append(struct.unpack_from("<H", data, pos + 2)[0])
            while True:
                length = data[pos]
                pos += 1
                if not length:
                    break
                pos += length
        elif marker == 0x2C:
            start = pos - 1
            local_packed = data[pos + 8]
            pos += 9
            if local_packed & 0x80:
                pos += 3 * (1 << ((local_packed & 7) + 1))
            pos += 1  # LZW minimum code size
            while True:
                length = data[pos]
                pos += 1
                if not length:
                    break
                pos += length
            frames.append(data[start:pos])
        else:
            raise ValueError(f"unknown GIF block {marker:#x}")
    if pos != len(data):
        raise ValueError("GIF trailer or block length mismatch")
    return width, height, frames, delays


class HomeAnimatedGifContract(unittest.TestCase):
    def test_local_asset_is_a_real_looping_multiframe_gif(self):
        data = GIF.read_bytes()
        width, height, frames, delays = gif_frames(data)
        self.assertEqual((width, height), (160, 100))
        self.assertGreaterEqual(len(frames), 8)
        self.assertLessEqual(len(frames), 24)
        self.assertEqual(len(frames), len(delays))
        self.assertTrue(all(10 <= d <= 40 for d in delays))
        self.assertGreater(len(set(frames)), 4, "Frames must actually move")
        self.assertIn(b"NETSCAPE2.0", data)
        self.assertIn(b"\x03\x01\x00\x00", data)
        self.assertLess(len(data), 150_000)

    @unittest.skipUnless(shutil.which("node"), "Node is required to reproduce GIF asset")
    def test_bundled_animation_matches_source_generator(self):
        generated = subprocess.check_output(
            ["node", "-e",
             "process.stdout.write(Buffer.from(require('./tools/generate_klyro_home_gif.cjs').generateKlyroGif()))"],
            cwd=ROOT
        )
        self.assertEqual(generated, GIF.read_bytes(), "Regenerate the GIF after sprite edits")

    def test_android_decoder_plays_and_releases_gif(self):
        self.assertIn("R.drawable.klyro_home_landscape_loop", COVER)
        self.assertIn("ImageDecoder.createSource", COVER)
        self.assertIn("ImageDecoder.decodeDrawable", COVER)
        self.assertIn("AnimatedImageDrawable.REPEAT_INFINITE", COVER)
        self.assertIn("setFilterBitmap(false)", COVER)
        self.assertIn("LifecycleEventObserver", COVER)
        self.assertIn("ViewTreeLifecycleOwner.get(LocalView.current)?.lifecycle", COVER)
        self.assertNotIn("(context as? LifecycleOwner)", COVER)
        self.assertIn("onDispose", COVER)
        self.assertIn(".stop()", COVER)
        self.assertIn("Build.VERSION.SDK_INT >= Build.VERSION_CODES.P", COVER)

    def test_reduced_motion_and_pre_android_9_use_static_fallback(self):
        self.assertIn("Settings.Global.ANIMATOR_DURATION_SCALE", COVER)
        self.assertIn("if (motionEnabled", COVER)
        self.assertIn("Canvas(", COVER)
        self.assertNotIn("rememberInfiniteTransition", COVER)
        self.assertIn("clearAndSetSemantics", COVER)

    def test_cover_still_fades_into_profile_and_actions(self):
        self.assertIn("val homeVisible = !showPlugins &&", HOME)
        self.assertIn("if (homeVisible) {", HOME)
        self.assertIn("KlyroHomePixelCover(bannerHeight, screenChrome)", HOME)
        self.assertIn(".66f to Color.Transparent", COVER)
        self.assertIn("1f to screenChrome", COVER)
        home = HOME.split("// The primary action, runtime and tools", 1)[1]
        self.assertLess(home.index("Spacer(Modifier.fillMaxWidth().height(bannerSpace))"),
                        home.index('Text("KLYRO"'))
        self.assertLess(home.index('Text("KLYRO"'),
                        home.index("if (homeAction != KlyroHomeAction.NONE)"))
        self.assertLess(home.index("if (homeAction != KlyroHomeAction.NONE)"),
                        home.index("stateLabel,"))

    def test_compact_screen_does_not_hide_primary_action(self):
        self.assertIn("screenHeightDp < 660", HOME)
        self.assertIn("80.dp else 194.dp", HOME)
        self.assertIn("148.dp else 256.dp", HOME)
        self.assertIn(".verticalScroll(rememberScrollState())", HOME)


if __name__ == "__main__":
    unittest.main()
