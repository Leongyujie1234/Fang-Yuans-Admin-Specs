/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.KeyMapping
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.bus.api.SubscribeEvent
 *  net.neoforged.fml.common.EventBusSubscriber
 *  net.neoforged.fml.common.EventBusSubscriber$Bus
 *  net.neoforged.neoforge.client.event.ClientTickEvent$Post
 *  net.neoforged.neoforge.network.PacketDistributor
 */
package com.adminspec.client;

import com.adminspec.client.ClientBeamManager;
import com.adminspec.client.ClientSpecState;
import com.adminspec.client.MoveKeybinds;
import com.adminspec.network.ActivateMovePayload;
import com.adminspec.network.DragonFlightInputPayload;
import java.util.Map;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.client.event.InputEvent;

@EventBusSubscriber(modid="adminspec", bus=EventBusSubscriber.Bus.GAME, value={Dist.CLIENT})
public final class ClientKeyHandler {
    private ClientKeyHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (ClientSpecState.clientFlashTicks > 0) {
            ClientSpecState.clientFlashTicks--;
        }
        ClientBeamManager.tick();
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }

        for (Player p : mc.level.players()) {
            ClientSpecState.Snapshot snap = ClientSpecState.get(p.getUUID());
            if (snap != null && snap.dragonFormActive && snap.dragonFormTicks > 0 && snap.dragonFormTicks < 4) {
                double dist = player.distanceTo(p);
                if (dist < 32.0) {
                    if (p == player) {
                        ClientSpecState.clientFlashTicks = 15;
                    } else if (dist < 16.0) {
                        ClientSpecState.clientFlashTicks = 8;
                    }
                    for (int i = 0; i < 40; ++i) {
                        double rx = p.getX() + (p.getRandom().nextDouble() - 0.5) * 3.0;
                        double ry = p.getY() + p.getRandom().nextDouble() * 2.0;
                        double rz = p.getZ() + (p.getRandom().nextDouble() - 0.5) * 3.0;
                        mc.level.addParticle(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, rx, ry, rz, 0.0, 0.15, 0.0);
                        mc.level.addParticle(net.minecraft.core.particles.ParticleTypes.FLASH, rx, ry, rz, 0.0, 0.0, 0.0);
                        mc.level.addParticle(net.minecraft.core.particles.ParticleTypes.DRAGON_BREATH, rx, ry, rz, (p.getRandom().nextDouble() - 0.5) * 0.1, 0.05, (p.getRandom().nextDouble() - 0.5) * 0.1);
                    }
                    mc.level.playLocalSound(p.getX(), p.getY(), p.getZ(), net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(), net.minecraft.sounds.SoundSource.PLAYERS, 1.5f, 0.8f, false);
                }
                snap.dragonFormTicks = 5;
            }
        }

        // Dragon form flight input — send jump/sneak state to server each tick
        ClientSpecState.Snapshot localSnap = ClientSpecState.get(player.getUUID());
        if (localSnap != null && localSnap.dragonFormActive) {
            boolean jumping = mc.options.keyJump.isDown();
            boolean sneaking = mc.options.keyShift.isDown();
            PacketDistributor.sendToServer(
                new DragonFlightInputPayload(jumping, sneaking),
                new CustomPacketPayload[0]
            );
        }

        for (Map.Entry<String, KeyMapping> entry : MoveKeybinds.all().entrySet()) {
            if (!entry.getValue().consumeClick()) continue;
            ClientKeyHandler.sendActivate(entry.getKey());
        }
    }

    @SubscribeEvent
    public static void onInteractionKeyMappingTriggered(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
        ClientSpecState.Snapshot snap = ClientSpecState.get(player.getUUID());
        if (snap != null && snap.dragonFormActive) {
            if (event.getKeyMapping() == mc.options.keyAttack) {
                event.setCanceled(true);
                event.setSwingHand(true);
                PacketDistributor.sendToServer((CustomPacketPayload)new com.adminspec.network.DragonBreathPayload(), new CustomPacketPayload[0]);
            } else if (event.getKeyMapping() == mc.options.keyUse) {
                event.setCanceled(true);
            }
        }
    }

    private static void sendActivate(String moveId) {
        PacketDistributor.sendToServer((CustomPacketPayload)new ActivateMovePayload(moveId), (CustomPacketPayload[])new CustomPacketPayload[0]);
    }
}

