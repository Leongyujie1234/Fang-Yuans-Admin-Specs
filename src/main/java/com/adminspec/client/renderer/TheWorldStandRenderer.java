package com.adminspec.client.renderer;

import com.adminspec.entity.TheWorldStandEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mod.azure.azurelib.common.api.client.renderer.GeoEntityRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public class TheWorldStandRenderer extends GeoEntityRenderer<TheWorldStandEntity> {
    public TheWorldStandRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new TheWorldStandModel());
    }

    @Override
    public RenderType getRenderType(TheWorldStandEntity animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }
}
