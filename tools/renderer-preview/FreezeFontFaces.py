"""Generate Java 17-compatible static faces from the bundled OFL variable font.

Requires fonttools (this iteration used 4.67.0). No network is used by the script.
"""
from pathlib import Path

from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont

fonts = Path(__file__).resolve().parents[2] / "src/main/resources/assets/veyra/fonts"
for weight, face in ((400, "Regular"), (600, "SemiBold")):
    font = TTFont(fonts / "Inter.ttf")
    instantiateVariableFont(font, {"opsz": 14, "wght": weight}, inplace=True)
    font.save(fonts / f"Inter-{face}.ttf")
