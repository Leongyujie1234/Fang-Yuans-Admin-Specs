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
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        // Spawn a guaranteed-visible beacon of CAMPFIRE_SIGNAL_SMOKE (very tall, dense)
        for (double d = 0.3; d < 16.0; d += 0.5) {
            Vec3 pos = eye.add(look.scale(d));
            level.addParticle(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,
                pos.x, pos.y, pos.z,
                look.x * 0.2, look.y * 0.2, look.z * 0.2);
            level.addParticle(ParticleTypes.CRIT,
                pos.x + (Math.random() - 0.5) * 0.5,
                pos.y + (Math.random() - 0.5) * 0.5,
                pos.z + (Math.random() - 0.5) * 0.5,
                look.x * 0.4, look.y * 0.4, look.z * 0.4);
            level.addParticle(ParticleTypes.SONIC_BOOM,
                pos.x, pos.y, pos.z,
                0, 0, 0);
        }
        // Guaranteed visible lava splash at mouth
        for (int i = 0; i < 10; i++) {
            level.addParticle(ParticleTypes.LAVA,
                eye.x + (Math.random() - 0.5) * 0.5,
                eye.y + (Math.random() - 0.5) * 0.5,
                eye.z + (Math.random() - 0.5) * 0.5,
                0, 0, 0);
        }
        // Tip burst
        Vec3 tip = eye.add(look.scale(16.0));
        level.addParticle(ParticleTypes.FLASH, tip.x, tip.y, tip.z, 0, 0, 0);
        for (int i = 0; i < 12; i++) {
            level.addParticle(ParticleTypes.EXPLOSION,
                tip.x + (Math.random() - 0.5) * 2.5,
                tip.y + (Math.random() - 0.5) * 2.5,
                tip.z + (Math.random() - 0.5) * 2.5,
                0, 0, 0);
        }
        level.playLocalSound(eye.x, eye.y, eye.z,
            net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_SWEEP,
            net.minecraft.sounds.SoundSource.PLAYERS, 1.5f, 0.7f, false);
    }
}
