package com.adminspec.moves.guyue;

import com.adminspec.ModSounds;
import com.adminspec.capability.PlayerSpecCapability;
import com.adminspec.capability.PlayerSpecData;
import com.adminspec.entity.FlyingHeadEntity;
import com.adminspec.entity.ModEntities;
import com.adminspec.spec.MoveContext;
import com.adminspec.spec.SpecMove;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class SwordEscapeMove extends SpecMove {
    public static final String ID = "sword_escape";
    private static final int COOLDOWN_TICKS = 100;
    private static final int DASH_DURATION_TICKS = 12;
    private static final float DAMAGE = 0.0f;
    private static final double DASH_INITIAL_SPEED = 3.0;
    private static final double DASH_DECAY_FACTOR = 0.85;
    private static final DustParticleOptions BLOOD_PARTICLE = new DustParticleOptions(new Vector3f(0.75f, 0.0f, 0.0f), 2.0f);

    public SwordEscapeMove() {
        super(ID,
            Component.literal("Sword Escape"),
            Component.literal("Dash forward smoothly as a streak of sword light. Deals 2 damage. The first target that dies from the dash is beheaded."));
    }

    @Override
    public void activate(MoveContext ctx) {
        Player player = ctx.player();
        if (player.level().isClientSide) {
            return;
        }
        if (!ctx.pressed()) {
            return;
        }
        PlayerSpecData data = PlayerSpecCapability.get(player);
        if (data.isSwordEscapeDashing()) {
            return;
        }
        if (data.getSwordEscapeCooldown() > 0) {
            player.sendSystemMessage(Component.literal("§b[Sword Escape] §7Cooling down: " + data.getSwordEscapeCooldown() / 20 + "s"));
            return;
        }
        ServerLevel sl = (ServerLevel)player.level();
        Vec3 look = player.getLookAngle();
        if (look.lengthSqr() < 1.0E-6) {
            look = new Vec3(0.0, 0.0, 1.0);
        }
        look = look.normalize();
        Vec3 start = player.position();
        Vec3 intendedEnd = start.add(look.scale(12.0));
        Vec3 actualEnd = this.findSafeEndpoint(player, sl, start, intendedEnd);
        float pitch = 1.0f + (player.getRandom().nextFloat() - 0.5f) * 0.1f;
        sl.playSound(null, player.getX(), player.getY() + 1.0, player.getZ(), ModSounds.SWORD_ESCAPE.get(), SoundSource.PLAYERS, 1.0f, pitch);
        
        Vec3 beamStart = start.add(0.0, 1.0, 0.0);
        Vec3 beamEnd = actualEnd.add(0.0, 1.0, 0.0);
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntityAndSelf(
            player,
            new com.adminspec.network.SwordEscapeBeamPayload(beamStart.x, beamStart.y, beamStart.z, beamEnd.x, beamEnd.y, beamEnd.z),
            new net.minecraft.network.protocol.common.custom.CustomPacketPayload[0]
        );

        data.startSwordEscape(look, start, actualEnd, DASH_DURATION_TICKS);
        data.setSwordEscapeCooldown(COOLDOWN_TICKS);
        player.setInvisible(true);
        player.setInvulnerable(true);
        this.tickDash(player, data, sl);
    }

    @Override
    public void tick(Player player) {
        if (player.level().isClientSide) {
            return;
        }
        PlayerSpecData data = PlayerSpecCapability.get(player);
        if (data.getSwordEscapeCooldown() > 0) {
            data.setSwordEscapeCooldown(data.getSwordEscapeCooldown() - 1);
        }
        if (data.isSwordEscapeDashing()) {
            this.tickDashWithDamage(player, data, (ServerLevel)player.level(), true, DAMAGE);
            if (!data.isSwordEscapeDashing()) {
                player.setInvulnerable(false);
                player.setInvisible(false);
            }
        }
    }

    private void tickDash(Player player, PlayerSpecData data, ServerLevel sl) {
        this.tickDashWithDamage(player, data, sl, true, DAMAGE);
    }

    private void tickDashWithDamage(Player player, PlayerSpecData data, ServerLevel sl, boolean hasSword, float damage) {
        int total = data.getSwordEscapeTotalDuration();
        int remaining = data.getSwordEscapeTicksRemaining();
        if (total <= 0) {
            return;
        }
        player.setInvulnerable(true);
        player.setInvisible(true);

        Vec3 look = data.getSwordEscapeDirection();
        // Decay speed exponentially: starts at 3.0 blocks/tick, decays smoothly
        double speed = DASH_INITIAL_SPEED * Math.pow(DASH_DECAY_FACTOR, total - remaining);
        player.setDeltaMovement(look.scale(speed));
        player.hurtMarked = true;

        // Spawn local trails
        Vec3 trailPos = player.position().add(0.0, player.getBbHeight() / 2.0, 0.0);
        sl.sendParticles(ParticleTypes.END_ROD, trailPos.x, trailPos.y, trailPos.z, 6, 0.1, 0.1, 0.1, 0.03);
        sl.sendParticles(ParticleTypes.CRIT, trailPos.x, trailPos.y, trailPos.z, 4, 0.15, 0.15, 0.15, 0.05);

        if (hasSword && damage > 0.0f) {
            AABB segBox = player.getBoundingBox().inflate(1.2, 0.6, 1.2);
            List<LivingEntity> victims = sl.getEntitiesOfClass(LivingEntity.class, segBox, e -> e.isAlive() && !e.equals(player));
            for (LivingEntity v : victims) {
                if (data.getSwordEscapeDamaged().contains(v.getUUID())) {
                    continue;
                }
                data.getSwordEscapeDamaged().add(v.getUUID());
                v.hurt(sl.damageSources().playerAttack(player), damage);
                if (!data.hasSEscapeBeheaded() && (v.isDeadOrDying() || v.getHealth() <= 0.0f)) {
                    this.behead(player, v, look);
                    data.markSwordEscapeBeheaded();
                }
            }
        }
        data.tickSwordEscape();
    }

    private Vec3 findSafeEndpoint(Player player, ServerLevel sl, Vec3 start, Vec3 intendedEnd) {
        Vec3 back2D;
        Vec3 horizontalOffset;
        Vec3 eyeEnd;
        Vec3 eyeStart = start.add(0.0, player.getEyeHeight(), 0.0);
        ClipContext ctx = new ClipContext(eyeStart, eyeEnd = eyeStart.add(intendedEnd.subtract(start)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player);
        BlockHitResult hit = sl.clip(ctx);
        if (hit.getType() == HitResult.Type.BLOCK) {
            Vec3 hitPos = hit.getLocation();
            Vec3 dir2D = new Vec3(intendedEnd.x - start.x, 0.0, intendedEnd.z - start.z);
            if (dir2D.lengthSqr() < 1.0E-6) {
                horizontalOffset = new Vec3(0.0, 0.0, 0.0);
            } else {
                dir2D = dir2D.normalize();
                double hitDist = Math.sqrt(Math.pow(hitPos.x - eyeStart.x, 2.0) + Math.pow(hitPos.z - eyeStart.z, 2.0));
                double safeDist = Math.max(0.0, hitDist - 0.5);
                horizontalOffset = dir2D.scale(safeDist);
            }
        } else {
            horizontalOffset = new Vec3(intendedEnd.x - start.x, 0.0, intendedEnd.z - start.z);
        }
        Vec3 endpoint = new Vec3(start.x + horizontalOffset.x, start.y, start.z + horizontalOffset.z);
        int maxSteps = 24;
        double stepDist = 0.5;
        while (maxSteps-- > 0 && this.isFootBlocked(sl, endpoint) && !((back2D = new Vec3(start.x - endpoint.x, 0.0, start.z - endpoint.z)).lengthSqr() < 0.01)) {
            back2D = back2D.normalize().scale(stepDist);
            endpoint = endpoint.add(back2D.x, 0.0, back2D.z);
        }
        return endpoint;
    }

    private boolean isFootBlocked(ServerLevel sl, Vec3 pos) {
        BlockPos footPos = BlockPos.containing(pos.x, pos.y, pos.z);
        BlockState state = sl.getBlockState(footPos);
        return !state.getCollisionShape(sl, footPos).isEmpty();
    }

    private void behead(Player dasher, LivingEntity victim, Vec3 dashDir) {
        ServerLevel sl = (ServerLevel)victim.level();
        Vec3 neckPos = victim.position().add(0.0, victim.getBbHeight() * 0.75, 0.0);
        ItemStack headItem = this.getHeadItemFor(victim);
        if (headItem != null) {
            FlyingHeadEntity head = new FlyingHeadEntity(ModEntities.FLYING_HEAD.get(), sl);
            head.init(neckPos, headItem, dashDir);
            sl.addFreshEntity(head);
        }
        sl.sendParticles(BLOOD_PARTICLE, neckPos.x, neckPos.y, neckPos.z, 40, 0.5, 0.5, 0.5, 0.3);
        if (dasher instanceof ServerPlayer) {
            ServerPlayer sp = (ServerPlayer)dasher;
            sl.sendParticles(sp, BLOOD_PARTICLE, true, neckPos.x, neckPos.y, neckPos.z, 40, 0.5, 0.5, 0.5, 0.3);
        }
        victim.remove(RemovalReason.KILLED);
        dasher.sendSystemMessage(Component.literal("§c§l[Sword Escape] §r§cBeheaded " + victim.getDisplayName().getString() + "§c!"));
        if (sl.getServer() != null) {
            sl.getServer().getPlayerList().broadcastSystemMessage(Component.literal("§c" + dasher.getDisplayName().getString() + " beheaded " + victim.getDisplayName().getString() + " with Sword Escape!"), false);
        }
    }

    private ItemStack getHeadItemFor(LivingEntity victim) {
        if (victim instanceof Player p) {
            ItemStack head = new ItemStack(Items.PLAYER_HEAD);
            try {
                ResolvableProfile profile = new ResolvableProfile(p.getGameProfile());
                head.set(DataComponents.PROFILE, profile);
            } catch (Throwable ignored) {}
            return head;
        }
        EntityType<?> type = victim.getType();
        if (type == EntityType.ZOMBIE) {
            return new ItemStack(Items.ZOMBIE_HEAD);
        }
        if (type == EntityType.SKELETON) {
            return new ItemStack(Items.SKELETON_SKULL);
        }
        if (type == EntityType.CREEPER) {
            return new ItemStack(Items.CREEPER_HEAD);
        }
        if (type == EntityType.WITHER_SKELETON) {
            return new ItemStack(Items.WITHER_SKELETON_SKULL);
        }
        if (type == EntityType.PIGLIN) {
            return new ItemStack(Items.PIGLIN_HEAD);
        }
        if (type == EntityType.ENDER_DRAGON) {
            return new ItemStack(Items.DRAGON_HEAD);
        }
        return null;
    }
}
