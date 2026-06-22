package com.adminspec.moves.dio;

import com.adminspec.entity.TheWorldStandEntity;
import com.adminspec.spec.MoveContext;
import com.adminspec.spec.SpecMove;
import net.minecraft.core.particles.ParticleTypes;
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
        super(ID, Component.literal("Forward Charge"), Component.literal("The World detaches and lunges forward. 5s cooldown."));
    }

    @Override public void activate(MoveContext ctx) {
        Player player = ctx.player();
        if (player.level().isClientSide) return;
        if (!ctx.pressed()) return;
        if (!(player instanceof ServerPlayer sp)) return;
        DioStandState.ensureStand(sp);
        int cd = DioStandState.CHARGE_CD.getOrDefault(sp.getUUID(), 0);
        if (cd > 0) { sp.sendSystemMessage(Component.literal("§e[Charge] §7" + String.format("%.1f", cd/20f) + "s")); return; }
        if (DioStandState.CHARGE_TICKS.getOrDefault(sp.getUUID(), 0) > 0) return;
        TheWorldStandEntity stand = DioStandState.getStand(sp);
        if (stand == null) return;
        stand.playAnimation("animation.theworld.charge");
        Vec3 dir = sp.getLookAngle();
        stand.getPersistentData().putDouble("cdx", dir.x); stand.getPersistentData().putDouble("cdy", dir.y); stand.getPersistentData().putDouble("cdz", dir.z);
        stand.getPersistentData().putDouble("cox", stand.getX()); stand.getPersistentData().putDouble("coy", stand.getY()); stand.getPersistentData().putDouble("coz", stand.getZ());
        DioStandState.CHARGE_TICKS.put(sp.getUUID(), DioStandState.CHARGE_DURATION);
        DioStandState.CHARGE_CD.put(sp.getUUID(), DioStandState.CHARGE_COOLDOWN);
        // Marker particles at activation
        ServerLevel sl = (ServerLevel) sp.level();
        sl.sendParticles(ParticleTypes.EXPLOSION, sp.getX(), sp.getY() + 1, sp.getZ(), 2, 0.5, 0.5, 0.5, 0);
        sl.sendParticles(ParticleTypes.FLASH, sp.getX(), sp.getY() + 1, sp.getZ(), 1, 0, 0, 0, 0);
        sp.sendSystemMessage(Component.literal("§e§lTHE WORLD!"));
    }

    @Override public void tick(Player player) {
        if (player.level().isClientSide) return;
        if (!(player instanceof ServerPlayer sp)) return;
        UUID u = sp.getUUID();
        int cd = DioStandState.CHARGE_CD.getOrDefault(u, 0);
        if (cd > 0) DioStandState.CHARGE_CD.put(u, cd - 1);
        int t = DioStandState.CHARGE_TICKS.getOrDefault(u, 0);
        if (t <= 0) return;

        TheWorldStandEntity stand = DioStandState.getStand(sp);
        if (stand == null) { DioStandState.CHARGE_TICKS.remove(u); return; }

        // Windup phase (first CHARGE_WINDUP ticks): stand stays still
        if (t > DioStandState.CHARGE_DURATION - DioStandState.CHARGE_WINDUP) {
            // Lunge phase: stand flies forward
            double dx = stand.getPersistentData().getDouble("cdx"); double dy = stand.getPersistentData().getDouble("cdy"); double dz = stand.getPersistentData().getDouble("cdz");
            Vec3 dir = new Vec3(dx, dy, dz).normalize();
            double ox = stand.getPersistentData().getDouble("cox"); double oy = stand.getPersistentData().getDouble("coy"); double oz = stand.getPersistentData().getDouble("coz");
            Vec3 origin = new Vec3(ox, oy, oz);
            double traveled = origin.distanceTo(stand.position());

            if (traveled > DioStandState.CHARGE_RANGE || stand.horizontalCollision) {
                t = 0;
            } else {
                stand.setDeltaMovement(dir.scale(0.8)); stand.hurtMarked = true;
                ServerLevel sl = (ServerLevel) sp.level();

                Vec3 trailPos = stand.position().add(0.0, stand.getBbHeight() / 2.0, 0.0);
                sl.sendParticles(ParticleTypes.END_ROD, trailPos.x, trailPos.y, trailPos.z, 6, 0.1, 0.1, 0.1, 0.03);
                sl.sendParticles(ParticleTypes.CRIT, trailPos.x, trailPos.y, trailPos.z, 4, 0.15, 0.15, 0.15, 0.05);
                AABB box = stand.getBoundingBox().inflate(1.0);
                for (LivingEntity v : sl.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && !e.equals(sp))) {
                    v.hurt(sl.damageSources().playerAttack(sp), DioStandState.CHARGE_DAMAGE);
                    Vec3 kb = v.position().subtract(stand.position()).normalize().scale(DioStandState.CHARGE_KNOCKBACK * 2).add(0, 0.3, 0);
                    v.setDeltaMovement(kb); v.hurtMarked = true;

                    Vec3 impactPos = v.position().add(0.0, v.getBbHeight() * 0.5, 0.0);
                    sl.sendParticles(ParticleTypes.EXPLOSION, impactPos.x, impactPos.y, impactPos.z, 3, 0.8, 0.8, 0.8, 0.1);
                    sl.sendParticles(ParticleTypes.FLASH, impactPos.x, impactPos.y, impactPos.z, 1, 0.0, 0.0, 0.0, 0.0);
                }
            }
        }

        t--;
        if (t <= 0) { DioStandState.CHARGE_TICKS.remove(u); stand.setDeltaMovement(Vec3.ZERO); }
        else DioStandState.CHARGE_TICKS.put(u, t);
    }
}
