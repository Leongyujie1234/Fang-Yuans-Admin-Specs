package com.adminspec.moves.dio;

import com.adminspec.entity.TheWorldStandEntity;
import com.adminspec.network.TimeStopVfxPayload;
import com.adminspec.spec.MoveContext;
import com.adminspec.spec.SpecMove;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;

public class DioTimeStopMove extends SpecMove {
    public static final String ID = "dio_timestop";
    private static final MobEffectInstance BLINDNESS = new MobEffectInstance(MobEffects.BLINDNESS, 19, 0, true, false, false);

    public DioTimeStopMove() {
        super(ID, Component.literal("The World - Time Stop"), Component.literal("Freeze time. 70s cooldown."));
    }

    @Override public void activate(MoveContext ctx) {
        Player player = ctx.player();
        if (player.level().isClientSide) return;
        if (!ctx.pressed()) return;
        if (!(player instanceof ServerPlayer sp)) return;
        DioStandState.ensureStand(sp);
        int cd = DioStandState.TIMESTOP_CD.getOrDefault(sp.getUUID(), 0);
        if (cd > 0) { sp.sendSystemMessage(Component.literal("§5[The World] §7Time Stop: §f" + String.format("%.1f", cd/20f) + "s")); return; }
        if (DioStandState.TIMESTOP_TICKS.getOrDefault(sp.getUUID(), 0) > 0) return;
        TheWorldStandEntity stand = DioStandState.getStand(sp);
        if (stand != null) stand.playAnimation("animation.theworld.timestop");

        // Start server-managed timestop (like JCraft's Timestops system)
        DioStandState.SERVER_TIMESTOPS.add(new DioStandState.ActiveTimestop(sp, sp.position(), (ServerLevel) sp.level(), DioStandState.TIMESTOP_FREEZE_DURATION));
        DioStandState.TIMESTOP_TICKS.put(sp.getUUID(), DioStandState.TIMESTOP_WINDUP);
        DioStandState.TIMESTOP_CD.put(sp.getUUID(), DioStandState.TIMESTOP_COOLDOWN);

        // Caster-only effects (like JCraft: blindness + shader)
        sp.addEffect(new MobEffectInstance(BLINDNESS));
        PacketDistributor.sendToPlayer(sp, new TimeStopVfxPayload(sp.getUUID(), true));
        sp.sendSystemMessage(Component.literal("§5§lZA WARUDO! TOKI WO TOMARE!"));
    }

    @Override public void tick(Player player) {
        if (player.level().isClientSide) return;
        if (!(player instanceof ServerPlayer sp)) return;
        UUID u = sp.getUUID();
        int cd = DioStandState.TIMESTOP_CD.getOrDefault(u, 0);
        if (cd > 0) DioStandState.TIMESTOP_CD.put(u, cd - 1);
        int t = DioStandState.TIMESTOP_TICKS.getOrDefault(u, 0);
        if (t <= 0) return;

        t--;
        if (t <= 0) {
            DioStandState.TIMESTOP_TICKS.remove(u);
            sp.sendSystemMessage(Component.literal("§e[The World] §7Time resumes."));
            PacketDistributor.sendToPlayer(sp, new TimeStopVfxPayload(sp.getUUID(), false));
        } else {
            DioStandState.TIMESTOP_TICKS.put(u, t);
        }
    }
}
