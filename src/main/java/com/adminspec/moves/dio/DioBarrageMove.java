package com.adminspec.moves.dio;

import com.adminspec.spec.MoveContext;
import com.adminspec.spec.SpecMove;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class DioBarrageMove extends SpecMove {
    public static final String ID = "dio_barrage";
    private static final int COOLDOWN = 60;

    public DioBarrageMove() {
        super(ID,
            Component.literal("Muda Barrage"),
            Component.literal("Rapidly punch nearby enemies. Stand must be active. 3s cooldown."));
    }

    @Override
    public void activate(MoveContext ctx) {
        Player player = ctx.player();
        if (player.level().isClientSide) return;
        if (!ctx.pressed()) return;
        if (!(player instanceof ServerPlayer sp)) return;
        if (!DioState.STAND_ACTIVE.contains(sp.getUUID())) {
            sp.sendSystemMessage(Component.literal("§e[The World] §cSummon your stand first!"));
            return;
        }
        if (DioState.BARRAGE_TICKS.getOrDefault(sp.getUUID(), 0) > 0) return;

        DioState.BARRAGE_TICKS.put(sp.getUUID(), DioState.BARRAGE_DURATION);
        sp.sendSystemMessage(Component.literal("§e§lMUDA MUDA MUDA!"));
    }

    @Override
    public void tick(Player player) {
        if (player.level().isClientSide) return;
        if (!(player instanceof ServerPlayer sp)) return;

        UUID uuid = sp.getUUID();
        int ticks = DioState.BARRAGE_TICKS.getOrDefault(uuid, 0);
        if (ticks <= 0) return;

        ServerLevel sl = (ServerLevel) sp.level();
        Vec3 look = sp.getLookAngle();
        Vec3 eye = sp.getEyePosition();
        double reach = 4.0;

        // Damage entities in cone
        AABB box = sp.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.5);
        List<LivingEntity> victims = sl.getEntitiesOfClass(LivingEntity.class, box,
            e -> e.isAlive() && !e.equals(sp));
        for (LivingEntity v : victims) {
            Vec3 toV = v.position().subtract(eye);
            double proj = toV.dot(look);
            if (proj < 0 || proj > reach) continue;
            Vec3 closest = eye.add(look.scale(proj));
            if (v.position().distanceTo(closest) > 2.0) continue;
            v.hurt(sl.damageSources().playerAttack(sp), 2.0f);
            // Knockback
            v.setDeltaMovement(look.scale(0.5).add(0, 0.2, 0));
            v.hurtMarked = true;
        }

        // Barrage particles
        for (int i = 0; i < 8; i++) {
            double spread = 0.6;
            Vec3 pos = eye.add(look.scale(0.5 + Math.random() * 2.0));
            sl.sendParticles(ParticleTypes.CRIT,
                pos.x + (Math.random() - 0.5) * spread,
                pos.y + (Math.random() - 0.5) * spread,
                pos.z + (Math.random() - 0.5) * spread,
                1, 0, 0, 0, 0.05);
            sl.sendParticles(ParticleTypes.ENCHANTED_HIT,
                pos.x + (Math.random() - 0.5) * spread,
                pos.y + (Math.random() - 0.5) * spread,
                pos.z + (Math.random() - 0.5) * spread,
                1, 0, 0, 0, 0.02);
        }
        // Sweep attack particles
        for (int i = 0; i < 3; i++) {
            Vec3 pos = eye.add(look.scale(1.0 + Math.random() * 2.0));
            sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
                pos.x, pos.y, pos.z,
                1, 0.2, 0.2, 0.2, 0);
        }

        ticks--;
        if (ticks <= 0) {
            DioState.BARRAGE_TICKS.remove(uuid);
        } else {
            DioState.BARRAGE_TICKS.put(uuid, ticks);
        }
    }
}
