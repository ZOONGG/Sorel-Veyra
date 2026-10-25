package dev.veyra.client.utils.render;

import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public class RenderUtils {
    private static final int CORNER_STEP_DEGREES = 12;
    private static final boolean[] ALL_CORNERS = {true, true, true, true};
    private static final int[] CORNER_ORDER = {0, 3, 2, 1};
    private static final float[] ARC_X = new float[36];
    private static final float[] ARC_Y = new float[36];
    static {
        for (int corner = 0; corner < 4; corner++) {
            for (int step = 0; step <= 8; step++) {
                double angle = Math.toRadians(180 + corner * 90 + step * 90.0 / 8);
                ARC_X[corner * 9 + step] = (float) Math.cos(angle);
                ARC_Y[corner * 9 + step] = (float) Math.sin(angle);
            }
        }
    }
    private static final int SAVED_GL_STATE = GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT
            | GL11.GL_CURRENT_BIT | GL11.GL_LINE_BIT;
    public static void setColor(final int color) {
        final float a = ((color >> 24) & 0xFF) / 255.0f;
        final float r = ((color >> 16) & 0xFF) / 255.0f;
        final float g = ((color >> 8) & 0xFF) / 255.0f;
        final float b = (color & 0xFF) / 255.0f;
        GL11.glColor4f(r, g, b, a);
    }

    public static void drawRoundedRect(float x, float y, float x1, float y1, final float radius, final int color) {
        drawRoundedRect(x, y, x1, y1, radius, color,  ALL_CORNERS );
    }

    public static void drawRoundedRect(float x, float y, float x1, float y1, final float radius, final int color, boolean[] round) {
        drawRoundedGradient(x, y, x1, y1, radius, color, color, round);
    }

    public static void drawRoundedGradient(float x, float y, float x1, float y1, float radius,
                                           int topColor, int bottomColor) {
        drawRoundedGradient(x, y, x1, y1, radius, topColor, bottomColor, ALL_CORNERS);
    }

    private static void drawRoundedGradient(float x, float y, float x1, float y1, float radius,
                                            int topColor, int bottomColor, boolean[] round) {
        if (x1 <= x || y1 <= y || ((topColor | bottomColor) >>> 24) == 0) return;
        float r = Math.max(0, Math.min(radius, Math.min(x1 - x, y1 - y) / 2));
        float[] edge = contour(x, y, x1, y1, r, round);
        GL11.glPushAttrib(SAVED_GL_STATE | GL11.GL_LIGHTING_BIT | GL11.GL_POLYGON_BIT);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_FOG);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GL11.glBegin(GL11.GL_POLYGON);
        for (int i = 0; i < edge.length; i += 2) {
            setColor(verticalColor(topColor, bottomColor, (edge[i + 1] - y) / (y1 - y)));
            GL11.glVertex2f(edge[i], edge[i + 1]);
        }
        GL11.glEnd();
        // Coverage fringe works without driver-dependent polygon smoothing/MSAA.
        float[] fringe = contour(x - 0.5F, y - 0.5F, x1 + 0.5F, y1 + 0.5F, r + 0.5F, round);
        GL11.glBegin(GL11.GL_QUAD_STRIP);
        for (int point = 0; point <= edge.length; point += 2) {
            int i = point % edge.length;
            int color = verticalColor(topColor, bottomColor, (edge[i + 1] - y) / (y1 - y));
            setColor(color);
            GL11.glVertex2f(edge[i], edge[i + 1]);
            setColor(color & 0xFFFFFF);
            GL11.glVertex2f(fringe[i], fringe[i + 1]);
        }
        GL11.glEnd();
        GL11.glPopAttrib();
        GlStateManager.color(1, 1, 1, 1);
    }

    private static int verticalColor(int first, int second, float progress) {
        if (first == second) return first;
        progress = Math.max(0, Math.min(1, progress));
        int color = 0;
        for (int shift = 0; shift <= 24; shift += 8) {
            int a = first >>> shift & 255;
            int b = second >>> shift & 255;
            color |= Math.round(a + (b - a) * progress) << shift;
        }
        return color;
    }

    private static float[] contour(float x, float y, float right, float bottom,
                                   float radius, boolean[] corners) {
        float[] points = new float[72];
        int cursor = 0;
        for (int corner = 0; corner < 4; corner++) {
            float r = corners[CORNER_ORDER[corner]] ? radius : 0;
            float cx = corner == 0 || corner == 3 ? x + r : right - r;
            float cy = corner < 2 ? y + r : bottom - r;
            for (int step = 0; step <= 8; step++) {
                int arc = corner * 9 + step;
                points[cursor++] = cx + ARC_X[arc] * r;
                points[cursor++] = cy + ARC_Y[arc] * r;
            }
        }
        return points;
    }

    private static void drawRing(float[] inner, float[] outer, int insideColor, int outsideColor) {
        GL11.glBegin(GL11.GL_QUAD_STRIP);
        for (int point = 0; point <= inner.length; point += 2) {
            int i = point % inner.length;
            setColor(insideColor);
            GL11.glVertex2f(inner[i], inner[i + 1]);
            setColor(outsideColor);
            GL11.glVertex2f(outer[i], outer[i + 1]);
        }
        GL11.glEnd();
    }

    public static void roundHelper(float x, float y, float radius, int pn, int pn2, int originalRotation, int finalRotation) {
        for (int i = originalRotation; i < finalRotation; i += CORNER_STEP_DEGREES) {
            GL11.glVertex2d(x + (radius * -pn) + (Math.sin((i * 3.141592653589793) / 180.0) * radius * pn), y + (radius * pn2) + (Math.cos((i * 3.141592653589793) / 180.0) * radius * pn));
        }
        int i = finalRotation;
        GL11.glVertex2d(x + (radius * -pn) + (Math.sin((i * Math.PI) / 180.0) * radius * pn),
                y + (radius * pn2) + (Math.cos((i * Math.PI) / 180.0) * radius * pn));
    }

    public static void drawRoundedOutline(float x, float y, float x1, float y1, final float radius, final float borderSize, final int color) {
        drawRoundedOutline(x, y, x1, y1, radius, borderSize , color, ALL_CORNERS);
    }

    public static void round(float x, float y, float x1, float y1, float radius, final boolean[] round) {
        if(round[0])
            roundHelper(x, y, radius, -1, 1,0, 90);
        else
            GL11.glVertex2d(x, y);

        if(round[1])
            roundHelper(x, y1, radius, -1, -1, 90, 180);
        else
            GL11.glVertex2d(x, y1);

        if(round[2])
            roundHelper(x1, y1, radius, 1, -1, 0, 90);
        else
            GL11.glVertex2d(x1, y1);

        if(round[3])
            roundHelper(x1, y, radius, 1, 1, 90, 180);
        else
            GL11.glVertex2d(x1, y);
    }


    public static void drawRoundedOutline(float x, float y, float x1, float y1, final float radius, final float borderSize, final int color, boolean[] drawCorner) {
        if (x1 <= x || y1 <= y || borderSize <= 0 || (color >>> 24) == 0) return;
        float r = Math.max(0, Math.min(radius, Math.min(x1 - x, y1 - y) / 2));
        float thickness = Math.min(borderSize, Math.min(x1 - x, y1 - y) / 2);
        float[] outer = contour(x, y, x1, y1, r, drawCorner);
        float[] inner = contour(x + thickness, y + thickness, x1 - thickness,
                y1 - thickness, Math.max(0, r - thickness), drawCorner);
        GL11.glPushAttrib(SAVED_GL_STATE | GL11.GL_LIGHTING_BIT | GL11.GL_POLYGON_BIT);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_FOG);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        drawRing(inner, outer, color, color);
        drawRing(outer, contour(x - 0.5F, y - 0.5F, x1 + 0.5F, y1 + 0.5F,
                r + 0.5F, drawCorner), color, color & 0xFFFFFF);
        if (x1 - x > thickness * 2 + 1 && y1 - y > thickness * 2 + 1) {
            drawRing(contour(x + thickness + 0.5F, y + thickness + 0.5F,
                    x1 - thickness - 0.5F, y1 - thickness - 0.5F,
                    Math.max(0, r - thickness - 0.5F), drawCorner), inner,
                    color & 0xFFFFFF, color);
        }
        GL11.glPopAttrib();
        GlStateManager.color(1, 1, 1, 1);
    }

    /** Four-corner color interpolation, preserving surrounding legacy GL state. */
    public static void gradient(float x, float y, float right, float bottom,
                                int topLeft, int topRight, int bottomRight, int bottomLeft) {
        GL11.glPushAttrib(SAVED_GL_STATE | GL11.GL_LIGHTING_BIT | GL11.GL_POLYGON_BIT);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_FOG);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GL11.glBegin(GL11.GL_QUADS);
        setColor(topLeft); GL11.glVertex2f(x, y);
        setColor(topRight); GL11.glVertex2f(right, y);
        setColor(bottomRight); GL11.glVertex2f(right, bottom);
        setColor(bottomLeft); GL11.glVertex2f(x, bottom);
        GL11.glEnd();
        GL11.glPopAttrib();
        GlStateManager.color(1, 1, 1, 1);
    }

    public static void drawBorderedRoundedRect1(float x, float y, float x1, float y1, float radius, float borderSize, int borderC, int insideC) {
        drawRoundedRect(x, y, x1, y1, radius, insideC);
        drawRoundedOutline(x, y, x1, y1, radius, borderSize, borderC);
    }

    public static int rainbowDraw(long speed, long... delay) {
        long time = System.currentTimeMillis() + (delay.length > 0 ? delay[0] : 0L);
        return Color.getHSBColor((float) (time % (15000L / speed)) / (15000.0F / (float) speed), 1.0F, 1.0F).getRGB();
    }
}
