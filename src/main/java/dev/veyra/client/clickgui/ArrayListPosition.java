package dev.veyra.client.clickgui;

import dev.veyra.client.clickgui.font.VeyraFonts;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import dev.veyra.client.module.modules.client.ArrayListModule;
import dev.veyra.client.utils.Utils;


public class ArrayListPosition extends GuiScreen {
    GuiButton resetPosButton;
    boolean mouseDown = false;
    int textBoxStartX = 0;
    int textBoxStartY = 0;
    ScaledResolution sr;
    int textBoxEndX = 0;
    int textBoxEndY = 0;
    int marginX = 5;
    int marginY = 70;
    int lastMousePosX = 0;
    int lastMousePosY = 0;
    int sessionMousePosX = 0;
    int sessionMousePosY = 0;

    public void initGui() {
        super.initGui();
        this.buttonList.clear();
        this.buttonList.add(this.resetPosButton = new UiButton(1, this.width - 106, 8, 98, 24, "Reset position"));
        this.marginX = ArrayListModule.hudX;
        this.marginY = ArrayListModule.hudY;
        this.mouseDown = false;
        sr = new ScaledResolution(mc);
        ArrayListModule.positionMode = Utils.HUD.getPostitionMode(marginX, marginY, sr.getScaledWidth(), sr.getScaledHeight());
    }

    public void drawScreen(int mX, int mY, float pt) {
        drawRect(0, 0, this.width, this.height, Theme.getScrimColor().getRGB());
        drawRect(0, this.height /2, this.width, this.height /2 + 1, Theme.getTrackColor().getRGB());
        drawRect(this.width /2, 0, this.width /2 + 1, this.height, Theme.getTrackColor().getRGB());
        int textBoxStartX = this.marginX;
        int textBoxStartY = this.marginY;
        int[] previewSize = this.drawArrayList();
        int textBoxEndX = textBoxStartX + previewSize[0];
        int textBoxEndY = textBoxStartY + previewSize[1];
        this.textBoxStartX = textBoxStartX;
        this.textBoxStartY = textBoxStartY;
        this.textBoxEndX = textBoxEndX;
        this.textBoxEndY = textBoxEndY;
        ArrayListModule.hudX = textBoxStartX;
        ArrayListModule.hudY = textBoxStartY;
        String title = "Drag the preview to position it";
        String hint = "Preview uses your live ArrayList style";
        int centerY = this.height / 2 - 20;
        VeyraFonts.semibold().drawPlain(title,
                (this.width - VeyraFonts.semibold().width(title, UiTokens.Type.TITLE)) / 2,
                centerY, Theme.TEXT_PRIMARY_INT, UiTokens.Type.TITLE);
        VeyraFonts.regular().drawPlain(hint,
                (this.width - VeyraFonts.regular().width(hint, UiTokens.Type.CAPTION)) / 2,
                centerY + 16, Theme.TEXT_MUTED_INT, UiTokens.Type.CAPTION);

        super.drawScreen(mX, mY, pt);
    }

    private int[] drawArrayList() {
        dev.veyra.client.module.Module module = dev.veyra.client.main.Veyra.moduleManager == null
                ? null : dev.veyra.client.main.Veyra.moduleManager.getModuleByClazz(ArrayListModule.class);
        if (module instanceof ArrayListModule arrayList) {
            return arrayList.renderPositionPreview(this.marginX, this.marginY);
        }
        return new int[]{92, 39};
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);
        if (button != 0 || resetPosButton.mousePressed(mc, mouseX, mouseY)) return;
        if (mouseX >= textBoxStartX && mouseX <= textBoxEndX
                && mouseY >= textBoxStartY && mouseY <= textBoxEndY) {
            mouseDown = true;
            sessionMousePosX = mouseX;
            sessionMousePosY = mouseY;
            lastMousePosX = marginX;
            lastMousePosY = marginY;
        }
    }

    @Override
    public void mouseClickMove(int mouseX, int mouseY, int button, long elapsed) {
        if (button != 0 || !mouseDown) return;
        int previewWidth = Math.max(1, textBoxEndX - textBoxStartX);
        int previewHeight = Math.max(1, textBoxEndY - textBoxStartY);
        marginX = Math.max(2, Math.min(Math.max(2, width - previewWidth - 2),
                lastMousePosX + mouseX - sessionMousePosX));
        marginY = Math.max(2, Math.min(Math.max(2, height - previewHeight - 2),
                lastMousePosY + mouseY - sessionMousePosY));
        ArrayListModule.positionMode = Utils.HUD.getPostitionMode(marginX, marginY, width, height);
    }

    public void mouseReleased(int mX, int mY, int state) {
        super.mouseReleased(mX, mY, state);
        if (state == 0) {
            if (this.mouseDown) persistPosition();
            this.mouseDown = false;
        }

    }

    public void actionPerformed(GuiButton b) {
        if (b == this.resetPosButton) {
            this.marginX = ArrayListModule.hudX = 5;
            this.marginY = ArrayListModule.hudY = 70;
            ArrayListModule.positionMode = Utils.HUD.getPostitionMode(marginX, marginY, width, height);
        }

    }

    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void onGuiClosed() { persistPosition(); }

    private void persistPosition() {
        ArrayListModule.hudX = marginX;
        ArrayListModule.hudY = marginY;
        if (dev.veyra.client.main.Veyra.clientConfig != null) {
            dev.veyra.client.main.Veyra.clientConfig.saveConfig();
        }
    }
}
