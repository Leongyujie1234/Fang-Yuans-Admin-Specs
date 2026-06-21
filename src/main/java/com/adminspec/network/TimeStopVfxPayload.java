package com.adminspec.network;

import com.adminspec.client.TimeStopOverlayHandler;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TimeStopVfxPayload(UUID playerId, boolean active) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<TimeStopVfxPayload> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("adminspec", "timestop_vfx"));

    public static final StreamCodec<FriendlyByteBuf, TimeStopVfxPayload> STREAM_CODEC = StreamCodec.composite(
        UUIDUtil.STREAM_CODEC, TimeStopVfxPayload::playerId,
        ByteBufCodecs.BOOL, TimeStopVfxPayload::active,
        TimeStopVfxPayload::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TimeStopVfxPayload payload, IPayloadContext ctx) {
        if (!FMLEnvironment.dist.isClient()) return;
        ctx.enqueueWork(() -> TimeStopOverlayHandler.update(payload));
    }
}
