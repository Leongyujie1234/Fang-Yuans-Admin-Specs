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

import java.util.Random;

@EventBusSubscriber(modid="adminspec", bus=EventBusSubscriber.Bus.GAME, value={Dist.CLIENT})
public final class ReverseFlowParticleHandler {
    private static final Random RANDOM = new Random();

    private ReverseFlowParticleHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        for (Player player : level.players()) {
            ClientSpecState.Snapshot snap = ClientSpecState.get(player.getUUID());
            if (snap == null || !snap.reverseFlowActive) continue;
            spawnRobe(level, player);
        }
    }

    private static void spawnRobe(ClientLevel level, Player player) {
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();
        int ticks = player.tickCount;

        // Dense blue dust ring using END_ROD (glowing white sparkle that works)
        for (int i = 0; i < 6; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2.0;
            double yOffset = RANDOM.nextDouble() * 2.0;
            double radius = 0.5 + RANDOM.nextDouble() * 0.5;
            double dx = px + Math.cos(angle) * radius;
            double dz = pz + Math.sin(angle) * radius;
            level.addParticle(ParticleTypes.END_ROD, dx, py + yOffset, dz, 0, 0, 0);
            level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, dx, py + yOffset, dz, 0, 0.02, 0);
        }

        // Rising water helix
        for (int i = 0; i < 3; i++) {
            double angle = (ticks * 0.2) + (i * Math.PI * 2.0 / 3.0);
            double yOffset = ((ticks + i * 8) % 24) / 24.0 * 2.0;
            double radius = 0.6 + Math.sin(ticks * 0.05 + i) * 0.1;
            double dx = px + Math.cos(angle) * radius;
            double dz = pz + Math.sin(angle) * radius;
            level.addParticle(ParticleTypes.SPLASH, dx, py + yOffset, dz, 0.0, 0.05, 0.0);
            level.addParticle(ParticleTypes.FALLING_WATER, dx, py + yOffset, dz, 0.0, -0.01, 0.0);
        }

        // Descending helix with blue INSTANT_EFFECT swirls
        for (int i = 0; i < 2; i++) {
            double angle = (-ticks * 0.15) + (i * Math.PI);
            double yOffset = 2.0 - (((ticks + i * 12) % 24) / 24.0 * 2.0);
            double radius = 0.55;
            double dx = px + Math.cos(angle) * radius;
            double dz = pz + Math.sin(angle) * radius;
            level.addParticle(ParticleTypes.DRIPPING_WATER, dx, py + yOffset, dz, 0.0, 0.0, 0.0);
            if (RANDOM.nextFloat() < 0.4f) {
                level.addParticle(ParticleTypes.INSTANT_EFFECT, dx, py + yOffset, dz, 0.1, 0.5, 0.9);
                level.addParticle(ParticleTypes.WAX_ON, dx, py + yOffset, dz, 0, 0, 0);
            }
        }

        // Pulsing blue ring at chest level using END_ROD
        double ringRadius = 0.6 + Math.sin(ticks * 0.1) * 0.15;
        for (int i = 0; i < 4; i++) {
            double angle = (ticks * 0.15) + (i * Math.PI / 2.0);
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
