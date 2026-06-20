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

    private DragonBreathHandler() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (localCooldown > 0) localCooldown--;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        ClientSpecState.Snapshot snap = ClientSpecState.get(mc.player.getUUID());
        if (snap == null || !snap.dragonFormActive) return;

        if (mc.options.keyAttack.isDown() && localCooldown == 0) {
            LOGGER.info("[AdminSpec] Dragon breath triggered");
            // Send to server for damage + VFX broadcast
            PacketDistributor.sendToServer(new DragonBreathPayload());
            // Spawn immediate client-side particles so player sees something
            spawnClientBreathVfx(mc.level, mc.player);
            localCooldown = 60;
        }
    }

    private static void spawnClientBreathVfx(ClientLevel level, net.minecraft.client.player.LocalPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        for (double d = 0.3; d < 16.0; d += 0.5) {
            Vec3 pos = eye.add(look.scale(d));
            double spread = d * 0.06;
            level.addParticle(ParticleTypes.CRIT,
                pos.x + (Math.random() - 0.5) * spread,
                pos.y + (Math.random() - 0.5) * spread,
                pos.z + (Math.random() - 0.5) * spread,
                look.x * 0.3, look.y * 0.3, look.z * 0.3);
            level.addParticle(ParticleTypes.ENCHANTED_HIT,
                pos.x + (Math.random() - 0.5) * spread,
                pos.y + (Math.random() - 0.5) * spread,
                pos.z + (Math.random() - 0.5) * spread,
                0, 0, 0);
            if (d % 1.5 < 0.5) {
                level.addParticle(ParticleTypes.SWEEP_ATTACK,
                    pos.x, pos.y, pos.z, 0, 0, 0);
            }
        }
        // Tip burst
        Vec3 tip = eye.add(look.scale(16.0));
        level.addParticle(ParticleTypes.FLASH, tip.x, tip.y, tip.z, 0, 0, 0);
        for (int i = 0; i < 6; i++) {
            level.addParticle(ParticleTypes.EXPLOSION,
                tip.x + (Math.random() - 0.5) * 2,
                tip.y + (Math.random() - 0.5) * 2,
                tip.z + (Math.random() - 0.5) * 2,
                0, 0, 0);
        }
    }
}
