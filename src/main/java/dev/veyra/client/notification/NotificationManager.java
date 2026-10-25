package dev.veyra.client.notification;

import dev.veyra.client.clickgui.FrameMotion;
import dev.veyra.client.module.modules.client.ClickGuiModule;
import dev.veyra.client.utils.player.PlayerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class NotificationManager {
    private static final long ENTER_MS = NotificationToast.ENTER_MS;
    private static final long HOLD_MS = NotificationToast.HOLD_MS;
    private static final long EXIT_MS = NotificationToast.EXIT_MS;
    private static final long LIFETIME_MS = ENTER_MS + HOLD_MS + EXIT_MS;
    private static final int CARD_HEIGHT = NotificationToast.CARD_HEIGHT;
    private static final int CARD_GAP = 6;
    private static final float TOP_MARGIN = 10.0F;

    private final Minecraft mc = Minecraft.getMinecraft();
    private final NotificationQueue queue = new NotificationQueue(3, LIFETIME_MS);
    private final Map<String, Float> animatedRows = new HashMap<>();
    private long lastFrameNanos;

    public void postModuleState(String moduleName, boolean enabled) {
        if (!ClickGuiModule.notificationsEnabled()) return;
        queue.post(moduleName, enabled, System.nanoTime() / 1_000_000L);
    }

    public void renderFrame() {
        if (!ClickGuiModule.notificationsEnabled()) {
            queue.clear();
            animatedRows.clear();
            lastFrameNanos = System.nanoTime();
            return;
        }
        long nowMs = System.nanoTime() / 1_000_000L;
        List<NotificationQueue.Entry> entries = queue.visibleAt(nowMs);
        if (entries.isEmpty()
                || !PlayerUtils.isPlayerInGame()
                || mc.currentScreen != null
                || mc.gameSettings.showDebugInfo) {
            if (entries.isEmpty()) animatedRows.clear();
            lastFrameNanos = System.nanoTime();
            return;
        }

        long nowNanos = System.nanoTime();
        float deltaSeconds = lastFrameNanos == 0L
                ? 1.0F / 60.0F
                : Math.min(0.05F, Math.max(0.0F,
                (nowNanos - lastFrameNanos) / 1_000_000_000.0F));
        lastFrameNanos = nowNanos;

        int matrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        int program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_VIEWPORT_BIT | GL11.GL_COLOR_BUFFER_BIT);
        GL20.glUseProgram(0);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_STENCIL_TEST);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glViewport(0, 0, mc.displayWidth, mc.displayHeight);
        ScaledResolution resolution = new ScaledResolution(mc);
        GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix(); GL11.glLoadIdentity();
        GL11.glOrtho(0, resolution.getScaledWidth(), resolution.getScaledHeight(), 0, -1, 1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix(); GL11.glLoadIdentity();
        try {
            Set<String> visibleKeys = new HashSet<>();
            for (int index = 0; index < entries.size(); index++) {
                NotificationQueue.Entry entry = entries.get(index);
                String key = entry.moduleName().toLowerCase(Locale.ROOT);
                visibleKeys.add(key);
                float targetY = TOP_MARGIN + index * (CARD_HEIGHT + CARD_GAP);
                float currentY = animatedRows.getOrDefault(key, targetY);
                currentY = FrameMotion.expApproach(currentY, targetY, 12.0F, deltaSeconds);
                animatedRows.put(key, currentY);
                NotificationToast.draw(entry, currentY, nowMs);
            }
            animatedRows.keySet().removeIf(key -> !visibleKeys.contains(key));
        } finally {
            GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix();
            GL11.glMatrixMode(matrixMode); GL11.glPopAttrib(); GL20.glUseProgram(program);
        }
    }

}
