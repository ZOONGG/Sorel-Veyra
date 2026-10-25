# Bind UX fixture

Run with Java 17 after a normal Gradle build:

```powershell
./gradlew.bat -I tools/bind-ux-probe/classpath.init.gradle bindUxClasspath --offline
./tools/bind-ux-probe/Verify.ps1
./tools/bind-ux-probe/Audit.ps1
```

The standalone probe renders production ClickGui module cards to an offscreen OpenGL framebuffer and exercises production capture, timeout, clearing, cancellation, search, expansion, config and input-lease code. It uses the existing LWJGL native DLLs in the Lunar directory. It does not construct or start Minecraft, a world, a player, or a game loop. The temporary LWJGL window is positioned offscreen.

Fixture boundaries: Module's Minecraft singleton initializer is bypassed; ClickGui's OS focus query is replaced with a controllable boolean. Movement tests allocate inert vanilla data holders without invoking constructors, and replace only the physical key-state reader with a controllable set. Rendering uses the existing renderer-preview DynamicTexture and GlStateManager substitutes. Those substitutes, the ASM loader and the probe are never packaged into the mod.

These checks validate Java input/config behavior and production card rendering, not live gameplay, Lunar mixin application, network behavior, server corrections or a real game's UI focus/input lifecycle.

Audit.ps1 compares the test JAR against the previous modern UI + old Timer candidate. Its baseline path can be overridden. Verify.ps1 accepts a custom exported classpath file.
