package com.adminspec.moves.dio;

import com.adminspec.entity.TheWorldStandEntity;
import com.adminspec.spec.MoveContext;
import com.adminspec.spec.SpecMove;
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
        int cd = DioStandState.BARRAGE_CD.getOrDefault(sp.getUUID(), 0);
        if (cd > 0) {
            sp.sendSystemMessage(Component.literal("§e[Barrage] §7Cooldown: §f" + String.format("%.1f", cd / 20.0) + "s"));
            return;
        }
        DioStandState.BARRAGE_TICKS.put(sp.getUUID(), DioStandState.BARRAGE_DURATION);
        DioStandState.BARRAGE_CD.put(sp.getUUID(), DioStandState.BARRAGE_COOLDOWN);
        TheWorldStandEntity stand = DioStandState.getStand(sp);
        if (stand != null) stand.playAnimation("animation.theworld.barrage");
        sp.sendSystemMessage(Component.literal("§e§lMUDA MUDA MUDA!"));
    }

    @Override
    public void tick(Player player) {
        if (player.level().isClientSide) return;
        if (!(player instanceof ServerPlayer sp)) return;
        UUID uuid = sp.getUUID();
        // Tick cooldown
        int cd = DioStandState.BARRAGE_CD.getOrDefault(uuid, 0);
        if (cd > 0) DioStandState.BARRAGE_CD.put(uuid, cd - 1);
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

        ticks--;
        if (ticks <= 0) {
            DioStandState.BARRAGE_TICKS.remove(uuid);
        } else {
            DioStandState.BARRAGE_TICKS.put(uuid, ticks);
        }
    }
}
