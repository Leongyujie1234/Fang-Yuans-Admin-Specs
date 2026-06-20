package com.adminspec.entity;

import com.adminspec.capability.BlockRecoveryManager;
import java.util.ArrayList;
import java.util.List;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
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
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class YamaChildEntity extends Zombie {
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(YamaChildEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> FUSE_LIT = SynchedEntityData.defineId(YamaChildEntity.class, EntityDataSerializers.BOOLEAN);
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
        this.setNoGravity(true);
    }

    public void setOwner(Player owner) {
        this.entityData.set(OWNER_ID, owner == null ? -1 : owner.getId());
    }

    public Player getOwnerPlayer() {
        int id = this.entityData.get(OWNER_ID);
        if (id < 0 || this.level().isClientSide) return null;
        Entity e = this.level().getEntity(id);
        return e instanceof Player ? (Player) e : null;
    }

    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OWNER_ID, -1);
        builder.define(FUSE_LIT, false);
    }

    public static AttributeSupplier.Builder createYamaAttributes() {
        return Zombie.createAttributes()
            .add(Attributes.MOVEMENT_SPEED, 0.35)
            .add(Attributes.MAX_HEALTH, 10.0)
            .add(Attributes.ARMOR, 4.0)
            .add(Attributes.ATTACK_DAMAGE, 1.0)
            .add(Attributes.FLYING_SPEED, 0.6);
    }

    public boolean isFuseLit() {
        return this.entityData.get(FUSE_LIT);
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new FlyingHomingGoal(this, 0.6));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
            e -> !(e instanceof YamaChildEntity) && !e.equals(this.getOwnerPlayer()) && e.isAlive()));
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    protected boolean isSunBurnTick() {
        return false;
    }

    public void tick() {
        super.tick();
        ++this.lifetimeTicks;
        if (this.lifetimeTicks > MAX_LIFETIME) {
            this.discard();
            return;
        }

        // Flying homing toward target
        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive()) {
            Vec3 aim = target.getBoundingBox().getCenter().subtract(this.position());
            double dist = aim.length();
            if (dist > 0.1) {
                double speed = 0.3;
                Vec3 push = aim.normalize().scale(speed);
                Vec3 vel = this.getDeltaMovement();
                this.setDeltaMovement(vel.scale(0.7).add(push.scale(0.3)));
                if (dist > 3.0) {
                    this.setDeltaMovement(this.getDeltaMovement().add(0, 0.02, 0));
                }
            }
            this.lookAt(target, 30, 30);
        } else {
            // Hover in place
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.8, 0.8, 0.8));
            this.setDeltaMovement(this.getDeltaMovement().add(0, Math.sin(this.tickCount * 0.05) * 0.005, 0));
        }

        // Fuse logic
        if (this.fuseTimer > 0) {
            --this.fuseTimer;
            this.entityData.set(FUSE_LIT, true);
            if (this.level().isClientSide && this.random.nextFloat() < 0.4f) {
                this.level().addParticle(ParticleTypes.SMOKE,
                    this.getX() + (this.random.nextDouble() - 0.5) * 0.4,
                    this.getY() + this.random.nextDouble() * 1.0,
                    this.getZ() + (this.random.nextDouble() - 0.5) * 0.4,
                    0.0, 0.1, 0.0);
            }
            if (this.fuseTimer == 0) {
                this.detonate();
            }
        } else if (target != null && this.distanceToSqr(target) < 6.25) {
            this.fuseTimer = FUSE_TICKS;
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
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.5f, 0.8f);
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

    static class FlyingHomingGoal extends Goal {
        private final YamaChildEntity entity;
        private final double speed;

        public FlyingHomingGoal(YamaChildEntity entity, double speed) {
            this.entity = entity;
            this.speed = speed;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return this.entity.getTarget() != null && this.entity.getTarget().isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse();
        }

        @Override
        public void tick() {
            LivingEntity target = this.entity.getTarget();
            if (target == null) return;
            this.entity.getNavigation().stop();
            Vec3 aim = target.getBoundingBox().getCenter().subtract(this.entity.position());
            double dist = aim.length();
            if (dist > 0.5) {
                Vec3 move = aim.normalize().scale(this.speed);
                this.entity.setDeltaMovement(this.entity.getDeltaMovement().scale(0.8).add(move.scale(0.2)));
            }
            this.entity.lookAt(target, 30, 30);
        }
    }
}

