package dev.veyra.client.clickgui.font;

import java.awt.Font;
import java.io.InputStream;

public final class VeyraFonts {
    private static final VeyraFont REGULAR = new VeyraFont(load(false));
    private static final VeyraFont SEMIBOLD = new VeyraFont(load(true));

    private VeyraFonts() {
    }

    public static VeyraFont regular() {
        return REGULAR;
    }

    public static VeyraFont semibold() {
        return SEMIBOLD;
    }

    private static Font load(boolean semibold) {
        // Java 17 ignores the variable font's wght axis for these attributes.
        // Static faces retain the intended 400/600 hierarchy without synthetic bold.
        String face = semibold ? "SemiBold" : "Regular";
        try (InputStream stream = VeyraFonts.class.getResourceAsStream(
                "/assets/veyra/fonts/Inter-" + face + ".ttf")) {
            if (stream == null) throw new IllegalStateException("Bundled Inter face is missing: " + face);
            return Font.createFont(Font.TRUETYPE_FONT, stream).deriveFont(36.0F);
        } catch (Exception exception) {
            return new Font("SansSerif", semibold ? Font.BOLD : Font.PLAIN, 36);
        }
    }
}
