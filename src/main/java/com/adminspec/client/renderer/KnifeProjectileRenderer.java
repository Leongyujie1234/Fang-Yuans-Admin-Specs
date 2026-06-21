package com.adminspec.client.renderer;

import com.adminspec.AdminSpecMod;
import com.adminspec.entity.KnifeProjectileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class KnifeProjectileRenderer extends EntityRenderer<KnifeProjectileEntity> {
    private static final ResourceLocation MISSING = ResourceLocation.fromNamespaceAndPath("adminspec", "textures/item/knife.png");

    public KnifeProjectileRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.0f;
    }

    @Override
    public ResourceLocation getTextureLocation(KnifeProjectileEntity entity) {
        return MISSING;
    }

    @Override
    public void render(KnifeProjectileEntity entity, float yaw, float partialTicks, PoseStack pose, MultiBufferSource buf, int light) {
        super.render(entity, yaw, partialTicks, pose, buf, light);
        pose.pushPose();
        pose.scale(0.8f, 0.8f, 0.8f);
        Minecraft.getInstance().getItemRenderer().renderStatic(
            new ItemStack(com.adminspec.item.ModItems.KNIFE.get()),
            ItemDisplayContext.GROUND, light, OverlayTexture.NO_OVERLAY, pose, buf, entity.level(), 0);
        pose.popPose();
    }
}
