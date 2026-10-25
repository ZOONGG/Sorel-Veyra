package dev.veyra.client.player.bridge;

/** Runtime facts supplied by Minecraft; the timing engine stays game-independent. */
public record BridgeAssistContext(
        long nowMs,
        boolean valid,
        boolean edgeReached,
        boolean emergencyEdgeReached,
        boolean physicalSneakDown
) {
}
