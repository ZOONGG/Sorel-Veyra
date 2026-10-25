package dev.veyra.client.module.modules.movement;

import dev.veyra.client.coordination.input.InputLeaseManager;
import dev.veyra.client.module.Module;
import dev.veyra.client.module.setting.impl.DescriptionSetting;
import dev.veyra.client.utils.player.PlayerUtils;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.weavemc.api.event.PacketEvent;
import net.weavemc.api.event.SubscribeEvent;
import net.weavemc.api.event.TickEvent;

/** Requests vanilla sprint input; vanilla owns sprint eligibility and state. */
public class Sprint extends Module {
    private static final String INPUT_OWNER = "auto-sprint";
    private static final int VANILLA_KNOCKBACK_TICKS = 12;
    private static Sprint activeInstance;
    private volatile int vanillaControlUntilTick = Integer.MIN_VALUE;

    public Sprint() {
        super("Sprint", ModuleCategory.Movement, 0);
        registerSetting(new DescriptionSetting("Automatically sprint forward when vanilla allows it."));
    }

    /** Retains WTap's input-only sprint reset and the existing damage pause. */
    public void pauseAutomationUntilTick(int tick) {
        vanillaControlUntilTick = Math.max(vanillaControlUntilTick, tick);
        releaseInput();
    }

    @SubscribeEvent
    public void onVelocity(PacketEvent.Receive event) {
        if (PlayerUtils.isPlayerInGame()
                && event.getPacket() instanceof S12PacketEntityVelocity packet
                && packet.getEntityID() == mc.thePlayer.getEntityId()) {
            pauseAutomationUntilTick(mc.thePlayer.ticksExisted + VANILLA_KNOCKBACK_TICKS);
        }
    }

    /** Called after this tick's movement input, before vanilla tests sprint. */
    public static void updateVanillaSprintInput() {
        Sprint instance = activeInstance;
        if (instance == null) return;
        instance.releaseInput();
        if (!PlayerUtils.isPlayerInGame() || !mc.inGameHasFocus || mc.currentScreen != null
                || mc.thePlayer.movementInput == null
                || mc.thePlayer.movementInput.moveForward < 0.8F
                || mc.thePlayer.movementInput.sneak || mc.thePlayer.isUsingItem()
                || mc.thePlayer.isRiding() || mc.thePlayer.isCollidedHorizontally
                || mc.thePlayer.hurtTime > 0
                || mc.thePlayer.ticksExisted <= instance.vanillaControlUntilTick) return;
        InputLeaseManager.set(INPUT_OWNER, mc.gameSettings.keyBindSprint.getKeyCode(), true, 40);
    }

    @SubscribeEvent
    public void onTick(TickEvent.Pre event) {
        if (!PlayerUtils.isPlayerInGame() || !mc.inGameHasFocus || mc.currentScreen != null) releaseInput();
    }

    private void releaseInput() {
        InputLeaseManager.releaseOwner(INPUT_OWNER, mc.currentScreen == null && mc.inGameHasFocus);
    }

    @Override
    public void onEnable() {
        vanillaControlUntilTick = Integer.MIN_VALUE;
        activeInstance = this;
    }

    @Override
    public void onDisable() {
        activeInstance = null;
        vanillaControlUntilTick = Integer.MIN_VALUE;
        releaseInput();
    }
}
