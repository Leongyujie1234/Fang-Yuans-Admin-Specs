package com.adminspec.network;

import com.adminspec.capability.PlayerSpecCapability;
import com.adminspec.capability.PlayerSpecData;
import com.adminspec.client.ClientSpecState;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SpecStatePayload(UUID playerId, boolean reverseFlowActive, float reverseFlowCapacity, boolean dragonFormActive, int dragonFormTicks) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<SpecStatePayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"adminspec", (String)"spec_state"));
    public static final StreamCodec<FriendlyByteBuf, SpecStatePayload> STREAM_CODEC = StreamCodec.composite(
        UUIDUtil.STREAM_CODEC, SpecStatePayload::playerId,
        ByteBufCodecs.BOOL, SpecStatePayload::reverseFlowActive,
        ByteBufCodecs.FLOAT, SpecStatePayload::reverseFlowCapacity,
        ByteBufCodecs.BOOL, SpecStatePayload::dragonFormActive,
        ByteBufCodecs.VAR_INT, SpecStatePayload::dragonFormTicks,
        SpecStatePayload::new
    );

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SpecStatePayload payload, IPayloadContext ctx) {
        if (!FMLEnvironment.dist.isClient()) {
            return;
        }
        ctx.enqueueWork(() -> ClientSpecState.update(payload));
    }

    public static void broadcast(Player player) {
        if (!(player instanceof ServerPlayer)) {
            return;
        }
        ServerPlayer sp = (ServerPlayer)player;
        PlayerSpecData data = PlayerSpecCapability.get(player);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf((Entity)sp, (CustomPacketPayload)new SpecStatePayload(player.getUUID(), data.isReverseFlowActive(), data.getReverseFlowCapacity(), data.isDragonFormActive(), data.getDragonFormTicks()), (CustomPacketPayload[])new CustomPacketPayload[0]);
    }
}
