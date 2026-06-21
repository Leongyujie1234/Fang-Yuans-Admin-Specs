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

public class DioRoadRollerMove extends SpecMove {
    public static final String ID = "dio_road_roller";
    private static final int COOLDOWN = 120;

    public DioRoadRollerMove() {
        super(ID,
            Component.literal("Road Roller"),
            Component.literal("Leap up and slam down, dealing massive AOE damage. Stand must be active."));
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
        if (DioState.ROAD_ROLLER_TICKS.getOrDefault(sp.getUUID(), 0) > 0) return;

        // Launch upward
        sp.setDeltaMovement(0, 1.8, 0);
        sp.hurtMarked = true;
        DioState.ROAD_ROLLER_TICKS.put(sp.getUUID(), DioState.ROAD_ROLLER_DURATION);
        sp.sendSystemMessage(Component.literal("§e§lROAD ROLLER DA!"));
    }

    @Override
    public void tick(Player player) {
        if (player.level().isClientSide) return;
        if (!(player instanceof ServerPlayer sp)) return;

        UUID uuid = sp.getUUID();
        int ticks = DioState.ROAD_ROLLER_TICKS.getOrDefault(uuid, 0);
        if (ticks <= 0) return;

        ServerLevel sl = (ServerLevel) sp.level();

        // Descending phase (second half of duration)
        if (ticks < DioState.ROAD_ROLLER_DURATION / 2) {
            // Slam down
            sp.setDeltaMovement(0, -1.5, 0);
            sp.hurtMarked = true;

            // Falling particles
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, sp.getX(), sp.getY() + 1, sp.getZ(), 3, 0.5, 0.3, 0.5, 0.05);
        } else {
            // Ascending phase
            double drift = (double) ticks / DioState.ROAD_ROLLER_DURATION;
            sp.setDeltaMovement(0, 1.8 * drift, 0);
            sp.hurtMarked = true;
            sl.sendParticles(ParticleTypes.CLOUD, sp.getX(), sp.getY(), sp.getZ(), 2, 0.3, 0, 0.3, 0.02);
        }

        // Hit ground check
        if (sp.onGround()) {
            // AOE damage and explosion
            AABB area = sp.getBoundingBox().inflate(DioState.ROAD_ROLLER_RADIUS);
            List<LivingEntity> victims = sl.getEntitiesOfClass(LivingEntity.class, area,
                e -> e.isAlive() && !e.equals(sp));
            for (LivingEntity v : victims) {
                double dist = v.distanceTo(sp);
                if (dist > DioState.ROAD_ROLLER_RADIUS) continue;
                float dmg = (float) (DioState.ROAD_ROLLER_DAMAGE * (1.0 - dist / DioState.ROAD_ROLLER_RADIUS));
                v.hurt(sl.damageSources().playerAttack(sp), dmg);
                Vec3 kb = v.position().subtract(sp.position()).normalize().scale(1.5).add(0, 0.5, 0);
                v.setDeltaMovement(kb);
                v.hurtMarked = true;
            }

            // Explosion particles
            Vec3 pos = sp.position();
            sl.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y + 0.5, pos.z, 10, 3, 1, 3, 0.3);
            sl.sendParticles(ParticleTypes.LAVA, pos.x, pos.y + 0.5, pos.z, 20, 2, 0.5, 2, 0);
            sl.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, pos.x, pos.y + 0.5, pos.z, 8, 2, 0.5, 2, 0.1);
            sl.sendParticles(ParticleTypes.FLASH, pos.x, pos.y + 1, pos.z, 2, 1, 1, 1, 0);

            sp.sendSystemMessage(Component.literal("§e§lROAD ROLLER!"));
            ticks = 0;
        }

        ticks--;
        if (ticks <= 0) {
            DioState.ROAD_ROLLER_TICKS.remove(uuid);
        } else {
            DioState.ROAD_ROLLER_TICKS.put(uuid, ticks);
        }
    }
}
