/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Holder
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.core.particles.ParticleTypes
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.syncher.EntityDataAccessor
 *  net.minecraft.network.syncher.EntityDataSerializer
 *  net.minecraft.network.syncher.EntityDataSerializers
 *  net.minecraft.network.syncher.SynchedEntityData
 *  net.minecraft.network.syncher.SynchedEntityData$Builder
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.world.damagesource.DamageSource
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.Mob
 *  net.minecraft.world.entity.PathfinderMob
 *  net.minecraft.world.entity.ai.attributes.AttributeSupplier$Builder
 *  net.minecraft.world.entity.ai.attributes.Attributes
 *  net.minecraft.world.entity.ai.goal.FloatGoal
 *  net.minecraft.world.entity.ai.goal.Goal
 *  net.minecraft.world.entity.ai.goal.MeleeAttackGoal
 *  net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal
 *  net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal
 *  net.minecraft.world.entity.monster.Zombie
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.Level$ExplosionInteraction
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 */
package com.adminspec.entity;

import com.adminspec.capability.BlockRecoveryManager;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class YamaChildEntity
extends Zombie {
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(YamaChildEntity.class, (EntityDataSerializer)EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> FUSE_LIT = SynchedEntityData.defineId(YamaChildEntity.class, (EntityDataSerializer)EntityDataSerializers.BOOLEAN);
    private static final int FUSE_TICKS = 20;
    private static final float TNT_DAMAGE = 48.0f;
    private static final float TNT_RADIUS = 4.0f;
    private static final int MAX_LIFETIME = 600;
    private int fuseTimer = -1;
    private int lifetimeTicks = 0;

    public YamaChildEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        this.setBaby(true);
        this.setPersistenceRequired();
    }

    public void setOwner(Player owner) {
        this.entityData.set(OWNER_ID, owner == null ? -1 : owner.getId());
    }

    public Player getOwnerPlayer() {
        Player p;
        int id = (Integer)this.entityData.get(OWNER_ID);
        if (id < 0 || this.level().isClientSide) {
            return null;
        }
        Entity e = this.level().getEntity(id);
        return e instanceof Player ? (p = (Player)e) : null;
    }

    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OWNER_ID, -1);
        builder.define(FUSE_LIT, false);
    }

    public static AttributeSupplier.Builder createYamaAttributes() {
        return Zombie.createAttributes().add(Attributes.MOVEMENT_SPEED, 0.35).add(Attributes.MAX_HEALTH, 6.0).add(Attributes.ATTACK_DAMAGE, 1.0);
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(0, (Goal)new FloatGoal((Mob)this));
        this.goalSelector.addGoal(1, (Goal)new MeleeAttackGoal((PathfinderMob)this, 1.5, true));
        this.goalSelector.addGoal(5, (Goal)new WaterAvoidingRandomStrollGoal((PathfinderMob)this, 1.0));
        this.targetSelector.addGoal(1, (Goal)new NearestAttackableTargetGoal((Mob)this, LivingEntity.class, 10, true, false, (java.util.function.Predicate<LivingEntity>)(e -> !(e instanceof YamaChildEntity) && !e.equals(this.getOwnerPlayer()) && e.isAlive())));
    }

    protected boolean isSunBurnTick() {
        return false;
    }

    public void tick() {
        super.tick();
        ++this.lifetimeTicks;
        if (this.lifetimeTicks > 600) {
            this.discard();
            return;
        }
        if (this.fuseTimer > 0) {
            --this.fuseTimer;
            this.entityData.set(FUSE_LIT, true);
            if (this.level().isClientSide && this.random.nextFloat() < 0.4f) {
                this.level().addParticle((ParticleOptions)ParticleTypes.SMOKE, this.getX() + (this.random.nextDouble() - 0.5) * 0.4, this.getY() + this.random.nextDouble() * 1.0, this.getZ() + (this.random.nextDouble() - 0.5) * 0.4, 0.0, 0.1, 0.0);
            }
            if (this.fuseTimer == 0) {
                this.detonate();
            }
        } else {
            LivingEntity target = this.getTarget();
            if (target != null && this.distanceToSqr((Entity)target) < 6.25) {
                this.fuseTimer = 20;
            }
        }
    }

    private void detonate() {
        if (this.level().isClientSide) {
            return;
        }
        ServerLevel sl = (ServerLevel)this.level();
        Player owner = this.getOwnerPlayer();
        DamageSource src = owner != null ? this.level().damageSources().playerAttack(owner) : this.level().damageSources().mobAttack((LivingEntity)this);
        AABB blastBox = AABB.ofSize((Vec3)this.position(), (double)8.0, (double)8.0, (double)8.0);
        ArrayList<BlockPos> positions = new ArrayList<BlockPos>();
        int minX = (int)Math.floor(blastBox.minX);
        int maxX = (int)Math.floor(blastBox.maxX);
        int minY = (int)Math.floor(blastBox.minY);
        int maxY = (int)Math.floor(blastBox.maxY);
        int minZ = (int)Math.floor(blastBox.minZ);
        int maxZ = (int)Math.floor(blastBox.maxZ);
        for (int x = minX; x <= maxX; ++x) {
            for (int y = minY; y <= maxY; ++y) {
                for (int z = minZ; z <= maxZ; ++z) {
                    positions.add(new BlockPos(x, y, z));
                }
            }
        }
        BlockRecoveryManager mgr = BlockRecoveryManager.get(sl);
        if (mgr != null) {
            mgr.snapshotAndSchedule(sl, positions, sl.getGameTime());
        }
        AABB box = AABB.ofSize((Vec3)this.position(), (double)8.0, (double)8.0, (double)8.0);
        List<LivingEntity> victims = sl.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && !e.equals(this) && !e.equals(owner));
        for (LivingEntity v : victims) {
            double dist = v.position().distanceTo(this.position());
            double falloff = Math.max(0.0, 1.0 - dist / 4.0);
            float dmg = (float)(48.0 * falloff);
            v.hurt(src, dmg);
            Vec3 kb = v.position().subtract(this.position()).normalize().scale(falloff * 1.5);
            v.push(kb.x, kb.y + 0.3, kb.z);
        }
        this.level().explode((Entity)this, this.getX(), this.getY() + 0.5, this.getZ(), 4.0f, Level.ExplosionInteraction.TNT);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), (Holder)SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.5f, 0.8f);
        sl.sendParticles((ParticleOptions)ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 0.5, this.getZ(), 2, 0.0, 0.0, 0.0, 0.0);
        this.discard();
    }

    public boolean isBaby() {
        return true;
    }

    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Fuse", this.fuseTimer);
        tag.putInt("Life", this.lifetimeTicks);
    }

    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.fuseTimer = tag.getInt("Fuse");
        this.lifetimeTicks = tag.getInt("Life");
    }
}

