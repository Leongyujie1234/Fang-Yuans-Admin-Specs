package com.adminspec.client;

import com.adminspec.network.DragonBreathPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sends the dragon breath packet to the server while M1 is held in dragon form.
 * Rate-limited to once per 60 ticks (3 seconds) by server-side cooldown.
 * Also spawns immediate client-side particles for instant feedback.
 */
@EventBusSubscriber(modid="adminspec", bus=EventBusSubscriber.Bus.GAME, value={Dist.CLIENT})
public final class DragonBreathHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("adminspec-breath");

    private static int localCooldown = 0;
    private static boolean debugOnce = false;

    private DragonBreathHandler() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (localCooldown > 0) localCooldown--;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        ClientSpecState.Snapshot snap = ClientSpecState.get(mc.player.getUUID());
        if (snap == null) {
            if (!debugOnce) {
                LOGGER.info("[AdminSpec] DragonBreathHandler: no state snapshot for {}", mc.player.getUUID());
                debugOnce = true;
            }
            return;
        }
        debugOnce = false;

        if (!snap.dragonFormActive) {
            return;
        }

        // Always spawn client VFX when M1 is held (no local cooldown for particles)
        if (mc.options.keyAttack.isDown()) {
            if (localCooldown == 0) {
                LOGGER.info("[AdminSpec] Dragon breath triggered (cooldown available)");
                PacketDistributor.sendToServer(new DragonBreathPayload());
                localCooldown = 60;
            }
            spawnClientBreathVfx(mc.level, mc.player);
        }
    }

    private static void spawnClientBreathVfx(ClientLevel level, net.minecraft.client.player.LocalPlayer player) {
        // ULTRA-SIMPLE TEST: flame particle at player feet every tick
        level.addParticle(ParticleTypes.FLAME,
            player.getX(), player.getY() + 0.5, player.getZ(),
            0, 0.1, 0);
        net.minecraft.network.chat.Component msg = net.minecraft.network.chat.Component.literal("§c[DEBUG] Breath VFX spawned");
        if (player != null) player.sendSystemMessage(msg);
    }
}
