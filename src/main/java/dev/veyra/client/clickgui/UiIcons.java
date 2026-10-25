package dev.veyra.client.clickgui;

import dev.veyra.client.utils.render.RenderUtils;
import org.lwjgl.opengl.*;
import org.lwjgl.BufferUtils;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

/** Small, consistently stroked interface icons, rasterized once with coverage mipmaps. */
public final class UiIcons {
    private static int atlas;
    private UiIcons() {}

    public static void line(float x1, float y1, float x2, float y2, float weight, int color) {
        float length = (float) Math.hypot(x2 - x1, y2 - y1);
        if (length == 0) return;
        GL11.glPushMatrix();
        GL11.glTranslatef(x1, y1, 0);
        GL11.glRotatef((float) Math.toDegrees(Math.atan2(y2 - y1, x2 - x1)), 0, 0, 1);
        RenderUtils.drawRoundedRect(-weight / 2, -weight / 2, length + weight / 2,
                weight / 2, weight / 2, color);
        GL11.glPopMatrix();
    }

    public static void settings(float x, float y, int color) { icon(2, x, y, 14, color); }
    public static void star(float x, float y, float radius, int color, boolean selected) {
        icon(selected ? 1 : 0, x, y, radius * 2.6F, color);
    }
    public static void chevron(float x, float y, float expanded, int color) {
        GL11.glPushMatrix(); GL11.glTranslatef(x, y, 0);
        GL11.glRotatef(expanded * 90, 0, 0, 1);
        line(-1.5F, -3, 1.5F, 0, .85F, color);
        line(1.5F, 0, -1.5F, 3, .85F, color);
        GL11.glPopMatrix();
    }

    private static void createAtlas() {
        BufferedImage bitmap = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bitmap.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        for (int icon = 0; icon < 3; icon++) {
            g.setTransform(AffineTransform.getTranslateInstance(icon * 64 + 8, 8));
            g.scale(2, 2);
            g.setStroke(new BasicStroke(1.35F, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            if (icon < 2) {
                Path2D star = new Path2D.Float();
                for (int i = 0; i < 10; i++) {
                    double angle = -Math.PI / 2 + i * Math.PI / 5;
                    double radius = (i & 1) == 0 ? 9 : 4.15;
                    double x = 12 + Math.cos(angle) * radius, y = 12 + Math.sin(angle) * radius;
                    if (i == 0) star.moveTo(x, y); else star.lineTo(x, y);
                }
                star.closePath();
                if (icon == 1) g.fill(star); else g.draw(star);
            } else {
                g.draw(new Line2D.Float(3, 6, 21, 6));
                g.draw(new Line2D.Float(3, 18, 21, 18));
                g.setColor(new Color(0, 0, 0, 0));
                g.setComposite(AlphaComposite.Clear);
                g.fill(new Ellipse2D.Float(6, 2, 8, 8));
                g.fill(new Ellipse2D.Float(11, 14, 8, 8));
                g.setComposite(AlphaComposite.SrcOver); g.setColor(Color.WHITE);
                g.draw(new Ellipse2D.Float(7, 3, 6, 6));
                g.draw(new Ellipse2D.Float(12, 15, 6, 6));
            }
        }
        g.dispose(); atlas = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, atlas);
        int size = 256, level = 0;
        int[] pixels = bitmap.getRGB(0, 0, size, size, null, 0, size);
        while (true) {
            ByteBuffer bytes = BufferUtils.createByteBuffer(pixels.length * 4);
            for (int pixel : pixels) bytes.put((byte)255).put((byte)255).put((byte)255).put((byte)(pixel >>> 24));
            bytes.flip();
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, level, GL11.GL_RGBA8, size, size, 0,
                    GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, bytes);
            if (size == 1) break;
            int[] next = new int[size * size / 4];
            for (int y = 0; y < size / 2; y++) for (int x = 0; x < size / 2; x++) {
                int at = y * 2 * size + x * 2;
                int a = ((pixels[at] >>> 24) + (pixels[at+1] >>> 24)
                        + (pixels[at+size] >>> 24) + (pixels[at+size+1] >>> 24) + 2) / 4;
                next[y * size / 2 + x] = a << 24;
            }
            pixels = next; size /= 2; level++;
        }
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, level);
    }

    private static void icon(int index, float x, float y, float size, int color) {
        int unit = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        int program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        GL20.glUseProgram(0);
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_TEXTURE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT);
        for (int i = GL11.glGetInteger(GL13.GL_MAX_TEXTURE_UNITS) - 1; i > 0; i--) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + i); GL11.glDisable(GL11.GL_TEXTURE_2D);
        }
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        if (atlas == 0) createAtlas();
        GL11.glEnable(GL11.GL_TEXTURE_2D); GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_ALPHA_TEST); GL11.glDisable(GL11.GL_LIGHTING); GL11.glDisable(GL11.GL_FOG);
        GL14.glBlendEquation(GL14.GL_FUNC_ADD);
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, atlas);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE);
        GL11.glColor4f((color >> 16 & 255)/255F, (color >> 8 & 255)/255F, (color & 255)/255F, (color >>> 24)/255F);
        float l = x - size/2, t = y - size/2, u = index/4F;
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(u, 0); GL11.glVertex2f(l, t);
        GL11.glTexCoord2f(u, .25F); GL11.glVertex2f(l, t+size);
        GL11.glTexCoord2f(u+.25F, .25F); GL11.glVertex2f(l+size, t+size);
        GL11.glTexCoord2f(u+.25F, 0); GL11.glVertex2f(l+size, t);
        GL11.glEnd(); GL11.glPopAttrib(); GL13.glActiveTexture(unit); GL20.glUseProgram(program);
    }
}
