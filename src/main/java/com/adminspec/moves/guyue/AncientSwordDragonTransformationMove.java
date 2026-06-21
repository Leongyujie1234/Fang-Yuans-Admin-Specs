package com.adminspec.moves.guyue;

import com.adminspec.AdminSpecMod;
import com.adminspec.capability.PlayerSpecCapability;
import com.adminspec.capability.PlayerSpecData;
import com.adminspec.network.DragonBreathVfxPayload;
import com.adminspec.network.DragonFormPayload;
import com.adminspec.spec.MoveContext;
import com.adminspec.spec.SpecMove;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public class AncientSwordDragonTransformationMove extends SpecMove {
    public static final String ID = "ancient_sword_dragon_transformation";

    private static final float  BREATH_DAMAGE    = 6.0f;
    private static final double BREATH_RANGE     = 16.0;

    public AncientSwordDragonTransformationMove() {
        super(ID,
            Component.literal("Ancient Sword Dragon Transformation"),
            Component.literal("Transform into the Ancient Sword Dragon. Velocity-based flight. M1 breathes sword qi. 5-minute duration."));
    }

    @Override
    public void activate(MoveContext ctx) {
        if (ctx.player().level().isClientSide) return;
        if (!(ctx.player() instanceof ServerPlayer sp)) return;
        if (!ctx.pressed()) return;

        PlayerSpecData data = PlayerSpecCapability.get(sp);
        if (data.isDragonFormActive()) {
            detransform(sp, data);
        } else {
            transform(sp, data);
        }
    }

    @Override
    public void tick(Player player) {
        if (player.level().isClientSide) return;
        if (!(player instanceof ServerPlayer sp)) return;

        PlayerSpecData data = PlayerSpecCapability.get(sp);

        // Tick breath cooldown always
        if (data.getDragonBreathCooldown() > 0) {
            data.setDragonBreathCooldown(data.getDragonBreathCooldown() - 1);
        }

        if (!data.isDragonFormActive()) return;

        data.incrementDragonFormTicks();

        // Time expiry check
        if (data.getDragonFormTicks() >= PlayerSpecData.getDragonFormMaxDuration()) {
            sp.sendSystemMessage(Component.literal("§6[Ancient Sword Dragon] §cTime has expired. Detransforming."));
            detransform(sp, data);
            return;
        }

        // Broadcast state every 10 ticks so clients see accurate dragonFormTicks for progressive model
        if (data.getDragonFormTicks() % 10 == 0) {
            com.adminspec.network.SpecStatePayload.broadcast(sp);
        }

        // Flight control: noGravity prevents falling, manual velocity gives direct control.
        // We intentionally do NOT set mayfly=true — vanilla creative flight would overwrite
        // our setDeltaMovement with input-based velocity. Instead, setNoGravity + direct
        // velocity lets us control movement completely.
        if (!sp.isNoGravity()) {
            sp.setNoGravity(true);
        }

        // Direct velocity flight: no acceleration buildup, immediate response
        Vec3 look = sp.getLookAngle();
        Vec3 lookH = new Vec3(look.x, 0, look.z);
        if (lookH.lengthSqr() < 1.0E-6) {
            lookH = new Vec3(0, 0, 1);
        } else {
            lookH = lookH.normalize();
        }
        Vec3 right = new Vec3(-lookH.z, 0, lookH.x);

        Vec3 move = Vec3.ZERO;
        float forward = data.getDragonForward();
        float strafe  = data.getDragonStrafe();

        if (Math.abs(forward) > 0.01f) {
            move = move.add(lookH.scale(forward));
        }
        if (Math.abs(strafe) > 0.01f) {
            move = move.add(right.scale(strafe));
        }

        double speed = 0.5;
        Vec3 newVel = Vec3.ZERO;
        if (move.lengthSqr() > 1.0E-6) {
            newVel = move.normalize().scale(speed);
        }

        // Vertical: jump = ascend, sneak = descend
        if (data.isDragonJumping()) {
            newVel = newVel.add(0, 0.4, 0);
        } else if (data.isDragonSneaking()) {
            newVel = newVel.add(0, -0.4, 0);
        }

        // Upward launch during first 20 ticks
        if (data.getDragonFormTicks() < 20) {
            double progress = (double) data.getDragonFormTicks() / 20.0;
            double up = 1.3 * (1.0 - progress);
            newVel = newVel.add(0, up, 0);
        }

        sp.setDeltaMovement(newVel);
        sp.hurtMarked = true;
    }

    private void transform(ServerPlayer player, PlayerSpecData data) {
        // Save inventory
        ArrayList<ItemStack> saved = new ArrayList<>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            saved.add(player.getInventory().getItem(i).copy());
        }
        data.setDragonSavedInventory(saved);
        player.getInventory().clearContent();

        // Attribute boosts: ARMOR and ARMOR_TOUGHNESS make the dragon form tankier.
        // (We intentionally do NOT change MOVEMENT_SPEED — its base for players is ~0.7,
        //  so setting 0.15 would slow the player on foot, and flight uses raw
        //  setDeltaMovement anyway, so it has no effect on the actual flight feel.)
        try {
            AttributeInstance armor = player.getAttribute(Attributes.ARMOR);
            if (armor != null) armor.setBaseValue(20.0);
            AttributeInstance toughness = player.getAttribute(Attributes.ARMOR_TOUGHNESS);
            if (toughness != null) toughness.setBaseValue(8.0);
        } catch (Throwable ignored) {}

        // Grant mayfly so the server anti-cheat doesn't rubber-band flight.
        // DO NOT set flying=true — vanilla flight logic would override our velocity.
        player.getAbilities().mayfly = true;
        player.getAbilities().flying = false;
        player.onUpdateAbilities();

        // Launch upward on transform
        player.setDeltaMovement(0, 1.5, 0);
        player.hurtMarked = true;

        // State
        data.setDragonFormActive(true);
        data.setDragonFormTicks(0);
        data.setDragonBreathCooldown(0);
        data.setDragonForward(0);
        data.setDragonStrafe(0);

        // Notify client
        PacketDistributor.sendToPlayer(player, new DragonFormPayload(true));
        com.adminspec.network.SpecStatePayload.broadcast(player);

        player.sendSystemMessage(Component.literal(
            "§6§l[Ancient Sword Dragon] §r§eTransformed! WASD to fly, Space=up, Shift=down. M1 = Sword Qi Breath. 5 min duration."));
    }

    private void detransform(ServerPlayer player, PlayerSpecData data) {
        // Restore inventory
        player.getInventory().clearContent();
        List<ItemStack> saved = data.getDragonSavedInventory();
        for (int i = 0; i < saved.size() && i < player.getInventory().getContainerSize(); i++) {
            player.getInventory().setItem(i, saved.get(i));
        }
        data.setDragonSavedInventory(new ArrayList<>());

        // Restore attributes
        try {
            AttributeInstance armor = player.getAttribute(Attributes.ARMOR);
            if (armor != null) armor.setBaseValue(0.0);
            AttributeInstance toughness = player.getAttribute(Attributes.ARMOR_TOUGHNESS);
            if (toughness != null) toughness.setBaseValue(0.0);
        } catch (Throwable ignored) {}

        // Revoke flight unless creative/spectator
        player.setNoGravity(false);
        if (!player.isCreative() && !player.isSpectator()) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }

        data.setDragonFormActive(false);
        data.setDragonFormTicks(0);
        data.setDragonForward(0);
        data.setDragonStrafe(0);

        PacketDistributor.sendToPlayer(player, new DragonFormPayload(false));
        com.adminspec.network.SpecStatePayload.broadcast(player);

        player.sendSystemMessage(Component.literal("§6[Ancient Sword Dragon] §7Returned to human form."));
    }

    public static void handleBreath(ServerPlayer player) {
        PlayerSpecData data = PlayerSpecCapability.get(player);
        if (!data.isDragonFormActive()) {
            AdminSpecMod.LOGGER.info("[AdminSpec] Breath ignored: not in dragon form");
            return;
        }
        if (data.getDragonBreathCooldown() > 0) {
            AdminSpecMod.LOGGER.info("[AdminSpec] Breath ignored: cooldown {}", data.getDragonBreathCooldown());
            return;
        }
        AdminSpecMod.LOGGER.info("[AdminSpec] Breath triggered for {}", player.getName().getString());

        ServerLevel sl = (ServerLevel) player.level();
        Vec3 eye  = player.getEyePosition();
        Vec3 look = player.getLookAngle();

        // Server-side particles (visible to all via broadcast overload)
        for (double d = 0.5; d < BREATH_RANGE; d += 0.5) {
            Vec3 pos = eye.add(look.scale(d));
            double spread = d * 0.07;
            // CRIT particles – bright visible sword qi
            sl.sendParticles(ParticleTypes.CRIT,
                pos.x, pos.y, pos.z, 4, spread, spread, spread, 0.05);
            sl.sendParticles(ParticleTypes.ENCHANTED_HIT,
                pos.x, pos.y, pos.z, 3, spread, spread, spread, 0.03);
            if (d % 1.5 < 0.5) {
                sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    pos.x, pos.y, pos.z, 1, spread * 2, spread * 2, spread * 2, 0.0);
            }
            if (d % 3.0 < 0.5) {
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    pos.x, pos.y, pos.z, 4, spread, spread, spread, 0.1);
            }
        }

        // End burst
        Vec3 tip = eye.add(look.scale(BREATH_RANGE));
        sl.sendParticles(ParticleTypes.EXPLOSION, tip.x, tip.y, tip.z, 3, 0.8, 0.8, 0.8, 0.1);
        sl.sendParticles(ParticleTypes.FLASH, tip.x, tip.y, tip.z, 1, 0, 0, 0, 0);

        // Damage entities in beam
        AABB beamBox = player.getBoundingBox()
            .expandTowards(look.scale(BREATH_RANGE))
            .inflate(1.5);
        List<LivingEntity> victims = sl.getEntitiesOfClass(LivingEntity.class, beamBox,
            e -> e.isAlive() && !e.equals(player));
        for (LivingEntity v : victims) {
            Vec3 toV       = v.position().subtract(eye);
            double proj    = toV.dot(look);
            if (proj < 0 || proj > BREATH_RANGE) continue;
            Vec3 closest   = eye.add(look.scale(proj));
            if (v.position().distanceTo(closest) >= 2.0) continue;
            v.hurt(sl.damageSources().playerAttack(player), BREATH_DAMAGE);
        }

        data.setDragonBreathCooldown(60); // 3 seconds

        // Tell all nearby clients to play local VFX
        PacketDistributor.sendToPlayersNear(
            sl, null,
            player.getX(), player.getY(), player.getZ(), 64.0,
            new DragonBreathVfxPayload(
                player.getId(),
                eye.x, eye.y, eye.z,
                look.x, look.y, look.z)
        );
    }
}
