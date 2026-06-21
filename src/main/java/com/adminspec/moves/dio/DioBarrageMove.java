package com.adminspec.moves.dio;

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

public class DioBarrageMove extends SpecMove {
    public static final String ID = "dio_barrage";

    public DioBarrageMove() {
        super(ID,
            Component.literal("Barrage"),
            Component.literal("The World unleashes a rapid barrage of punches. Stand auto-summons."));
    }

    @Override
    public void activate(MoveContext ctx) {
        Player player = ctx.player();
        if (player.level().isClientSide) return;
        if (!ctx.pressed()) return;
        if (!(player instanceof ServerPlayer sp)) return;
        DioStandState.ensureStand(sp);
        if (DioStandState.BARRAGE_TICKS.getOrDefault(sp.getUUID(), 0) > 0) return;
        DioStandState.BARRAGE_TICKS.put(sp.getUUID(), DioStandState.BARRAGE_DURATION);
        sp.sendSystemMessage(Component.literal("§e§lMUDA MUDA MUDA!"));
    }

    @Override
    public void tick(Player player) {
        if (player.level().isClientSide) return;
        if (!(player instanceof ServerPlayer sp)) return;
        UUID uuid = sp.getUUID();
        int ticks = DioStandState.BARRAGE_TICKS.getOrDefault(uuid, 0);
        if (ticks <= 0) return;

        ServerLevel sl = (ServerLevel) sp.level();
        Vec3 look = sp.getLookAngle();
        Vec3 eye = sp.getEyePosition();
        int reach = DioStandState.BARRAGE_REACH;

        AABB box = sp.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.5);
        List<LivingEntity> victims = sl.getEntitiesOfClass(LivingEntity.class, box,
            e -> e.isAlive() && !e.equals(sp));
        for (LivingEntity v : victims) {
            Vec3 toV = v.position().subtract(eye);
            double proj = toV.dot(look);
            if (proj < 0 || proj > reach) continue;
            Vec3 closest = eye.add(look.scale(proj));
            if (v.position().distanceTo(closest) > 2.0) continue;
            v.hurt(sl.damageSources().playerAttack(sp), 1.5f);
            v.setDeltaMovement(look.scale(0.4).add(0, 0.15, 0));
            v.hurtMarked = true;
        }

        for (int i = 0; i < 6; i++) {
            double spread = 0.5;
            Vec3 pos = eye.add(look.scale(0.5 + Math.random() * 2.0));
            sl.sendParticles(ParticleTypes.CRIT,
                pos.x + (Math.random() - 0.5) * spread,
                pos.y + (Math.random() - 0.5) * spread + 0.5,
                pos.z + (Math.random() - 0.5) * spread,
                1, 0, 0, 0, 0.05);
            sl.sendParticles(ParticleTypes.ENCHANTED_HIT,
                pos.x + (Math.random() - 0.5) * spread,
                pos.y + (Math.random() - 0.5) * spread + 0.5,
                pos.z + (Math.random() - 0.5) * spread,
                1, 0, 0, 0, 0.02);
        }
        sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
            eye.x + look.x * 1.5, eye.y, eye.z + look.z * 1.5, 2, 0.3, 0.3, 0.3, 0);

        ticks--;
        if (ticks <= 0) {
            DioStandState.BARRAGE_TICKS.remove(uuid);
        } else {
            DioStandState.BARRAGE_TICKS.put(uuid, ticks);
        }
    }
}
