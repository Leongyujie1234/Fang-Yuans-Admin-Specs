package com.adminspec.moves.dio;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.phys.Vec3;

public final class DioState {
    public static final Set<UUID> STAND_ACTIVE = new HashSet<>();
    public static final Map<UUID, Integer> BARRAGE_TICKS = new ConcurrentHashMap<>();
    public static final Map<UUID, Integer> TIMESTOP_TICKS = new ConcurrentHashMap<>();
    public static final Map<UUID, Integer> ROAD_ROLLER_TICKS = new ConcurrentHashMap<>();
    public static final Map<UUID, Map<UUID, Vec3>> FROZEN_ENTITIES = new ConcurrentHashMap<>();

    public static final int BARRAGE_DURATION = 20;
    public static final int TIMESTOP_DURATION = 60;
    public static final int TIMESTOP_RADIUS = 20;
    public static final int ROAD_ROLLER_DURATION = 30;
    public static final int ROAD_ROLLER_DAMAGE = 10;
    public static final double ROAD_ROLLER_RADIUS = 6.0;

    private DioState() {}
}
