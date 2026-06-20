/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.entity.EntityRenderer
 *  net.minecraft.client.renderer.entity.EntityRendererProvider$Context
 *  net.minecraft.client.renderer.texture.OverlayTexture
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.item.ItemDisplayContext
 *  net.minecraft.world.item.ItemStack
 */
package com.adminspec.client;

import com.adminspec.entity.FlyingHeadEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class FlyingHeadRenderer
extends EntityRenderer<FlyingHeadEntity> {
    public FlyingHeadRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.0f;
    }

    public ResourceLocation getTextureLocation(FlyingHeadEntity entity) {
        return ResourceLocation.fromNamespaceAndPath((String)"minecraft", (String)"textures/misc/white.png");
    }

    public void render(FlyingHeadEntity entity, float yaw, float partialTicks, PoseStack pose, MultiBufferSource buffers, int packedLight) {
        super.render(entity, yaw, partialTicks, pose, buffers, packedLight);
        ItemStack headItem = entity.getHeadItem();
        if (headItem.isEmpty()) {
            return;
        }
        pose.pushPose();
        pose.scale(1.5f, 1.5f, 1.5f);
        Minecraft.getInstance().getItemRenderer().renderStatic(headItem, ItemDisplayContext.GROUND, packedLight, OverlayTexture.NO_OVERLAY, pose, buffers, entity.level(), 0);
        pose.popPose();
    }
}

