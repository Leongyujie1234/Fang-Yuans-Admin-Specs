package com.adminspec.moves.dio;

import com.adminspec.entity.TheWorldStandEntity;
import com.adminspec.spec.MoveContext;
import com.adminspec.spec.SpecMove;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;
import java.util.UUID;

public class DioBarrageMove extends SpecMove {
    public static final String ID = "dio_barrage";

    public DioBarrageMove() {
        super(ID, Component.literal("Barrage"), Component.literal("The World unleashes a rapid barrage. Holdable. 14s cooldown."));
    }

    @Override public void activate(MoveContext ctx) {
        Player player = ctx.player();
        if (player.level().isClientSide) return;
        if (!(player instanceof ServerPlayer sp)) return;
        DioStandState.ensureStand(sp);
        int cd = DioStandState.BARRAGE_CD.getOrDefault(sp.getUUID(), 0);
        if (cd > 0) { sp.sendSystemMessage(Component.literal("§e[Barrage] §7" + String.format("%.1f", cd/20f) + "s")); return; }
        if (DioStandState.BARRAGE_TICKS.getOrDefault(sp.getUUID(), 0) > 0) return;
        TheWorldStandEntity stand = DioStandState.getStand(sp);
        if (stand != null) stand.playAnimation("animation.theworld.barrage");
        DioStandState.BARRAGE_TICKS.put(sp.getUUID(), DioStandState.BARRAGE_DURATION);
        DioStandState.BARRAGE_CD.put(sp.getUUID(), DioStandState.BARRAGE_COOLDOWN);
        sp.sendSystemMessage(Component.literal("§e§lMUDA MUDA MUDA!"));
    }

    @Override public void tick(Player player) {
        if (player.level().isClientSide) return;
        if (!(player instanceof ServerPlayer sp)) return;
        UUID u = sp.getUUID();
        int cd = DioStandState.BARRAGE_CD.getOrDefault(u, 0);
        if (cd > 0) DioStandState.BARRAGE_CD.put(u, cd - 1);
        int t = DioStandState.BARRAGE_TICKS.getOrDefault(u, 0);
        if (t <= 0) return;

        // Fire every INTERVAL ticks
        if (t % DioStandState.BARRAGE_INTERVAL == 0) {
            ServerLevel sl = (ServerLevel) sp.level();
            Vec3 look = sp.getLookAngle();
            Vec3 eye = sp.getEyePosition();

            // Guaranteed marker particles at player's position (same pattern as dragon breath)
            sl.sendParticles(ParticleTypes.EXPLOSION, eye.x, eye.y, eye.z, 1, 0.3, 0.3, 0.3, 0);
            sl.sendParticles(ParticleTypes.FLASH, eye.x, eye.y, eye.z, 1, 0, 0, 0, 0);

            AABB box = sp.getBoundingBox().expandTowards(look.scale(DioStandState.BARRAGE_REACH)).inflate(1.5);
            List<LivingEntity> hits = sl.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && !e.equals(sp));
            for (LivingEntity v : hits) {
                Vec3 toV = v.position().subtract(eye);
                double proj = toV.dot(look);
                if (proj < 0 || proj > DioStandState.BARRAGE_REACH) continue;
                if (v.position().distanceTo(eye.add(look.scale(proj))) > 2.0) continue;
                v.hurt(sl.damageSources().playerAttack(sp), DioStandState.BARRAGE_DAMAGE);
                v.setDeltaMovement(look.scale(DioStandState.BARRAGE_KNOCKBACK).add(0, 0.1, 0));
                v.hurtMarked = true;

                Vec3 hitPos = v.position().add(0.0, v.getBbHeight() * 0.5, 0.0);

                sl.sendParticles(ParticleTypes.ENCHANTED_HIT, hitPos.x, hitPos.y, hitPos.z, 3, 0.25, 0.25, 0.25, 0.03);
                sl.sendParticles(ParticleTypes.CRIT, hitPos.x, hitPos.y, hitPos.z, 3, 0.25, 0.25, 0.25, 0.05);
                sl.sendParticles(new DustParticleOptions(new Vector3f(1.0f, 0.84f, 0.0f), 1.5f), hitPos.x, hitPos.y, hitPos.z, 5, 0.3, 0.3, 0.3, 0.0);
            }
        }
        t--;
        if (t <= 0) DioStandState.BARRAGE_TICKS.remove(u);
        else DioStandState.BARRAGE_TICKS.put(u, t);
    }
}
