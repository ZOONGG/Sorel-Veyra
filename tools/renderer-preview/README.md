# OpenGL renderer probe

Run `./tools/renderer-preview/Render.ps1` from PowerShell after building Veyra.
Supply `-JavaHome`, `-GradleHome`, and `-Natives` when local runtime paths differ.

This compiles the production font, token, drawing and rounded geometry sources
against small Minecraft texture/state adapters and renders through LWJGL 2.
The adapters live exclusively in this tool, outside the mod's source sets.
The PNG is written to `.artifacts/redesign/primitives-scaleN.png`.

The probe checks real OpenGL primitive rendering at GUI scales 1–4 (`-Scale 1`, etc.), font coverage,
Cyrillic, toggle positions, slider endpoints, and color gradients. It checks
`glGetError` and exits. It does not launch Minecraft, execute ClickGui, exercise
mouse input, or prove compatibility with Lunar's render state. Those checks
still require the full in-game interface.

Font faces are generated from the existing bundled variable Inter with
`FreezeFontFaces.py` (fonttools 4.67.0, opsz=14, wght=400/600). The original font
and its OFL license remain included. The JUnit font regression test verifies
that Java renders distinct weights and retains Russian glyph coverage.

The probe uses an offscreen framebuffer, so large scales are not limited by the
monitor's window size. It verifies inherited GL state restoration and a pixel
color under lighting/fog/alpha-test state before rendering the visual samples.
