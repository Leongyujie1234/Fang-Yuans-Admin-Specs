package com.adminspec.moves.dio;

import com.adminspec.entity.TheWorldStandEntity;
import com.adminspec.network.TimeStopVfxPayload;
import com.adminspec.spec.MoveContext;
import com.adminspec.spec.SpecMove;
import net.minecraft.core.particles.ParticleTypes;
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

        ServerLevel sl = (ServerLevel) sp.level();
        double px = sp.getX(); double py = sp.getY(); double pz = sp.getZ();
        sl.sendParticles(ParticleTypes.DRAGON_BREATH, px, py + 0.5, pz, 20, 2.0, 2.0, 2.0, 0.01);
        sl.sendParticles(ParticleTypes.FLASH, px, py + 0.5, pz, 1, 0.0, 0.0, 0.0, 0.0);
        // Guaranteed explosion markers for visibility
        sl.sendParticles(ParticleTypes.EXPLOSION, px, py + 0.5, pz, 3, 0.5, 0.5, 0.5, 0);
        for (int i = 0; i < 12; ++i) {
            double angle = Math.random() * Math.PI * 2.0;
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, px + Math.cos(angle) * 3.0, py + 0.5, pz + Math.sin(angle) * 3.0, 1, 0.02, 0.02, 0.02, 0.0);
        }

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

            ServerLevel sl = (ServerLevel) sp.level();
            double px = sp.getX(); double py = sp.getY() + 0.5; double pz = sp.getZ();
            sl.sendParticles(ParticleTypes.EXPLOSION, px, py, pz, 5, 0.5, 0.5, 0.5, 0.0);
            sl.sendParticles(ParticleTypes.CRIT, px, py, pz, 15, 1.0, 1.0, 1.0, 0.05);

            sp.sendSystemMessage(Component.literal("§e[The World] §7Time resumes."));
            PacketDistributor.sendToPlayer(sp, new TimeStopVfxPayload(sp.getUUID(), false));
        } else {
            ServerLevel sl = (ServerLevel) sp.level();
            double px = sp.getX(); double py = sp.getY() + 0.5; double pz = sp.getZ();
            sl.sendParticles(ParticleTypes.DRAGON_BREATH, px + (Math.random() - 0.5) * 2.0, py + Math.random(), pz + (Math.random() - 0.5) * 2.0, 5, 0.3, 0.3, 0.3, 0.01);
            sl.sendParticles(ParticleTypes.END_ROD, px + (Math.random() - 0.5) * 2.0, py + Math.random(), pz + (Math.random() - 0.5) * 2.0, 3, 0.1, 0.1, 0.1, 0.0);

            DioStandState.TIMESTOP_TICKS.put(u, t);
        }
    }
}
