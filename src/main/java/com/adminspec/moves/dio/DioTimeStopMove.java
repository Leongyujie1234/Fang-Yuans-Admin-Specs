package com.adminspec.moves.dio;

import com.adminspec.spec.MoveContext;
import com.adminspec.spec.SpecMove;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class DioTimeStopMove extends SpecMove {
    public static final String ID = "dio_timestop";
    private static final int COOLDOWN = 400;

    public DioTimeStopMove() {
        super(ID,
            Component.literal("The World - Time Stop"),
            Component.literal("Freeze all entities in a 20-block radius for 3 seconds. 20s cooldown."));
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
        if (DioState.TIMESTOP_TICKS.getOrDefault(sp.getUUID(), 0) > 0) return;

        ServerLevel sl = (ServerLevel) sp.level();
        AABB area = sp.getBoundingBox().inflate(DioState.TIMESTOP_RADIUS);
        List<Entity> entities = sl.getEntities((Entity)null, area, (Predicate<? super Entity>)(e -> e.isAlive() && !e.equals(sp)));

        Map<UUID, Vec3> frozen = new HashMap<>();
        for (Entity e : entities) {
            frozen.put(e.getUUID(), e.position());
            if (e instanceof LivingEntity le) {
                le.setNoActionTime(DioState.TIMESTOP_DURATION + 20);
            }
        }
        DioState.FROZEN_ENTITIES.put(sp.getUUID(), frozen);
        DioState.TIMESTOP_TICKS.put(sp.getUUID(), DioState.TIMESTOP_DURATION);

        // Flash effect
        sl.sendParticles(ParticleTypes.FLASH, sp.getX(), sp.getY() + 1, sp.getZ(), 1, 0, 0, 0, 0);
        sl.sendParticles(ParticleTypes.EXPLOSION, sp.getX(), sp.getY() + 1, sp.getZ(), 5, 2, 2, 2, 0.1);

        sp.sendSystemMessage(Component.literal("§e§l[The World] §r§5ZA WARUDO! TOKI WO TOMARE!"));
    }

    @Override
    public void tick(Player player) {
        if (player.level().isClientSide) return;
        if (!(player instanceof ServerPlayer sp)) return;

        UUID uuid = sp.getUUID();
        int ticks = DioState.TIMESTOP_TICKS.getOrDefault(uuid, 0);
        if (ticks <= 0) return;

        ServerLevel sl = (ServerLevel) sp.level();
        Map<UUID, Vec3> frozen = DioState.FROZEN_ENTITIES.get(uuid);

        // Keep frozen entities in place
        if (frozen != null) {
            for (Map.Entry<UUID, Vec3> entry : frozen.entrySet()) {
                Entity e = sl.getEntity(entry.getKey());
                if (e != null && e.isAlive()) {
                    Vec3 pos = entry.getValue();
                    e.teleportTo(pos.x, pos.y, pos.z);
                    e.setDeltaMovement(Vec3.ZERO);
                    if (e instanceof LivingEntity le) {
                        le.setNoActionTime(DioState.TIMESTOP_DURATION + 20);
                    }
                }
            }
        }

        // Particles around timeline
        double px = sp.getX();
        double py = sp.getY() + 1;
        double pz = sp.getZ();
        sl.sendParticles(ParticleTypes.END_ROD, px, py, pz, 4, 1.5, 1.5, 1.5, 0.05);
        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, px, py, pz, 3, 1.5, 1.5, 1.5, 0.1);

        ticks--;
        if (ticks <= 0) {
            DioState.TIMESTOP_TICKS.remove(uuid);
            DioState.FROZEN_ENTITIES.remove(uuid);
            sp.sendSystemMessage(Component.literal("§e[The World] §7Time resumes."));
            // Explosion release
            sl.sendParticles(ParticleTypes.EXPLOSION, px, py, pz, 3, 1.5, 1.5, 1.5, 0.1);
        } else {
            DioState.TIMESTOP_TICKS.put(uuid, ticks);
        }
    }
}
