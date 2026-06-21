package com.adminspec.moves.dio;

import com.adminspec.entity.TheWorldStandEntity;
import com.adminspec.spec.MoveContext;
import com.adminspec.spec.SpecMove;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

public class DioChargeMove extends SpecMove {
    public static final String ID = "dio_charge";

    public DioChargeMove() {
        super(ID,
            Component.literal("Charge"),
            Component.literal("The World flies forward, damaging enemies in its path."));
    }

    @Override
    public void activate(MoveContext ctx) {
        Player player = ctx.player();
        if (player.level().isClientSide) return;
        if (!ctx.pressed()) return;
        if (!(player instanceof ServerPlayer sp)) return;
        DioStandState.ensureStand(sp);
        if (DioStandState.CHARGE_TICKS.getOrDefault(sp.getUUID(), 0) > 0) return;
        int cd = DioStandState.CHARGE_CD.getOrDefault(sp.getUUID(), 0);
        if (cd > 0) {
            sp.sendSystemMessage(Component.literal("§e[Charge] §7Cooldown: §f" + String.format("%.1f", cd / 20.0) + "s"));
            return;
        }
        TheWorldStandEntity stand = DioStandState.getStand(sp);
        if (stand == null) return;
        stand.playAnimation("animation.theworld.charge");
        Vec3 dir = sp.getLookAngle();
        stand.getPersistentData().putDouble("chargeDirX", dir.x);
        stand.getPersistentData().putDouble("chargeDirY", dir.y);
        stand.getPersistentData().putDouble("chargeDirZ", dir.z);
        stand.getPersistentData().putDouble("chargeOriginX", stand.getX());
        stand.getPersistentData().putDouble("chargeOriginY", stand.getY());
        stand.getPersistentData().putDouble("chargeOriginZ", stand.getZ());
        DioStandState.CHARGE_TICKS.put(sp.getUUID(), DioStandState.CHARGE_DURATION);
        DioStandState.CHARGE_CD.put(sp.getUUID(), DioStandState.CHARGE_COOLDOWN);
        sp.sendSystemMessage(Component.literal("§e§lTHE WORLD! CHARGE!"));
    }

    @Override
    public void tick(Player player) {
        if (player.level().isClientSide) return;
        if (!(player instanceof ServerPlayer sp)) return;
        UUID uuid = sp.getUUID();
        int cd = DioStandState.CHARGE_CD.getOrDefault(uuid, 0);
        if (cd > 0) DioStandState.CHARGE_CD.put(uuid, cd - 1);
        int ticks = DioStandState.CHARGE_TICKS.getOrDefault(uuid, 0);
        if (ticks <= 0) return;

        TheWorldStandEntity stand = DioStandState.getStand(sp);
        if (stand == null) {
            DioStandState.CHARGE_TICKS.remove(uuid);
            return;
        }

        ServerLevel sl = (ServerLevel) sp.level();
        double dx = stand.getPersistentData().getDouble("chargeDirX");
        double dy = stand.getPersistentData().getDouble("chargeDirY");
        double dz = stand.getPersistentData().getDouble("chargeDirZ");
        Vec3 dir = new Vec3(dx, dy, dz).normalize();
        double ox = stand.getPersistentData().getDouble("chargeOriginX");
        double oy = stand.getPersistentData().getDouble("chargeOriginY");
        double oz = stand.getPersistentData().getDouble("chargeOriginZ");
        Vec3 origin = new Vec3(ox, oy, oz);
        double traveled = origin.distanceTo(stand.position());

        if (traveled > DioStandState.MAX_CHARGE_TRAVEL || stand.horizontalCollision || stand.verticalCollision) {
            ticks = 0;
        } else {
            double speed = 0.8;
            stand.setDeltaMovement(dir.scale(speed));
            stand.hurtMarked = true;

            AABB hitBox = stand.getBoundingBox().inflate(1.0);
            List<LivingEntity> victims = sl.getEntitiesOfClass(LivingEntity.class, hitBox,
                e -> e.isAlive() && !e.equals(sp));
            for (LivingEntity v : victims) {
                v.hurt(sl.damageSources().playerAttack(sp), 6.0f);
                Vec3 kb = v.position().subtract(stand.position()).normalize().scale(1.2).add(0, 0.4, 0);
                v.setDeltaMovement(kb);
                v.hurtMarked = true;
            }
        }

        ticks--;
        if (ticks <= 0) {
            DioStandState.CHARGE_TICKS.remove(uuid);
            stand.setDeltaMovement(Vec3.ZERO);
        } else {
            DioStandState.CHARGE_TICKS.put(uuid, ticks);
        }
    }
}
