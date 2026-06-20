/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.HolderLookup$Provider
 *  net.minecraft.core.particles.DustParticleOptions
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.syncher.EntityDataAccessor
 *  net.minecraft.network.syncher.EntityDataSerializer
 *  net.minecraft.network.syncher.EntityDataSerializers
 *  net.minecraft.network.syncher.SynchedEntityData
 *  net.minecraft.network.syncher.SynchedEntityData$Builder
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.MoverType
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Vector3f
 */
package com.adminspec.entity;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class FlyingHeadEntity
extends Entity {
    private static final EntityDataAccessor<ItemStack> HEAD_ITEM = SynchedEntityData.defineId(FlyingHeadEntity.class, (EntityDataSerializer)EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Float> NECK_X = SynchedEntityData.defineId(FlyingHeadEntity.class, (EntityDataSerializer)EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> NECK_Y = SynchedEntityData.defineId(FlyingHeadEntity.class, (EntityDataSerializer)EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> NECK_Z = SynchedEntityData.defineId(FlyingHeadEntity.class, (EntityDataSerializer)EntityDataSerializers.FLOAT);
    private static final int MAX_LIFETIME = 200;
    private static final int BLOOD_DURATION = 40;
    private int lifetimeTicks = 200;
    private int bloodTimer = 40;
    private static final DustParticleOptions BLOOD_PARTICLE = new DustParticleOptions(new Vector3f(0.75f, 0.0f, 0.0f), 1.5f);

    public FlyingHeadEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = false;
    }

    public void init(Vec3 neckPos, ItemStack headItem, Vec3 dashDir) {
        this.setPos(neckPos.x, neckPos.y, neckPos.z);
        this.entityData.set(HEAD_ITEM, headItem);
        this.entityData.set(NECK_X, (float)neckPos.x);
        this.entityData.set(NECK_Y, (float)neckPos.y);
        this.entityData.set(NECK_Z, (float)neckPos.z);
        this.setDeltaMovement(dashDir.scale(0.25).add(0.0, 0.55, 0.0));
    }

    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(HEAD_ITEM, ItemStack.EMPTY);
        builder.define(NECK_X, 0.0f);
        builder.define(NECK_Y, 0.0f);
        builder.define(NECK_Z, 0.0f);
    }

    public ItemStack getHeadItem() {
        return (ItemStack)this.entityData.get(HEAD_ITEM);
    }

    public Vec3 getNeckPos() {
        return new Vec3((double)((Float)this.entityData.get(NECK_X)).floatValue(), (double)((Float)this.entityData.get(NECK_Y)).floatValue(), (double)((Float)this.entityData.get(NECK_Z)).floatValue());
    }

    public void tick() {
        super.tick();
        if (!this.isNoGravity()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.04, 0.0));
        }
        this.setDeltaMovement(this.getDeltaMovement().multiply(0.98, 0.98, 0.98));
        this.move(MoverType.SELF, this.getDeltaMovement());
        if (this.onGround()) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.0, 0.0, 0.0));
        }
        if (this.bloodTimer > 0) {
            --this.bloodTimer;
            this.spawnBlood();
        }
        --this.lifetimeTicks;
        if (this.lifetimeTicks <= 0) {
            this.discard();
        }
    }

    private void spawnBlood() {
        Vec3 neck = this.getNeckPos();
        if (this.level().isClientSide) {
            for (int i = 0; i < 4; ++i) {
                double dx = (this.random.nextDouble() - 0.5) * 0.3;
                double dy = this.random.nextDouble() * 0.4 + 0.1;
                double dz = (this.random.nextDouble() - 0.5) * 0.3;
                this.level().addParticle((ParticleOptions)BLOOD_PARTICLE, neck.x + (this.random.nextDouble() - 0.5) * 0.2, neck.y + (this.random.nextDouble() - 0.5) * 0.2, neck.z + (this.random.nextDouble() - 0.5) * 0.2, dx, dy, dz);
            }
        } else {
            Level level = this.level();
            if (level instanceof ServerLevel) {
                ServerLevel sl = (ServerLevel)level;
                sl.sendParticles((ParticleOptions)BLOOD_PARTICLE, neck.x, neck.y, neck.z, 3, 0.15, 0.2, 0.15, 0.1);
            }
        }
    }

    public boolean isPickable() {
        return false;
    }

    public boolean isPushable() {
        return false;
    }

    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("HeadItem")) {
            this.entityData.set(HEAD_ITEM, ItemStack.parseOptional(this.level().registryAccess(), tag.getCompound("HeadItem")));
        }
        if (tag.contains("NeckX")) {
            this.entityData.set(NECK_X, tag.getFloat("NeckX"));
        }
        if (tag.contains("NeckY")) {
            this.entityData.set(NECK_Y, tag.getFloat("NeckY"));
        }
        if (tag.contains("NeckZ")) {
            this.entityData.set(NECK_Z, tag.getFloat("NeckZ"));
        }
        this.lifetimeTicks = tag.getInt("Life");
        this.bloodTimer = tag.getInt("Blood");
    }

    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.put("HeadItem", this.getHeadItem().saveOptional((HolderLookup.Provider)this.level().registryAccess()));
        tag.putFloat("NeckX", ((Float)this.entityData.get(NECK_X)).floatValue());
        tag.putFloat("NeckY", ((Float)this.entityData.get(NECK_Y)).floatValue());
        tag.putFloat("NeckZ", ((Float)this.entityData.get(NECK_Z)).floatValue());
        tag.putInt("Life", this.lifetimeTicks);
        tag.putInt("Blood", this.bloodTimer);
    }
}

