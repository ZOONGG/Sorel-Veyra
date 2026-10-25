package dev.veyra.client.module.bind;

import java.util.HashSet;
import java.util.Set;

/** Captured presses cannot trigger any module until the input is released. */
public final class CapturedBindInput {
    private static final Set<Integer> SUPPRESSED = new HashSet<>();

    public static synchronized void suppressUntilRelease(int code) {
        if (code != 0) SUPPRESSED.add(code);
    }

    public static synchronized boolean isSuppressed(int code, boolean down) {
        if (!down) {
            SUPPRESSED.remove(code);
            return false;
        }
        return SUPPRESSED.contains(code);
    }

    private CapturedBindInput() {}
}
