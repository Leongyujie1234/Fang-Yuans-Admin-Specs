package com.adminspec.client.renderer;

import com.adminspec.entity.TheWorldStandEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class TheWorldStandRenderer extends MobRenderer<TheWorldStandEntity, HumanoidModel<TheWorldStandEntity>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("adminspec", "textures/entity/stand/the_world.png");

    public TheWorldStandRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 0.3f);
    }

    @Override
    public ResourceLocation getTextureLocation(TheWorldStandEntity entity) {
        return TEXTURE;
    }
}
