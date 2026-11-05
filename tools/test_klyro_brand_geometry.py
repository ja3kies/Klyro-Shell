"""Cross-format Klyro brand invariants: the SVG master owns all geometry.

This is a deterministic source comparison. Physical launcher screenshots
at 48dp/themed icon mode still require Pixel 9 acceptance.
"""
import re
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SVG = "{http://www.w3.org/2000/svg}"
ANDROID = "{http://schemas.android.com/apk/res/android}"
AAPT = "{http://schemas.android.com/aapt}"
RES = ROOT / "app/src/main/res/drawable"
DESIGN = ROOT / "design"


def paths(file, svg=False):
    tag = SVG + "path" if svg else "path"
    key = "id" if svg else ANDROID + "name"
    return {p.attrib[key]: p for p in ET.parse(file).getroot().iter(tag) if key in p.attrib}


def bounds(d):
    # Conservative control-point bounds for the fixed absolute M/L/Q/H/V/Z
    # commands in the vector masters: no external libraries or raster OCR.
    tokens = re.findall(r"[MLQHVZ]|-?\d+(?:\.\d+)?", d)
    x, y = 0.0, 0.0
    xx, yy = [], []
    i = 0
    while i < len(tokens):
        code = tokens[i]
        i += 1
        if code == "Z":
            continue
        if code in ("M", "L"):
            x, y = float(tokens[i]), float(tokens[i+1])
            i += 2
            xx.append(x)
            yy.append(y)
        elif code == "Q":
            cx, cy, x, y = map(float, tokens[i:i+4])
            i += 4
            xx.extend((cx, x))
            yy.extend((cy, y))
        elif code == "H":
            x = float(tokens[i])
            i += 1
            xx.append(x)
            yy.append(y)
        elif code == "V":
            y = float(tokens[i])
            i += 1
            xx.append(x)
            yy.append(y)
        else:
            raise AssertionError("Unsupported SVG command: " + code)
    return (min(xx), min(yy), max(xx), max(yy))


