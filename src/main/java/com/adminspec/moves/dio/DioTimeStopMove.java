package com.adminspec.moves.dio;

import com.adminspec.entity.TheWorldStandEntity;
import com.adminspec.network.TimeStopVfxPayload;
import com.adminspec.spec.MoveContext;
import com.adminspec.spec.SpecMove;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;
import java.util.function.Predicate;

public class DioTimeStopMove extends SpecMove {
    public static final String ID = "dio_timestop";
    private static final int RADIUS = 20;
    private static final int DURATION = 120;

    public DioTimeStopMove() {
        super(ID,
            Component.literal("The World - Time Stop"),
            Component.literal("Freeze all entities in a 20-block radius for 6 seconds."));
    }

    @Override
    public void activate(MoveContext ctx) {
        Player player = ctx.player();
        if (player.level().isClientSide) return;
        if (!ctx.pressed()) return;
        if (!(player instanceof ServerPlayer sp)) return;
        DioStandState.ensureStand(sp);
        if (DioStandState.TIMESTOP_TICKS.getOrDefault(sp.getUUID(), 0) > 0) return;
        TheWorldStandEntity stand = DioStandState.getStand(sp);
        if (stand != null) stand.playAnimation("animation.theworld.timestop");

        ServerLevel sl = (ServerLevel) sp.level();
        AABB area = sp.getBoundingBox().inflate(RADIUS);
        List<Entity> entities = sl.getEntities((Entity) null, area,
            (Predicate<? super Entity>) (e -> e.isAlive() && !e.equals(sp)));

        Map<UUID, Vec3> frozen = new HashMap<>();
        for (Entity e : entities) {
            frozen.put(e.getUUID(), e.position());
            if (e instanceof LivingEntity le) {
                le.setNoActionTime(DURATION + 20);
            }
        }
        DioStandState.FROZEN.put(sp.getUUID(), frozen);
        DioStandState.TIMESTOP_TICKS.put(sp.getUUID(), DURATION);

        // Send gray overlay to caster only
        PacketDistributor.sendToPlayer(sp, new TimeStopVfxPayload(sp.getUUID(), true));
        sp.sendSystemMessage(Component.literal("§5§lZA WARUDO! TOKI WO TOMARE!"));
    }

    @Override
    public void tick(Player player) {
        if (player.level().isClientSide) return;
        if (!(player instanceof ServerPlayer sp)) return;
        UUID uuid = sp.getUUID();
        int ticks = DioStandState.TIMESTOP_TICKS.getOrDefault(uuid, 0);
        if (ticks <= 0) return;

        ServerLevel sl = (ServerLevel) sp.level();
        Map<UUID, Vec3> frozen = DioStandState.FROZEN.get(uuid);
        if (frozen != null) {
            for (Map.Entry<UUID, Vec3> entry : frozen.entrySet()) {
                Entity e = sl.getEntity(entry.getKey());
                if (e != null && e.isAlive()) {
                    Vec3 pos = entry.getValue();
                    e.teleportTo(pos.x, pos.y, pos.z);
                    e.setDeltaMovement(Vec3.ZERO);
                    if (e instanceof LivingEntity le) {
                        le.setNoActionTime(DURATION + 20);
                    }
                }
            }
        }

        ticks--;
        if (ticks <= 0) {
            DioStandState.TIMESTOP_TICKS.remove(uuid);
            DioStandState.FROZEN.remove(uuid);
            sp.sendSystemMessage(Component.literal("§e[The World] §7Time resumes."));
            PacketDistributor.sendToPlayer(sp, new TimeStopVfxPayload(sp.getUUID(), false));
        } else {
            DioStandState.TIMESTOP_TICKS.put(uuid, ticks);
        }
    }
}
