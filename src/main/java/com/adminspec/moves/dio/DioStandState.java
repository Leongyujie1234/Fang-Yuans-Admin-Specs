package com.adminspec.moves.dio;

import com.adminspec.AdminSpecMod;
import com.adminspec.entity.ModEntities;
import com.adminspec.entity.TheWorldStandEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DioStandState {
    private static final Map<UUID, Integer> STAND_ENTITY = new ConcurrentHashMap<>();
    public static final Map<UUID, Integer> BARRAGE_TICKS = new ConcurrentHashMap<>();
    public static final Map<UUID, Integer> CHARGE_TICKS = new ConcurrentHashMap<>();
    public static final Map<UUID, Integer> TIMESTOP_TICKS = new ConcurrentHashMap<>();
    public static final Map<UUID, Map<UUID, Vec3>> FROZEN = new ConcurrentHashMap<>();
    static final int CHARGE_DURATION = 25;
    static final int BARRAGE_DURATION = 30;
    static final int MAX_CHARGE_TRAVEL = 15;
    static final int BARRAGE_REACH = 4;

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
    }

    private DioStandState() {}
}
