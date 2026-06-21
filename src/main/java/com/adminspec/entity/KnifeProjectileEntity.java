package com.adminspec.entity;

import com.adminspec.item.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class KnifeProjectileEntity extends AbstractArrow {
    private int ticksInAir;

    public KnifeProjectileEntity(EntityType<? extends AbstractArrow> type, Level level) {
        super(type, level);
    }

    public KnifeProjectileEntity(Level level, LivingEntity owner) {
        super(ModEntities.KNIFE.get(), owner, level, new ItemStack(ModItems.KNIFE.get()), ItemStack.EMPTY);
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(ModItems.KNIFE.get());
    }

    @Override
    public void tick() {
        super.tick();
        if (!inGround) ticksInAir++;
        if (ticksInAir > 200 && !level().isClientSide) discard();
        if (level().isClientSide) {
            level().addParticle(ParticleTypes.END_ROD, getX(), getY(), getZ(), 0, 0, 0);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (level().isClientSide) return;
        Entity target = result.getEntity();
        Entity owner = getOwner();
        if (owner != null && (target == owner || owner.hasPassenger(target))) return;
        float dmg = 4.0f;
        target.hurt(level().damageSources().mobProjectile(this, owner instanceof LivingEntity le ? le : null), dmg);
        Vec3 kb = getDeltaMovement().scale(0.3).add(0, 0.2, 0);
        target.setDeltaMovement(kb);
        if (target instanceof LivingEntity) {
            ((LivingEntity) target).hurtMarked = true;
        }
        playSound(SoundEvents.TRIDENT_HIT, 1, 1);
        if (target instanceof LivingEntity lt) {
            spawnAtLocation(getPickupItem(), 0.1f);
        }
        discard();
    }

    @Override
    protected void onHitBlock(net.minecraft.world.phys.BlockHitResult result) {
        super.onHitBlock(result);
        if (!level().isClientSide) {
            ((ServerLevel) level()).sendParticles(ParticleTypes.CRIT, getX(), getY(), getZ(), 5, 0.1, 0.1, 0.1, 0.05);
        }
    }
}
