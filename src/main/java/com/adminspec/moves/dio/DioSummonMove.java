package com.adminspec.moves.dio;

import java.util.UUID;
import com.adminspec.spec.MoveContext;
import com.adminspec.spec.SpecMove;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

public class DioSummonMove extends SpecMove {
    public static final String ID = "dio_summon";
    private static final double DAMAGE_BOOST = 8.0;

    public DioSummonMove() {
        super(ID,
            Component.literal("The World - Summon"),
            Component.literal("Toggle your stand The World. While active, attacks deal massive damage and golden particles surround you."));
    }

    @Override
    public void activate(MoveContext ctx) {
        Player player = ctx.player();
        if (player.level().isClientSide) return;
        if (!ctx.pressed()) return;
        if (!(player instanceof ServerPlayer sp)) return;

        UUID uuid = player.getUUID();
        if (DioState.STAND_ACTIVE.contains(uuid)) {
            deactivate(sp);
        } else {
            activate(sp);
        }
    }

    private void activate(ServerPlayer player) {
        DioState.STAND_ACTIVE.add(player.getUUID());
        try {
            AttributeInstance atk = player.getAttribute(Attributes.ATTACK_DAMAGE);
            if (atk != null) atk.setBaseValue(DAMAGE_BOOST);
        } catch (Throwable ignored) {}
        player.sendSystemMessage(Component.literal("§e§l[The World] §r§6Standing tall!"));
    }

    private void deactivate(ServerPlayer player) {
        DioState.STAND_ACTIVE.remove(player.getUUID());
        try {
            AttributeInstance atk = player.getAttribute(Attributes.ATTACK_DAMAGE);
            if (atk != null) atk.setBaseValue(1.0);
        } catch (Throwable ignored) {}
        player.sendSystemMessage(Component.literal("§e§l[The World] §r§7Stand dismissed."));
    }

    @Override
    public void tick(Player player) {
        if (player.level().isClientSide) return;
        if (!(player instanceof ServerPlayer sp)) return;
        if (!DioState.STAND_ACTIVE.contains(player.getUUID())) return;

        ServerLevel sl = (ServerLevel) player.level();
        double x = player.getX();
        double y = player.getY() + 1.5;
        double z = player.getZ();
        // Golden aura particles
        sl.sendParticles(ParticleTypes.END_ROD, x, y, z, 3, 0.4, 0.6, 0.4, 0.02);
        sl.sendParticles(ParticleTypes.ENCHANTED_HIT, x, y + 0.5, z, 2, 0.3, 0.3, 0.3, 0.01);
        if (Math.random() < 0.3) {
            sl.sendParticles(ParticleTypes.INSTANT_EFFECT, x + (Math.random() - 0.5) * 1.2, y + Math.random() * 1.0, z + (Math.random() - 0.5) * 1.2, 1, 0, 0, 0, 0);
        }
    }
}
