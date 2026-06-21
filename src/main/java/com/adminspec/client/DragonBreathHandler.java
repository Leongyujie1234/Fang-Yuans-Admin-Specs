package com.adminspec.client;

import com.adminspec.network.DragonBreathPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

@EventBusSubscriber(modid="adminspec", bus=EventBusSubscriber.Bus.GAME, value={Dist.CLIENT})
public final class DragonBreathHandler {
    private static final DustParticleOptions DUST = new DustParticleOptions(new Vector3f(0.3f, 0.7f, 1.0f), 1.5f);

    private static int localCooldown = 0;

    private DragonBreathHandler() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (localCooldown > 0) localCooldown--;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        ClientSpecState.Snapshot snap = ClientSpecState.get(mc.player.getUUID());
        if (snap == null || !snap.dragonFormActive) return;

        if (mc.options.keyAttack.isDown()) {
            // Fire breath continuously — server rate-limits via cooldown; client just spams particles
            if (localCooldown == 0) {
                PacketDistributor.sendToServer(new DragonBreathPayload());
                localCooldown = 2;
            }
            spawnClientBreathVfx(mc.level, mc.player);
        }
    }

    private static void spawnClientBreathVfx(ClientLevel level, net.minecraft.client.player.LocalPlayer player) {
        double x = player.getX();
        double y = player.getY() + 0.6;
        double z = player.getZ();

        // Dense visible cloud: CAMPFIRE_SIGNAL_SMOKE + DUST
        level.addParticle(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, x, y, z, 0, 0.15, 0);
        level.addParticle(DUST, x + (Math.random() - 0.5) * 0.8, y + Math.random() * 1.2, z + (Math.random() - 0.5) * 0.8, 0, 0, 0);
        level.addParticle(ParticleTypes.FLAME, x + (Math.random() - 0.5) * 0.6, y + Math.random() * 1.0, z + (Math.random() - 0.5) * 0.6, 0, 0.05, 0);
        level.addParticle(ParticleTypes.LAVA, x + (Math.random() - 0.5) * 0.4, y + Math.random() * 1.5, z + (Math.random() - 0.5) * 0.4, 0, 0, 0);

        // Beam indicator
        double lx = player.getLookAngle().x;
        double lz = player.getLookAngle().z;
        level.addParticle(ParticleTypes.CRIT, x + lx * 2, y + 0.3, z + lz * 2, lx * 0.5, 0, lz * 0.5);
        level.addParticle(ParticleTypes.ENCHANTED_HIT, x + lx * 3, y + 0.5, z + lz * 3, lx * 0.3, 0, lz * 0.3);
    }
}
