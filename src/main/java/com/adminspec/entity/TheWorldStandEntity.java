package com.adminspec.entity;

import com.adminspec.moves.dio.DioStandState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class TheWorldStandEntity extends Mob {
    private UUID ownerUUID;
    private static final double FOLLOW_DIST = 1.2;
    private static final double FOLLOW_HEIGHT = 0.3;
    private static final double SIDE_OFFSET = 0.6;

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

        // Check if user still has DIO spec
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