class KlyroBrandGeometryTest(unittest.TestCase):
    def setUp(self):
        self.master = DESIGN / "klyro-logo-master.svg"
        self.svg_paths = paths(self.master, svg=True)
        self.native_paths = paths(RES / "ic_klyro_launcher_foreground.xml")

    def test_master_is_editable_and_old_link_is_same_source(self):
        self.assertEqual(self.master.read_bytes(), (DESIGN / "klyro-brand-mark.svg").read_bytes())
        expected = {"k-stem", "terminal-panel", "terminal-glint", "k-ribbon",
                    "k-highlight", "prompt-chevron", "prompt-underscore"}
        self.assertEqual(set(self.svg_paths), expected)
        self.assertEqual(set(self.native_paths), expected)
        for group in ("k-stem-layer", "terminal-layer", "k-ribbon-layer",
                      "terminal-prompt-layer"):
            self.assertIsNotNone(ET.parse(self.master).getroot().find(
                ".//" + SVG + "g[@id='" + group + "']"))

    def test_android_paths_match_source_geometry_exactly(self):
        for name, source in self.svg_paths.items():
            with self.subTest(name=name):
                svg = "".join(source.attrib["d"].split())
                native = "".join(self.native_paths[name].attrib[ANDROID + "pathData"].split())
                self.assertEqual(native, svg)

    def test_gradient_geometry_and_colors_match_svg_source(self):
        root = ET.parse(self.master).getroot()
        definitions = {v.attrib["id"]: v for v in root.iter(SVG + "linearGradient")}
        for name in ("k-stem", "terminal-panel", "k-ribbon", "k-highlight"):
            svg = self.svg_paths[name]
            key = svg.attrib["fill"].removeprefix("url(#").removesuffix(")")
            src = definitions[key]
            native = self.native_paths[name].find(AAPT + "attr")
            self.assertIsNotNone(native)
            gradient = native.find("gradient")
            self.assertIsNotNone(gradient)
            for svgattr, nativeattr in (("x1", "startX"), ("y1", "startY"),
                                        ("x2", "endX"), ("y2", "endY")):
                self.assertEqual(src.attrib[svgattr], gradient.attrib[ANDROID+nativeattr])
            stops = list(src.findall(SVG+"stop"))
            self.assertEqual(stops[0].attrib["stop-color"],
                             gradient.attrib[ANDROID+"startColor"])
            self.assertEqual(stops[-1].attrib["stop-color"],
                             gradient.attrib[ANDROID+"endColor"])
        self.assertAlmostEqual(
            float(self.native_paths["k-highlight"].attrib[ANDROID+"fillAlpha"]),
            float(self.svg_paths["k-highlight"].attrib["opacity"])
        )

    def test_safe_art_and_k_panel_gap(self):
        root = ET.parse(self.master).getroot()
        group = root.find(".//" + SVG + "g[@id='mark']")
        self.assertIn("scale(.72)", group.attrib["transform"])
        box = [bounds(p.attrib["d"]) for p in self.svg_paths.values()]
        x0=min(b[0] for b in box); y0=min(b[1] for b in box)
        x1=max(b[2] for b in box); y1=max(b[3] for b in box)
        scale=.72
        # Native 48dp bounding target, inside official 66dp safe zone.
        self.assertLessEqual(max(x1-x0,y1-y0)*scale, 48.0)
        for point in (x0,x1,y0,y1):
            out=54+(point-54)*scale
            self.assertGreaterEqual(out, 21)
            self.assertLessEqual(out, 87)
        start=float(re.search(r"^M(\d+(?:\.\d+)?)", self.svg_paths["terminal-panel"].attrib["d"]).group(1))
        self.assertGreaterEqual(start-46.0, 3.5) # visible K/>_ seam, not fused
        self.assertEqual(start, 49.8)

    def test_monochrome_uses_same_stem_panel_and_ribbon(self):
        mono=paths(RES/"ic_klyro_mark_monochrome.xml")
        for name in ("k-stem","k-ribbon"):
            self.assertEqual(mono[name].attrib[ANDROID+"pathData"],
                             self.svg_paths[name].attrib["d"])
        self.assertTrue(mono["terminal-panel"].attrib[ANDROID+"pathData"].startswith(
                        self.svg_paths["terminal-panel"].attrib["d"]+" "))
        self.assertEqual(mono["terminal-panel"].attrib[ANDROID+"fillType"],"evenOdd")
        for p in mono.values():
            self.assertEqual(p.attrib[ANDROID+"fillColor"],"#FFFFFFFF")
        self.assertFalse(ET.parse(RES/"ic_klyro_mark_monochrome.xml").getroot().findall(
                         ".//"+AAPT+"attr"))

    def test_all_svg_variants_keep_identical_shapes_and_scale(self):
        main=self.master.read_text()
        for name in ("klyro-brand-mark.svg", "klyro-logo-light.svg"):
            self.assertEqual(paths(DESIGN/name,True).keys(),self.svg_paths.keys())
            variant=paths(DESIGN/name,True)
            for id,p in self.svg_paths.items():
                self.assertEqual(p.attrib["d"],variant[id].attrib["d"])
        for name in ("klyro-logo-monochrome-light.svg",
                     "klyro-logo-monochrome-dark.svg"):
            root=ET.parse(DESIGN/name).getroot()
            self.assertIn("scale(.72)",root.find(".//"+SVG+"g[@id='mark']").attrib["transform"])
            self.assertEqual(len(root.findall(".//"+SVG+"path")),3)
        self.assertTrue((DESIGN/"klyro-logo-construction.svg").exists())
        self.assertIn('x="21" y="21" width="66" height="66"',
                      (DESIGN/"klyro-logo-construction.svg").read_text())


if __name__=="__main__":
    unittest.main()
