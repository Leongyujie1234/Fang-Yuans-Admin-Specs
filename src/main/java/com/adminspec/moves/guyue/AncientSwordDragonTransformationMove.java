package com.adminspec.moves.guyue;

import com.adminspec.capability.PlayerSpecCapability;
import com.adminspec.capability.PlayerSpecData;
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
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public class AncientSwordDragonTransformationMove
extends SpecMove {
    public static final String ID = "ancient_sword_dragon_transformation";
    private static final double FLIGHT_ACCEL = 0.15;
    private static final double FLIGHT_MAX_SPEED = 1.5;
    private static final double FLIGHT_FRICTION = 0.88;
    private static final float BREATH_DAMAGE = 4.0f;
    private static final double BREATH_RANGE = 16.0;
    private static final int BREATH_DURATION_TICKS = 200;
    private static final int BREATH_COOLDOWN_TICKS = 200;
    private static final double SPEED_BOOST = 0.15;
    private static final double ARMOR_BOOST = 2.0;
    private static final double ARMOR_TOUGHNESS_BOOST = 4.0;

    public AncientSwordDragonTransformationMove() {
        super(ID, (Component)Component.literal("Ancient Sword Dragon Transformation"), (Component)Component.literal("Transform into the Ancient Sword Dragon. Custom flight system (velocity-based). M1 breathes sword qi. 5-minute duration. Inventory cleared + restored on detransform."));
    }

    @Override
    public void activate(MoveContext ctx) {
        Player player = ctx.player();
        if (player.level().isClientSide) {
            return;
        }
        if (!(player instanceof ServerPlayer)) {
            return;
        }
        ServerPlayer sp = (ServerPlayer)player;
        if (!ctx.pressed()) {
            return;
        }
        PlayerSpecData data = PlayerSpecCapability.get(player);
        if (data.isDragonFormActive()) {
            this.detransform(sp, data);
            return;
        }
        this.transform(sp, data);
    }

    @Override
    public void tick(Player player) {
        if (player.level().isClientSide) {
            return;
        }
        if (!(player instanceof ServerPlayer)) {
            return;
        }
        ServerPlayer sp = (ServerPlayer)player;
        PlayerSpecData data = PlayerSpecCapability.get(player);
        if (data.getDragonBreathCooldown() > 0) {
            data.setDragonBreathCooldown(data.getDragonBreathCooldown() - 1);
        }
        if (!data.isDragonFormActive()) {
            return;
        }
        data.incrementDragonFormTicks();
        if (data.getDragonFormTicks() >= PlayerSpecData.getDragonFormMaxDuration()) {
            sp.sendSystemMessage((Component)Component.literal("\u00a76[Ancient Sword Dragon] \u00a7cTime has expired. Detransforming."));
            this.detransform(sp, data);
            return;
        }

        // Custom Flight System Physics:
        sp.setNoGravity(true);
        sp.getAbilities().mayfly = false;
        sp.getAbilities().flying = false;
        sp.onUpdateAbilities();

        Vec3 look = sp.getLookAngle();
        Vec3 moveDir = Vec3.ZERO;

        float forward = sp.zza;
        float strafe = sp.xxa;

        if (forward > 0.0f) {
            moveDir = moveDir.add(look);
        } else if (forward < 0.0f) {
            moveDir = moveDir.subtract(look);
        }

        if (strafe != 0.0f) {
            Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
            if (strafe > 0.0f) {
                moveDir = moveDir.subtract(right);
            } else {
                moveDir = moveDir.add(right);
            }
        }

        if (data.isDragonJumping()) {
            moveDir = moveDir.add(new Vec3(0, 0.6, 0));
        } else if (data.isDragonSneaking()) {
            moveDir = moveDir.subtract(new Vec3(0, 0.6, 0));
        }

        Vec3 velocity = sp.getDeltaMovement();
        if (moveDir.lengthSqr() > 1.0E-6) {
            moveDir = moveDir.normalize().scale(FLIGHT_ACCEL);
            velocity = velocity.add(moveDir);
        }

        velocity = new Vec3(velocity.x * FLIGHT_FRICTION, velocity.y * FLIGHT_FRICTION, velocity.z * FLIGHT_FRICTION);

        double speed = velocity.length();
        if (speed > FLIGHT_MAX_SPEED) {
            velocity = velocity.scale(FLIGHT_MAX_SPEED / speed);
        }

        sp.setDeltaMovement(velocity);
        sp.hurtMarked = true;
    }

    private void transform(ServerPlayer player, PlayerSpecData data) {
        ArrayList<ItemStack> saved = new ArrayList<ItemStack>();
        for (int i = 0; i < player.getInventory().getContainerSize(); ++i) {
            saved.add(player.getInventory().getItem(i).copy());
        }
        data.setDragonSavedInventory(saved);
        player.getInventory().clearContent();
        try {
            AttributeInstance toughness;
            AttributeInstance armor;
            AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) {
                speed.setBaseValue(0.15);
            }
            if ((armor = player.getAttribute(Attributes.ARMOR)) != null) {
                armor.setBaseValue(2.0);
            }
            if ((toughness = player.getAttribute(Attributes.ARMOR_TOUGHNESS)) != null) {
                toughness.setBaseValue(4.0);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        player.setNoGravity(true);
        player.getAbilities().mayfly = false;
        player.getAbilities().flying = false;
        player.onUpdateAbilities();
        player.setDeltaMovement(new Vec3(0.0, 1.2, 0.0));
        player.hurtMarked = true;
        data.setDragonFormActive(true);
        data.setDragonFormTicks(0);
        data.setDragonBreathCooldown(0);
        PacketDistributor.sendToPlayer((ServerPlayer)player, (CustomPacketPayload)new DragonFormPayload(true), (CustomPacketPayload[])new CustomPacketPayload[0]);
        com.adminspec.network.SpecStatePayload.broadcast(player);
        player.sendSystemMessage((Component)Component.literal("\u00a76\u00a7l[Ancient Sword Dragon] \u00a7r\u00a7eYou have transformed into the Ancient Sword Dragon! WASD to fly, Space to ascend, Shift to descend. M1 to breathe sword qi. 5 minutes until forced detransform."));
    }

    private void detransform(ServerPlayer player, PlayerSpecData data) {
        player.getInventory().clearContent();
        List<ItemStack> saved = data.getDragonSavedInventory();
        for (int i = 0; i < saved.size() && i < player.getInventory().getContainerSize(); ++i) {
            player.getInventory().setItem(i, saved.get(i));
        }
        data.setDragonSavedInventory(new ArrayList<ItemStack>());
        try {
            AttributeInstance toughness;
            AttributeInstance armor;
            AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) {
                speed.setBaseValue(0.1);
            }
            if ((armor = player.getAttribute(Attributes.ARMOR)) != null) {
                armor.setBaseValue(0.0);
            }
            if ((toughness = player.getAttribute(Attributes.ARMOR_TOUGHNESS)) != null) {
                toughness.setBaseValue(0.0);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        player.setNoGravity(false);
        if (!player.isCreative() && !player.isSpectator()) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
        }
        player.onUpdateAbilities();
        data.setDragonFormActive(false);
        data.setDragonFormTicks(0);
        PacketDistributor.sendToPlayer((ServerPlayer)player, (CustomPacketPayload)new DragonFormPayload(false), (CustomPacketPayload[])new CustomPacketPayload[0]);
        com.adminspec.network.SpecStatePayload.broadcast(player);
        player.sendSystemMessage((Component)Component.literal("\u00a76[Ancient Sword Dragon] \u00a77Transformed back to human form."));
    }

    public static void handleBreath(ServerPlayer player) {
        PlayerSpecData data = PlayerSpecCapability.get((Player)player);
        if (!data.isDragonFormActive()) {
            return;
        }
        if (data.getDragonBreathCooldown() > 0) {
            return;
        }
        ServerLevel sl = (ServerLevel)player.level();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 start = eye.add(look.scale(1.0));
        
        // Spawn sword qi particles
        for (double d = 0.0; d < 16.0; d += 0.5) {
            Vec3 pos = start.add(look.scale(d));
            sl.sendParticles(player, (ParticleOptions)ParticleTypes.SWEEP_ATTACK, true, pos.x, pos.y, pos.z, 2, 0.1, 0.1, 0.1, 0.05);
            sl.sendParticles(player, (ParticleOptions)ParticleTypes.CRIT, true, pos.x, pos.y, pos.z, 3, 0.08, 0.08, 0.08, 0.05);
            sl.sendParticles((ParticleOptions)ParticleTypes.SWEEP_ATTACK, pos.x, pos.y, pos.z, 1, 0.1, 0.1, 0.1, 0.05);
            sl.sendParticles((ParticleOptions)ParticleTypes.CRIT, pos.x, pos.y, pos.z, 2, 0.08, 0.08, 0.08, 0.05);
        }
        
        AABB beamBox = player.getBoundingBox().expandTowards(look.scale(16.0)).inflate(1.0);
        List<LivingEntity> victims = sl.getEntitiesOfClass(LivingEntity.class, beamBox, e -> e.isAlive() && !e.equals(player));
        for (LivingEntity v : victims) {
            Vec3 toVictim = v.position().subtract(eye);
            double projection = toVictim.dot(look);
            if (projection < 0.0 || projection > 16.0) continue;
            Vec3 closestPoint = eye.add(look.scale(projection));
            if (!(v.position().distanceTo(closestPoint) < 1.5)) continue;
            v.hurt(sl.damageSources().playerAttack((Player)player), 6.0f);
        }
        data.setDragonBreathCooldown(60); // 3 seconds cooldown
    }
}
