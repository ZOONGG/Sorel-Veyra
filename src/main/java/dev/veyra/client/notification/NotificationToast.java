package dev.veyra.client.notification;

import dev.veyra.client.clickgui.UiTokens;
import dev.veyra.client.clickgui.UiIcons;
import dev.veyra.client.clickgui.font.VeyraFont;
import dev.veyra.client.clickgui.font.VeyraFonts;
import dev.veyra.client.utils.render.RenderUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

/** One production toast renderer shared with the offscreen visual probe. */
public final class NotificationToast {
    public static final long ENTER_MS = 650L, HOLD_MS = 1400L, EXIT_MS = 550L;
    public static final long LIFETIME_MS = ENTER_MS + HOLD_MS + EXIT_MS;
    public static final int CARD_HEIGHT = 24;
    private NotificationToast() {}
    public static void draw(NotificationQueue.Entry entry, float y, long nowMs) {
        int previousProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        GL20.glUseProgram(0);
        try {
            long ageMs = Math.max(0L, nowMs - entry.startedAtMs());
            float visibility = NotificationMotion.visibility(
                    ageMs, ENTER_MS, HOLD_MS, EXIT_MS);
            int alpha = Math.round(255.0F * visibility);
    
            VeyraFont semibold = VeyraFonts.semibold();
            float nameWidth = semibold.width(entry.moduleName(), 11.5F);
            int width = Math.max(90, Math.round(nameWidth + 42));
            float x = 10.0F - 10 * (1 - visibility);
            y -= 3 * (1 - visibility);
            int accent = entry.enabled() ? 0xFF9C7BFF : UiTokens.Color.TEXT_SECONDARY;
            int text = withAlpha(UiTokens.Color.TEXT_PRIMARY, alpha);
            int mark = withAlpha(accent, alpha);
            RenderUtils.drawRoundedRect(x, y, x + width, y + CARD_HEIGHT, 5,
                    withAlpha(0xFF111620, Math.round(250 * visibility)));
            RenderUtils.drawRoundedOutline(x, y, x + width, y + CARD_HEIGHT, 5, .7F,
                    withAlpha(UiTokens.Color.BORDER, alpha));
            RenderUtils.drawRoundedRect(x + 6, y + 5, x + 20, y + 19, 5,
                    withAlpha(accent, Math.round(25 * visibility)));
            if (entry.enabled()) {
                UiIcons.line(x + 9.5F, y + 12, x + 12, y + 14.5F, 1.1F, mark);
                UiIcons.line(x + 12, y + 14.5F, x + 17, y + 9.5F, 1.1F, mark);
            } else {
                UiIcons.line(x + 9.5F, y + 12, x + 16.5F, y + 12, 1.1F, mark);
            }
            semibold.drawPlain(entry.moduleName(), x + 27, y + 5.5F, text, 11.5F);
            float remaining = Math.max(0, 1 - Math.max(0, ageMs - ENTER_MS) / (float) (HOLD_MS + EXIT_MS));
            RenderUtils.drawRoundedRect(x + 6, y + CARD_HEIGHT - 2,
                    x + 6 + (width - 12) * remaining, y + CARD_HEIGHT - 1, .5F,
                    withAlpha(accent, Math.round(130 * visibility)));
        } finally { GL20.glUseProgram(previousProgram); }
    }

    private static int withAlpha(int color, int alpha) {
        return Math.max(0, Math.min(255, alpha)) << 24 | color & 0x00FFFFFF;
    }
}
