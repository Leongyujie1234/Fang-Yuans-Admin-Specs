/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.Registry
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.entity.player.Player
 *  net.neoforged.neoforge.attachment.AttachmentType
 *  net.neoforged.neoforge.event.entity.player.PlayerEvent$Clone
 *  net.neoforged.neoforge.event.entity.player.PlayerEvent$PlayerLoggedInEvent
 *  net.neoforged.neoforge.event.entity.player.PlayerEvent$PlayerRespawnEvent
 *  net.neoforged.neoforge.registries.DeferredRegister
 *  net.neoforged.neoforge.registries.NeoForgeRegistries
 */
package com.adminspec.capability;

import com.adminspec.capability.PlayerSpecData;
import com.adminspec.network.SpecStatePayload;
import com.adminspec.spec.Spec;
import com.adminspec.spec.SpecRegistry;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class PlayerSpecCapability {
    public static final ResourceLocation CAP_ID = ResourceLocation.fromNamespaceAndPath((String)"adminspec", (String)"player_spec");
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create((Registry)NeoForgeRegistries.ATTACHMENT_TYPES, (String)"adminspec");
    public static final Supplier<AttachmentType<PlayerSpecData>> PLAYER_SPEC = ATTACHMENT_TYPES.register("player_spec", () -> AttachmentType.serializable(() -> new PlayerSpecData()).copyOnDeath().build());

    private PlayerSpecCapability() {
    }

    public static PlayerSpecData get(Player player) {
        return (PlayerSpecData)player.getData(PLAYER_SPEC);
    }

    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player player = event.getEntity();
        if (!(player instanceof Player)) {
            return;
        }
        Player newPlayer = player;
        Player original = event.getOriginal();
        PlayerSpecData oldData = (PlayerSpecData)original.getData(PLAYER_SPEC);
        PlayerSpecData newData = (PlayerSpecData)newPlayer.getData(PLAYER_SPEC);
        newData.copyFrom(oldData);
    }

    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Spec spec;
        Player player = event.getEntity();
        if (!(player instanceof Player)) {
            return;
        }
        Player player2 = player;
        PlayerSpecData data = PlayerSpecCapability.get(player2);
        if (data.getSpecId() != null && (spec = SpecRegistry.get(data.getSpecId())) == null) {
            data.setSpecId(null);
        }
        SpecStatePayload.broadcast(player2);
    }

    public static void onPlayerStartTracking(PlayerEvent.StartTracking event) {
        Entity target = event.getTarget();
        if (target instanceof ServerPlayer) {
            ServerPlayer trackedPlayer = (ServerPlayer)target;
            PlayerSpecData data = PlayerSpecCapability.get(trackedPlayer);
            ServerPlayer trackingPlayer = (ServerPlayer)event.getEntity();
            PacketDistributor.sendToPlayer(trackingPlayer, (CustomPacketPayload)new SpecStatePayload(trackedPlayer.getUUID(), data.isReverseFlowActive(), data.getReverseFlowCapacity(), data.isDragonFormActive(), data.getDragonFormTicks()), new CustomPacketPayload[0]);
        }
    }

    public static void tickPlayer(Player player) {
        if (player.level().isClientSide()) {
            return;
        }
        PlayerSpecData data = PlayerSpecCapability.get(player);
        if (data.getSpecId() == null) {
            return;
        }
        Spec spec = SpecRegistry.get(data.getSpecId());
        if (spec == null) {
            return;
        }
        data.serverTick(player, spec);
    }
}

