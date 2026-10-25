package dev.veyra.client.clickgui;

import dev.veyra.client.clickgui.font.VeyraFonts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

/** Shared menu button for screens that use Minecraft's button dispatch. */
public final class UiButton extends GuiButton {
    private float hoverProgress;
    private long lastFrame;

    public UiButton(int id, int x, int y, int width, int height, String label) {
        super(id, x, y, width, height, label);
    }

    @Override
    public void drawButton(Minecraft minecraft, int mouseX, int mouseY) {
        if (!visible) return;
        hovered = enabled && mouseX >= xPosition && mouseY >= yPosition
                && mouseX < xPosition + width && mouseY < yPosition + height;
        long now = System.nanoTime();
        float delta = lastFrame == 0 ? 0 : (now - lastFrame) / 1_000_000_000.0F;
        lastFrame = now;
        hoverProgress = FrameMotion.expApproach(hoverProgress, hovered ? 1 : 0,
                UiTokens.Motion.HOVER_SPEED, delta);
        UiDraw.button(xPosition, yPosition, xPosition + width, yPosition + height,
                hoverProgress, 0, Theme.getMainColor().getRGB());
        float textWidth = VeyraFonts.semibold().width(displayString, UiTokens.Type.BODY);
        VeyraFonts.semibold().drawPlain(displayString, xPosition + (width - textWidth) / 2,
                yPosition + (height - UiTokens.Type.BODY) / 2,
                enabled ? UiTokens.Color.TEXT_SECONDARY : UiTokens.Color.TEXT_DISABLED,
                UiTokens.Type.BODY);
    }
}
