package com.adminspec.entity;

import mod.azure.azurelib.common.api.common.animatable.GeoEntity;
import mod.azure.azurelib.core.animatable.instance.AnimatableInstanceCache;
import mod.azure.azurelib.core.animation.AnimatableManager;
import mod.azure.azurelib.core.animation.AnimationController;
import mod.azure.azurelib.core.animation.RawAnimation;
import mod.azure.azurelib.core.object.PlayState;
import mod.azure.azurelib.common.internal.common.util.AzureLibUtil;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class TheWorldStandEntity extends Mob implements GeoEntity {
    private UUID ownerUUID;
    private static final double FOLLOW_DIST = 1.2;
    private static final double FOLLOW_HEIGHT = 0.3;
    private static final double SIDE_OFFSET = 0.6;
    private final AnimatableInstanceCache cache = AzureLibUtil.createInstanceCache(this);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.theworld.idle");
    private static final RawAnimation SUMMON = RawAnimation.begin().thenPlayAndHold("summon");
    private boolean summonPlayed = false;

    public TheWorldStandEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        setNoGravity(true);
        setPersistenceRequired();
        noCulling = true;
    }

    public void setOwner(Player owner) {
        this.ownerUUID = owner.getUUID();
    }

    public Player getOwner() {
        if (ownerUUID == null) return null;
        if (level().isClientSide) return null;
        var e = ((net.minecraft.server.level.ServerLevel) level()).getEntity(ownerUUID);
        return e instanceof Player ? (Player) e : null;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 100.0)
            .add(Attributes.ARMOR, 10.0);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, event -> {
            String animName = getPersistentData().getString("animCmd");
            if (!animName.isEmpty()) {
                event.getController().setAnimation(RawAnimation.begin().thenLoop(animName));
                getPersistentData().putString("animCmd", "");
                summonPlayed = true;
            } else if (!summonPlayed) {
                event.getController().setAnimation(SUMMON);
                summonPlayed = true;
            } else {
                event.getController().setAnimation(IDLE);
            }
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    public void playAnimation(String animName) {
        getPersistentData().putString("animCmd", animName);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        Player owner = getOwner();
        if (owner == null || !owner.isAlive()) {
            if (tickCount > 20) discard();
            return;
        }
        Vec3 look = owner.getLookAngle();
        Vec3 right = new Vec3(-look.z, 0, look.x);
        Vec3 target = owner.position()
            .add(0, FOLLOW_HEIGHT, 0)
            .subtract(look.scale(FOLLOW_DIST))
            .add(right.scale(SIDE_OFFSET));

        Vec3 current = position();
        Vec3 delta = target.subtract(current);
        double dist = delta.length();
        if (dist > 0.1) {
            double speed = Math.min(0.5, dist * 0.3);
            setDeltaMovement(delta.normalize().scale(speed));
        } else {
            setDeltaMovement(Vec3.ZERO);
        }
        setYRot(owner.getYRot());
        yHeadRot = owner.getYHeadRot();
        setXRot(owner.getXRot());

        var data = com.adminspec.capability.PlayerSpecCapability.get(owner);
        if (!"dio_the_world".equals(data.getSpecId())) {
            discard();
        }
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void pushEntities() {}
}
