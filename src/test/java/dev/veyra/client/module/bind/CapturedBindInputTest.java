package dev.veyra.client.module.bind;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CapturedBindInputTest {
    @Test
    void captureIsSuppressedForAllModulesUntilRelease() {
        CapturedBindInput.suppressUntilRelease(47);
        assertTrue(CapturedBindInput.isSuppressed(47, true));
        assertTrue(CapturedBindInput.isSuppressed(47, true));
        assertFalse(CapturedBindInput.isSuppressed(48, true));
        assertFalse(CapturedBindInput.isSuppressed(47, false));
        assertFalse(CapturedBindInput.isSuppressed(47, true));
    }

    @Test
    void mouseCaptureHasTheSameReleaseSemantics() {
        CapturedBindInput.suppressUntilRelease(-97);
        assertTrue(CapturedBindInput.isSuppressed(-97, true));
        assertFalse(CapturedBindInput.isSuppressed(-97, false));
        assertFalse(CapturedBindInput.isSuppressed(-97, true));
    }
}
