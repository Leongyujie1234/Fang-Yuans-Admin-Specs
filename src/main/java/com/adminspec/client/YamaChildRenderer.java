package com.adminspec.client;

import com.adminspec.entity.YamaChildEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Zombie;

public final class YamaChildRenderer extends ZombieRenderer {
    private static final ResourceLocation BLACK_TEXTURE = ResourceLocation.fromNamespaceAndPath("adminspec", "textures/entity/yama_child.png");

    public YamaChildRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public ResourceLocation getTextureLocation(Zombie entity) {
        return BLACK_TEXTURE;
    }

    @Override
    public void render(Zombie entity, float entityYaw, float partialTick, com.mojang.blaze3d.vertex.PoseStack poseStack, net.minecraft.client.renderer.MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
        // Emit fuse sparks on the client when the fuse is lit, so the "trigger fuses"
        // behaviour is visible even if server-side smoke particles are sparse.
        if (entity instanceof YamaChildEntity yc && yc.isFuseLit() && entity.level().random.nextFloat() < 0.5f) {
            double dx = entity.getX() + (entity.level().random.nextDouble() - 0.5) * 0.4;
            double dy = entity.getY() + entity.level().random.nextDouble() * 1.0;
            double dz = entity.getZ() + (entity.level().random.nextDouble() - 0.5) * 0.4;
            entity.level().addParticle(ParticleTypes.FLAME, dx, dy, dz, 0.0, 0.05, 0.0);
            entity.level().addParticle(ParticleTypes.SMOKE, dx, dy + 0.1, dz, 0.0, 0.08, 0.0);
        }
    }
}
