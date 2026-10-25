package dev.veyra.client.clickgui;

import dev.veyra.client.utils.render.RenderUtils;

/** Low-cost reusable drawing primitives shared by Veyra screens. */
public final class UiDraw {
    private UiDraw() {
    }

    public static void surface(float left, float top, float right, float bottom,
                               float radius, int fill, int border, int elevation) {
        if (right <= left || bottom <= top) return;
        if (elevation > 0) {
            int layers = elevation >= 2 ? 8 : 4;
            for (int layer = layers; layer >= 1; layer--) {
                float spread = layer * 0.8F;
                RenderUtils.drawRoundedRect(left - spread, top + 2 - spread * 0.5F,
                        right + spread, bottom + 2 + spread, radius + spread,
                        withAlpha(0, 3 + (layers - layer) * 2));
            }
        }
        RenderUtils.drawRoundedRect(left, top, right, bottom, radius, fill);
        if ((border >>> 24) != 0) {
            RenderUtils.drawRoundedOutline(left, top, right, bottom, radius, 0.85F, border);
        }
    }

    public static void control(float left, float top, float right, float bottom,
                               boolean hovered, boolean focused, int accent) {
        control(left, top, right, bottom, hovered ? 1 : 0, focused ? 1 : 0, accent);
    }

    public static void control(float left, float top, float right, float bottom,
                               float hover, float focus, int accent) {
        int fill = mix(mix(UiTokens.Color.INPUT, UiTokens.Color.INPUT_HOVER, hover),
                UiTokens.Color.INPUT_FOCUS, focus);
        int border = mix(UiTokens.Color.BORDER_SOFT, withAlpha(accent, 180), focus);
        surface(left, top, right, bottom, UiTokens.Radius.SM, fill, border, 0);
    }

    public static void button(float left, float top, float right, float bottom,
                              boolean hovered, boolean active, int accent) {
        button(left, top, right, bottom, hovered ? 1 : 0, active ? 1 : 0, accent);
    }

    public static void button(float left, float top, float right, float bottom,
                              float hover, float active, int accent) {
        int fill = mix(UiTokens.Color.SURFACE, UiTokens.Color.SURFACE_HOVER, hover);
        int selectedFill = mix(UiTokens.Color.SURFACE_RAISED, accent, 0.12F);
        selectedFill = mix(selectedFill, 0xFFFFFFFF, clamp01(hover) * 0.025F);
        fill = mix(fill, selectedFill, active);
        int border = mix(UiTokens.Color.BORDER_SOFT, withAlpha(accent, 155), active);
        surface(left, top, right, bottom, UiTokens.Radius.SM, fill, border, 0);
    }

    public static void toggle(float left, float top, float right, float bottom,
                               boolean enabled, int accent) {
        toggle(left, top, right, bottom, enabled ? 1.0F : 0.0F, false, false, accent);
    }

    public static void toggle(float left, float top, float right, float bottom,
                              float progress, boolean hovered, boolean pressed, int accent) {
        progress = clamp01(progress);
        float height = bottom - top;
        float radius = height / 2.0F;
        RenderUtils.drawRoundedRect(left, top, right, bottom, radius,
                mix(UiTokens.Color.TRACK_INSET, accent, progress * 0.88F));
        int outline = mix(UiTokens.Color.BORDER,
                withAlpha(accent, hovered ? 220 : 160), progress);
        RenderUtils.drawRoundedOutline(left, top, right, bottom, radius, 0.65F,
                hovered ? mix(outline, 0xFFD6DCE7, 0.18F) : outline);
        float knobSize = height - 4;
        float knobLeft = left + 2 + (right - left - knobSize - 4) * progress;
        float inset = pressed ? 0.5F : 0.0F;
        RenderUtils.drawRoundedRect(knobLeft + inset, top + 2 + inset,
                knobLeft + knobSize - inset, top + 2 + knobSize - inset,
                knobSize / 2.0F, UiTokens.Color.KNOB);
    }

    public static void slider(float left, float top, float right, float bottom,
                              float first, float second, int accent) {
        slider(left, top, right, bottom, first, second, accent, 0);
    }

    public static void slider(float left, float top, float right, float bottom,
                              float first, float second, int accent, float emphasis) {
        emphasis = clamp01(emphasis);
        float radius = Math.max(1.0F, (bottom - top) / 2.0F);
        RenderUtils.drawRoundedRect(left, top - 1, right, bottom + 1,
                radius + 1, UiTokens.Color.TRACK_INSET);
        RenderUtils.drawRoundedRect(left, top, right, bottom, radius, UiTokens.Color.TRACK);
        float firstX = left + (right - left) * clamp01(first);
        if (second < 0.0F) {
            RenderUtils.drawRoundedRect(left, top, firstX, bottom, radius, accent);
            sliderKnob(firstX, (top + bottom) / 2.0F, accent, emphasis);
        } else {
            float secondX = left + (right - left) * clamp01(second);
            RenderUtils.drawRoundedRect(firstX, top, secondX, bottom, radius, accent);
            sliderKnob(firstX, (top + bottom) / 2.0F, accent, emphasis);
            sliderKnob(secondX, (top + bottom) / 2.0F, accent, emphasis);
        }
    }

    private static void sliderKnob(float x, float centerY, int accent, float emphasis) {
        float halo = 6 + emphasis * 2;
        RenderUtils.drawRoundedRect(x - halo, centerY - halo, x + halo, centerY + halo,
                halo, withAlpha(accent, Math.round(30 * emphasis)));
        RenderUtils.drawRoundedRect(x - 5, centerY - 4, x + 5, centerY + 6,
                5, UiTokens.Color.SHADOW_KEY);
        RenderUtils.drawRoundedRect(x - 4, centerY - 4, x + 4, centerY + 4,
                4, UiTokens.Color.KNOB);
        RenderUtils.drawRoundedOutline(x - 4, centerY - 4, x + 4, centerY + 4,
                4, 0.65F, withAlpha(accent, 190));
    }

    public static int withAlpha(int color, int alpha) {
        return Math.max(0, Math.min(255, alpha)) << 24 | color & 0x00FFFFFF;
    }

    public static int mix(int first, int second, float amount) {
        amount = clamp01(amount);
        int a = Math.round((first >>> 24 & 255) + ((second >>> 24 & 255) - (first >>> 24 & 255)) * amount);
        int r = Math.round((first >> 16 & 255) + ((second >> 16 & 255) - (first >> 16 & 255)) * amount);
        int g = Math.round((first >> 8 & 255) + ((second >> 8 & 255) - (first >> 8 & 255)) * amount);
        int b = Math.round((first & 255) + ((second & 255) - (first & 255)) * amount);
        return a << 24 | r << 16 | g << 8 | b;
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
