package dev.veyra.client.clickgui.font;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.BufferUtils;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class VeyraFont {
    private static final int ATLAS_SIZE = 2048;
    private static final int PADDING = 4;
    private static final String GLYPHS = buildGlyphSet();

    private final Map<Character, Glyph> glyphs = new HashMap<>();
    private final int textureId;
    private final float nativeHeight;

    public VeyraFont(Font font) {
        BufferedImage metricsImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D metricsGraphics = metricsImage.createGraphics();
        configure(metricsGraphics);
        metricsGraphics.setFont(font);
        FontMetrics metrics = metricsGraphics.getFontMetrics();
        nativeHeight = metrics.getHeight() + PADDING * 2.0F;
        metricsGraphics.dispose();

        BufferedImage atlas = new BufferedImage(ATLAS_SIZE, ATLAS_SIZE, BufferedImage.TYPE_INT_ARGB);
        // Transparent texels retain white RGB so linear sampling interpolates
        // coverage, rather than darkening strokes against transparent black.
        Arrays.fill(((DataBufferInt) atlas.getRaster().getDataBuffer()).getData(), 0x00FFFFFF);
        Graphics2D graphics = atlas.createGraphics();
        configure(graphics);
        graphics.setFont(font);
        graphics.setColor(Color.WHITE);

        int x = PADDING;
        int y = PADDING;
        int rowHeight = 0;
        for (char character : GLYPHS.toCharArray()) {
            float advance = Math.max(1, (float) font.getStringBounds(
                    String.valueOf(character), graphics.getFontRenderContext()).getWidth());
            int glyphWidth = (int) Math.ceil(advance) + PADDING * 2;
            int glyphHeight = metrics.getHeight() + PADDING * 2;
            if (x + glyphWidth >= ATLAS_SIZE) {
                x = PADDING;
                y += rowHeight;
                rowHeight = 0;
            }
            if (y + glyphHeight >= ATLAS_SIZE) {
                break;
            }

            graphics.drawString(String.valueOf(character), x + PADDING, y + PADDING + metrics.getAscent());
            glyphs.put(character, new Glyph(x, y, glyphWidth, glyphHeight, advance));
            x += glyphWidth;
            rowHeight = Math.max(rowHeight, glyphHeight);
        }
        graphics.dispose();
        textureId = GL11.glGenTextures();
        uploadCoverageMipmaps(atlas);
    }

    private void uploadCoverageMipmaps(BufferedImage atlas) {
        int previousUnit = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        GL11.glPushAttrib(GL11.GL_TEXTURE_BIT);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        int size = ATLAS_SIZE;
        int[] pixels = atlas.getRGB(0, 0, size, size, null, 0, size);
        int level = 0;
        while (true) {
            ByteBuffer bytes = BufferUtils.createByteBuffer(pixels.length * 4);
            for (int pixel : pixels) {
                bytes.put((byte) 255).put((byte) 255).put((byte) 255).put((byte) (pixel >>> 24));
            }
            bytes.flip();
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, level, GL11.GL_RGBA8,
                    size, size, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, bytes);
            if (size == 1) break;
            int nextSize = size / 2;
            int[] next = new int[nextSize * nextSize];
            for (int y = 0; y < nextSize; y++) {
                for (int x = 0; x < nextSize; x++) {
                    int at = y * 2 * size + x * 2;
                    int alpha = ((pixels[at] >>> 24) + (pixels[at + 1] >>> 24)
                            + (pixels[at + size] >>> 24) + (pixels[at + size + 1] >>> 24) + 2) / 4;
                    next[y * nextSize + x] = alpha << 24 | 0x00FFFFFF;
                }
            }
            pixels = next;
            size = nextSize;
            level++;
        }
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, level);
        GL11.glPopAttrib();
        GL13.glActiveTexture(previousUnit);
    }

    public void draw(String text, float x, float y, int color, float height) {
        draw(text, x + 0.5F, y + 0.5F, withAlpha(0xFF000000, color, 0.42F), height, false);
        draw(text, x, y, color, height, false);
    }

    public void drawPlain(String text, float x, float y, int color, float height) {
        draw(text, x, y, color, height, false);
    }

    public float width(String text, float height) {
        return width(text, height, 0.0F);
    }

    public float width(String text, float height, float tracking) {
        float scale = height / nativeHeight;
        float width = 0.0F;
        char[] characters = text.toCharArray();
        for (int index = 0; index < characters.length; index++) {
            char character = characters[index];
            Glyph glyph = glyphs.getOrDefault(character, glyphs.get('?'));
            if (glyph != null) {
                width += glyph.advance * scale;
                if (index + 1 < characters.length) width += tracking;
            }
        }
        return width;
    }

    public void drawPlainTracked(String text, float x, float y, int color,
                                 float height, float tracking) {
        draw(text, x, y, color, height, tracking);
    }

    private void draw(String text, float x, float y, int color, float height, boolean ignored) {
        draw(text, x, y, color, height, 0.0F);
    }

    private void draw(String text, float x, float y, int color, float height, float tracking) {
        if (text == null || text.isEmpty()) {
            return;
        }

        float scale = height / nativeHeight;
        float alpha = (float)(color >>> 24 & 255) / 255.0F;
        float red = (float)(color >>> 16 & 255) / 255.0F;
        float green = (float)(color >>> 8 & 255) / 255.0F;
        float blue = (float)(color & 255) / 255.0F;

        // Other legacy mods can change raw GL behind Minecraft's state cache.
        // Establish authoritative texture/blend state and restore it without
        // touching that cache. An untextured or unblended quad becomes a solid bar.
        int previousUnit = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        int previousProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        GL20.glUseProgram(0);
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_TEXTURE_BIT
                | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT);
        for (int i = GL11.glGetInteger(GL13.GL_MAX_TEXTURE_UNITS) - 1; i > 0; i--) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + i);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
        }
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        int previousMatrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        GL11.glMatrixMode(GL11.GL_TEXTURE); GL11.glPushMatrix(); GL11.glLoadIdentity();
        GL11.glMatrixMode(previousMatrixMode);
        GL11.glDisable(GL11.GL_TEXTURE_GEN_S); GL11.glDisable(GL11.GL_TEXTURE_GEN_T);
        GL11.glDisable(GL11.GL_TEXTURE_GEN_R); GL11.glDisable(GL11.GL_TEXTURE_GEN_Q);
        GL11.glDisable(GL11.GL_DEPTH_TEST); GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_COLOR_LOGIC_OP);
        GL11.glTexEnvf(GL14.GL_TEXTURE_FILTER_CONTROL, GL14.GL_TEXTURE_LOD_BIAS, 0);
        GL14.glBlendEquation(GL14.GL_FUNC_ADD);
        GL11.glEnable(GL11.GL_BLEND);
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glColor4f(red, green, blue, alpha);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_FOG);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE);
        float cursor = x;
        float drawY = y;
        GL11.glBegin(GL11.GL_QUADS);
        for (char character : text.toCharArray()) {
            Glyph glyph = glyphs.getOrDefault(character, glyphs.get('?'));
            if (glyph == null) {
                continue;
            }

            float drawWidth = glyph.width * scale;
            float drawHeight = glyph.height * scale;
            // Preserve the full padded cell and continuous glyph geometry.
            float u0 = glyph.x / (float) ATLAS_SIZE;
            float v0 = glyph.y / (float) ATLAS_SIZE;
            float u1 = (glyph.x + glyph.width) / (float) ATLAS_SIZE;
            float v1 = (glyph.y + glyph.height) / (float) ATLAS_SIZE;

            GL11.glTexCoord2f(u0, v0);
            float drawLeft = cursor;
            float drawRight = cursor + drawWidth;
            GL11.glVertex2f(drawLeft, drawY);
            GL11.glTexCoord2f(u0, v1);
            GL11.glVertex2f(drawLeft, drawY + drawHeight);
            GL11.glTexCoord2f(u1, v1);
            GL11.glVertex2f(drawRight, drawY + drawHeight);
            GL11.glTexCoord2f(u1, v0);
            GL11.glVertex2f(drawRight, drawY);
            cursor += glyph.advance * scale + tracking;
        }
        GL11.glEnd();
        GL11.glMatrixMode(GL11.GL_TEXTURE); GL11.glPopMatrix();
        GL11.glMatrixMode(previousMatrixMode);
        GL11.glPopAttrib();
        GL20.glUseProgram(previousProgram);

        GL13.glActiveTexture(previousUnit);
    }

    private static void configure(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    private static int withAlpha(int base, int source, float multiplier) {
        int alpha = Math.max(0, Math.min(255, Math.round((source >>> 24 & 255) * multiplier)));
        return alpha << 24 | base & 0x00FFFFFF;
    }

    private static String buildGlyphSet() {
        StringBuilder builder = new StringBuilder();
        for (char character = 32; character <= 126; character++) {
            builder.append(character);
        }
        for (char character = '\u0400'; character <= '\u04FF'; character++) {
            builder.append(character);
        }
        builder.append("—–…•×✓★☆");
        return builder.toString();
    }

    private static final class Glyph {
        private final int x;
        private final int y;
        private final int width;
        private final int height;
        private final float advance;

        private Glyph(int x, int y, int width, int height, float advance) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.advance = advance;
        }
    }
}
