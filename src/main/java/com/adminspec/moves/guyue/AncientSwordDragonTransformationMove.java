package com.adminspec.moves.guyue;

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

    // Flight constants – tuned to feel like Do a Barrel Roll style custom flight
    private static final double FLIGHT_ACCEL    = 0.08;
    private static final double FLIGHT_MAX_SPEED = 1.2;
    private static final double FLIGHT_FRICTION  = 0.90;
    private static final double VERT_ACCEL       = 0.06;

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

        // --- Custom Velocity-Based Flight ---
        // Keep mayfly+flying set so the server doesn't rubber-band us.
        // We drive motion entirely via setDeltaMovement each tick.
        if (!sp.getAbilities().mayfly) {
            sp.getAbilities().mayfly = true;
            sp.onUpdateAbilities();
        }
        // Keep flying mode active (stops gravity)
        sp.getAbilities().flying = true;
        sp.onUpdateAbilities();

        Vec3 velocity = sp.getDeltaMovement();

        if (data.getDragonFormTicks() < 20) {
            // Force upward launch during transition animation (ignoring normal flight steer)
            double progress = (double)data.getDragonFormTicks() / 20.0;
            double upwardVelocity = 1.3 * (1.0 - progress);
            sp.setDeltaMovement(velocity.x, upwardVelocity, velocity.z);
            sp.hurtMarked = true;
            return;
        }

        // Build desired movement direction from player-sent input
        Vec3 look = sp.getLookAngle();
        Vec3 moveDir = Vec3.ZERO;

        float forward = data.getDragonForward();
        float strafe  = data.getDragonStrafe();

        if (forward > 0.01f) {
            moveDir = moveDir.add(look.multiply(1, 1, 1));
        } else if (forward < -0.01f) {
            moveDir = moveDir.subtract(look.multiply(1, 1, 1));
        }

        if (Math.abs(strafe) > 0.01f) {
            // Right vector = look cross up
            Vec3 right = new Vec3(look.z, 0, -look.x).normalize();
            if (strafe > 0f) {
                moveDir = moveDir.subtract(right);  // strafe right
            } else {
                moveDir = moveDir.add(right);       // strafe left
            }
        }

        // Vertical: jump key = up, sneak = down
        if (data.isDragonJumping()) {
            moveDir = moveDir.add(0, VERT_ACCEL / FLIGHT_ACCEL, 0);
        } else if (data.isDragonSneaking()) {
            moveDir = moveDir.subtract(0, VERT_ACCEL / FLIGHT_ACCEL, 0);
        }

        // Apply acceleration
        if (moveDir.lengthSqr() > 1.0E-6) {
            Vec3 accel = moveDir.normalize().scale(FLIGHT_ACCEL);
            velocity = velocity.add(accel);
        }

        // Friction
        velocity = velocity.scale(FLIGHT_FRICTION);

        // Speed cap
        double speed = velocity.length();
        if (speed > FLIGHT_MAX_SPEED) {
            velocity = velocity.scale(FLIGHT_MAX_SPEED / speed);
        }

        sp.setDeltaMovement(velocity);
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

        // CRITICAL: grant mayfly so the server anti-cheat doesn't rubber-band flight
        player.getAbilities().mayfly = true;
        player.getAbilities().flying = true;
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
        if (!data.isDragonFormActive()) return;
        if (data.getDragonBreathCooldown() > 0) return;

        ServerLevel sl = (ServerLevel) player.level();
        Vec3 eye  = player.getEyePosition();
        Vec3 look = player.getLookAngle();

        // Server-side particles (visible to all via broadcast overload)
        for (double d = 0.5; d < BREATH_RANGE; d += 0.6) {
            Vec3 pos = eye.add(look.scale(d));
            double spread = d * 0.06;
            // CRIT particles – bright visible sword qi
            sl.sendParticles(ParticleTypes.CRIT,
                pos.x, pos.y, pos.z, 3, spread, spread, spread, 0.05);
            sl.sendParticles(ParticleTypes.ENCHANTED_HIT,
                pos.x, pos.y, pos.z, 2, spread, spread, spread, 0.03);
            if (d % 1.8 < 0.6) {
                sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    pos.x, pos.y, pos.z, 1, spread * 2, spread * 2, spread * 2, 0.0);
            }
        }

        // End burst
        Vec3 tip = eye.add(look.scale(BREATH_RANGE));
        sl.sendParticles(ParticleTypes.EXPLOSION, tip.x, tip.y, tip.z, 1, 0.5, 0.5, 0.5, 0.1);

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
