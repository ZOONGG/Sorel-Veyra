package dev.veyra.client.clickgui;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class FontFacesTest {
    private static final String SAMPLE = "Veyra AimAssist Настройки 0123456789";

    @Test
    void bundledFacesHaveRealWeightContrastAndCyrillicCoverage() throws Exception {
        Font regular = load("Regular");
        Font semibold = load("SemiBold");
        assertEquals(-1, regular.canDisplayUpTo(SAMPLE));
        assertEquals(-1, semibold.canDisplayUpTo(SAMPLE));
        int[] light = raster(regular);
        int[] heavy = raster(semibold);
        assertFalse(Arrays.equals(light, heavy), "Java must not collapse both faces to the same raster");
        assertTrue(coverage(heavy) > coverage(light), "Semibold must have greater stroke coverage");
    }

    private Font load(String face) throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/assets/veyra/fonts/Inter-" + face + ".ttf")) {
            assertNotNull(input, "Missing bundled face: " + face);
            return Font.createFont(Font.TRUETYPE_FONT, input).deriveFont(36.0F);
        }
    }

    private int[] raster(Font font) {
        BufferedImage image = new BufferedImage(1000, 80, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setFont(font);
        graphics.setColor(Color.WHITE);
        graphics.drawString(SAMPLE, 4, 50);
        graphics.dispose();
        return image.getRGB(0, 0, 1000, 80, null, 0, 1000);
    }

    private long coverage(int[] pixels) {
        long sum = 0;
        for (int pixel : pixels) sum += pixel >>> 24;
        return sum;
    }
}
