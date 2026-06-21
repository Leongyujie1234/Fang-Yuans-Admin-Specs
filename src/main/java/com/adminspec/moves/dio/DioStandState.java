package com.adminspec.moves.dio;

import com.adminspec.AdminSpecMod;
import com.adminspec.entity.ModEntities;
import com.adminspec.entity.TheWorldStandEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class DioStandState {
    private static final Map<UUID, Integer> STAND_ENTITY = new ConcurrentHashMap<>();
    public static final Map<UUID, Integer> BARRAGE_TICKS = new ConcurrentHashMap<>();
    public static final Map<UUID, Integer> CHARGE_TICKS = new ConcurrentHashMap<>();
    public static final Map<UUID, Integer> TIMESTOP_TICKS = new ConcurrentHashMap<>();
    public static final Map<UUID, Map<UUID, Vec3>> FROZEN = new ConcurrentHashMap<>();
    public static final Map<UUID, Integer> BARRAGE_CD = new ConcurrentHashMap<>();
    public static final Map<UUID, Integer> CHARGE_CD = new ConcurrentHashMap<>();
    public static final Map<UUID, Integer> TIMESTOP_CD = new ConcurrentHashMap<>();
    // Server-side timestop list (like JCraft's Timestops)
    static final List<ActiveTimestop> SERVER_TIMESTOPS = new ArrayList<>();

    // JCraft-exact parameters
    public static final int BARRAGE_DURATION = 40;
    public static final int BARRAGE_COOLDOWN = 280;
    public static final int BARRAGE_INTERVAL = 3;
    public static final float BARRAGE_DAMAGE = 1.0f;
    public static final float BARRAGE_KNOCKBACK = 0.25f;
    public static final int BARRAGE_REACH = 3;

    public static final int CHARGE_WINDUP = 7;
    public static final int CHARGE_DURATION = 19;
    public static final int CHARGE_COOLDOWN = 100;
    public static final float CHARGE_DAMAGE = 5.0f;
    public static final float CHARGE_KNOCKBACK = 0.25f;
    public static final double CHARGE_RANGE = 7.5;

    public static final int TIMESTOP_WINDUP = 45;
    public static final int TIMESTOP_COOLDOWN = 1400;
    public static final int TIMESTOP_FREEZE_DURATION = 80;

    public static TheWorldStandEntity getStand(Player player) {
        Integer id = STAND_ENTITY.get(player.getUUID());
        if (id == null) return null;
        if (player.level().isClientSide) return null;
        var e = ((ServerLevel) player.level()).getEntity(id);
        return e instanceof TheWorldStandEntity tw ? tw : null;
    }

    public static void ensureStand(ServerPlayer player) {
        if (getStand(player) != null) return;
        TheWorldStandEntity stand = new TheWorldStandEntity(ModEntities.THE_WORLD.get(), player.level());
        stand.setPos(player.getX(), player.getY(), player.getZ());
        stand.setOwner(player);
        player.level().addFreshEntity(stand);
        STAND_ENTITY.put(player.getUUID(), stand.getId());
        AdminSpecMod.LOGGER.info("[DIO] Spawned The World for {}", player.getName().getString());
    }

    public static void removeStand(Player player) {
        TheWorldStandEntity stand = getStand(player);
        if (stand != null) stand.discard();
        STAND_ENTITY.remove(player.getUUID());
        BARRAGE_TICKS.remove(player.getUUID());
        CHARGE_TICKS.remove(player.getUUID());
        TIMESTOP_TICKS.remove(player.getUUID());
        FROZEN.remove(player.getUUID());
        BARRAGE_CD.remove(player.getUUID());
        CHARGE_CD.remove(player.getUUID());
        TIMESTOP_CD.remove(player.getUUID());
    }

    // Server tick: runs every server tick to process active timestops
    public static void serverTick() {
        Iterator<ActiveTimestop> it = SERVER_TIMESTOPS.iterator();
        while (it.hasNext()) {
            ActiveTimestop ts = it.next();
            if (ts.ticksRemaining <= 0 || ts.user == null || !ts.user.isAlive()) {
                ts.onEnd();
                it.remove();
                continue;
            }
            ts.ticksRemaining--;
            ServerLevel sl = ts.world;
            if (sl == null) continue;
            Vec3 pos = ts.pos;
            AABB box = new AABB(pos.add(96, 96, 96), pos.subtract(96, 96, 96));
            List<Entity> entities = sl.getEntitiesOfClass(Entity.class, box, e -> {
                if (e == ts.user) return false;
                if (e.isSpectator()) return false;
                if (e instanceof Player p && p.isCreative()) return false;
                return true;
            });
            for (Entity e : entities) {
                if (e instanceof LivingEntity le) {
                    le.setNoActionTime(2);
                    e.teleportTo(e.xo, e.yo, e.zo);
                    e.setDeltaMovement(Vec3.ZERO);
                    e.xRotO = e.getXRot();
                    e.yRotO = e.getYRot();
                    if (e instanceof LivingEntity le2) {
                        le2.yBodyRotO = le2.yBodyRot;
                        le2.yHeadRotO = le2.yHeadRot;
                        le2.walkDistO = le2.walkDist;
                    }
                }
            }
        }
    }

    static class ActiveTimestop {
        final Entity user;
        final Vec3 pos;
        final ServerLevel world;
        int ticksRemaining;

        ActiveTimestop(Entity user, Vec3 pos, ServerLevel world, int ticks) {
            this.user = user; this.pos = pos; this.world = world; this.ticksRemaining = ticks;
        }

        void onEnd() {
            // timestop ended - unfreezing handled by expiration of noActionTime
        }
    }

    private DioStandState() {}
}
