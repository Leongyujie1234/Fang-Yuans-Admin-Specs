package com.adminspec.client;

import com.adminspec.client.ClientSpecState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

@EventBusSubscriber(modid="adminspec", bus=EventBusSubscriber.Bus.GAME, value={Dist.CLIENT})
public final class ReverseFlowParticleHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("adminspec-reverseflow");
    private static final Random RANDOM = new Random();
    private static boolean debugOnce = false;

    private ReverseFlowParticleHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) return;

        // ULTRA-SIMPLE TEST: always spawn a flame at player position
        level.addParticle(ParticleTypes.FLAME,
            mc.player.getX(), mc.player.getY() + 0.5, mc.player.getZ(),
            0, 0.1, 0);
        mc.player.sendSystemMessage(
            net.minecraft.network.chat.Component.literal("§b[DEBUG] ReverseFlow handler ticking"));

        // Also check real state and spawn robe
        for (Player player : level.players()) {
            ClientSpecState.Snapshot snap = ClientSpecState.get(player.getUUID());
            if (snap == null) continue;
            if (!snap.reverseFlowActive) continue;
            spawnRobe(level, player);
            LOGGER.info("[AdminSpec] ReverseFlow active for {}", player.getName().getString());
        }
    }

    private static void spawnRobe(ClientLevel level, Player player) {
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();
        int ticks = player.tickCount;

        // Guaranteed visible LAVA + CAMPFIRE_SIGNAL_SMOKE at player position
        level.addParticle(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, px, py, pz, 0, 0.1, 0);
        if (ticks % 3 == 0) {
            level.addParticle(ParticleTypes.LAVA,
                px + (Math.random() - 0.5) * 0.5,
                py + Math.random() * 1.8,
                pz + (Math.random() - 0.5) * 0.5,
                0, 0, 0);
        }

        // Dense glowing ring around the body
        for (int i = 0; i < 10; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2.0;
            double yOffset = RANDOM.nextDouble() * 2.2;
            double radius = 0.5 + RANDOM.nextDouble() * 0.6;
            double dx = px + Math.cos(angle) * radius;
            double dz = pz + Math.sin(angle) * radius;
            level.addParticle(ParticleTypes.END_ROD, dx, py + yOffset, dz, 0, 0, 0);
            level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, dx, py + yOffset, dz, 0, 0.02, 0);
            if (i % 2 == 0) {
                level.addParticle(ParticleTypes.WAX_ON, dx, py + yOffset, dz, 0, 0, 0);
            }
        }

        // Helix 1: rising water
        for (int i = 0; i < 3; i++) {
            double angle = (ticks * 0.2) + (i * Math.PI * 2.0 / 3.0);
            double yOffset = ((ticks + i * 8) % 24) / 24.0 * 2.0;
            double radius = 0.6 + Math.sin(ticks * 0.05 + i) * 0.1;
            double dx = px + Math.cos(angle) * radius;
            double dz = pz + Math.sin(angle) * radius;
            level.addParticle(ParticleTypes.SPLASH, dx, py + yOffset, dz, 0.0, 0.05, 0.0);
            level.addParticle(ParticleTypes.FALLING_WATER, dx, py + yOffset, dz, 0.0, -0.01, 0.0);
        }

        // Helix 2: descending
        for (int i = 0; i < 2; i++) {
            double angle = (-ticks * 0.15) + (i * Math.PI);
            double yOffset = 2.0 - (((ticks + i * 12) % 24) / 24.0 * 2.0);
            double radius = 0.55;
            double dx = px + Math.cos(angle) * radius;
            double dz = pz + Math.sin(angle) * radius;
            level.addParticle(ParticleTypes.DRIPPING_WATER, dx, py + yOffset, dz, 0.0, 0.0, 0.0);
            level.addParticle(ParticleTypes.END_ROD, dx, py + yOffset, dz, 0, 0, 0);
        }

        // Pulsing ring at chest level
        double ringRadius = 0.6 + Math.sin(ticks * 0.1) * 0.15;
        for (int i = 0; i < 6; i++) {
            double angle = (ticks * 0.15) + (i * Math.PI / 3.0);
            double dx = px + Math.cos(angle) * ringRadius;
            double dz = pz + Math.sin(angle) * ringRadius;
            level.addParticle(ParticleTypes.END_ROD, dx, py + 1.0, dz, 0, 0, 0);
            level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, dx, py + 1.0, dz, 0, 0.01, 0);
        }

        // Ground splash
        if (ticks % 2 == 0) {
            double groundAngle = RANDOM.nextDouble() * Math.PI * 2.0;
            double groundRadius = 0.7 + RANDOM.nextDouble() * 0.3;
            double dx = px + Math.cos(groundAngle) * groundRadius;
            double dz = pz + Math.sin(groundAngle) * groundRadius;
            level.addParticle(ParticleTypes.SPLASH, dx, py + 0.05, dz, 0.0, 0.02, 0.0);
            level.addParticle(ParticleTypes.END_ROD, dx, py + 0.05, dz, 0, 0.05, 0);
        }
    }
}
