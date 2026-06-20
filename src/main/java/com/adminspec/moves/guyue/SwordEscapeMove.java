/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.component.DataComponents
 *  net.minecraft.core.particles.DustParticleOptions
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.core.particles.ParticleTypes
 *  net.minecraft.network.chat.Component
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.Entity$RemovalReason
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.item.component.ResolvableProfile
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.ClipContext
 *  net.minecraft.world.level.ClipContext$Block
 *  net.minecraft.world.level.ClipContext$Fluid
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.HitResult$Type
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Vector3f
 */
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class SwordEscapeMove
extends SpecMove {
    public static final String ID = "sword_escape";
    private static final int COOLDOWN_TICKS = 100;
    private static final int INVULN_TICKS = 10;
    private static final double DASH_DISTANCE = 12.0;
    private static final int DASH_DURATION_TICKS = 10;
    private static final float DAMAGE = 2.0f;
    private static final DustParticleOptions BLOOD_PARTICLE = new DustParticleOptions(new Vector3f(0.75f, 0.0f, 0.0f), 2.0f);

    public SwordEscapeMove() {
        super(ID, (Component)Component.literal((String)"Sword Escape"), (Component)Component.literal((String)"Dash forward 12 blocks as a streak of sword light. Deals 2 damage. The first target that dies from the dash is beheaded."));
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
            player.sendSystemMessage((Component)Component.literal((String)("\u00a7b[Sword Escape] \u00a77Cooling down: " + data.getSwordEscapeCooldown() / 20 + "s")));
            return;
        }
        ServerLevel sl = (ServerLevel)player.level();
        float damage = 2.0f;
        Vec3 look = player.getLookAngle();
        if (look.lengthSqr() < 1.0E-6) {
            look = new Vec3(0.0, 0.0, 1.0);
        }
        look = look.normalize();
        Vec3 start = player.position();
        Vec3 intendedEnd = start.add(look.scale(12.0));
        Vec3 actualEnd = this.findSafeEndpoint(player, sl, start, intendedEnd);
        float pitch = 1.0f + (player.getRandom().nextFloat() - 0.5f) * 0.1f;
        sl.playSound(null, player.getX(), player.getY() + 1.0, player.getZ(), (SoundEvent)ModSounds.SWORD_ESCAPE.get(), SoundSource.PLAYERS, 1.0f, pitch);
        Vec3 beamStart = start.add(0.0, 1.0, 0.0);
        Vec3 beamEnd = actualEnd.add(0.0, 1.0, 0.0);
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntityAndSelf(
            (net.minecraft.world.entity.Entity)player,
            (net.minecraft.network.protocol.common.custom.CustomPacketPayload)new com.adminspec.network.SwordEscapeBeamPayload(beamStart.x, beamStart.y, beamStart.z, beamEnd.x, beamEnd.y, beamEnd.z),
            (net.minecraft.network.protocol.common.custom.CustomPacketPayload[])new net.minecraft.network.protocol.common.custom.CustomPacketPayload[0]
        );
        Vec3 beamDir = beamEnd.subtract(beamStart);
        double beamLen = beamDir.length();
        if (beamLen > 0.1) {
            Vec3 stepDir = beamDir.normalize();
            for (double d = 0.0; d <= beamLen; d += 0.6) {
                Vec3 pos = beamStart.add(stepDir.scale(d));
                sl.sendParticles((ServerPlayer)player, (ParticleOptions)ParticleTypes.END_ROD, true, pos.x, pos.y, pos.z, 5, 0.1, 0.15, 0.1, 0.04);
                sl.sendParticles((ParticleOptions)ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 3, 0.1, 0.15, 0.1, 0.04);
            }
        }
        data.startSwordEscape(look, start, actualEnd, 10);
        data.setSwordEscapeCooldown(100);
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
            this.tickDashWithDamage(player, data, (ServerLevel)player.level(), true, 2.0f);
            if (!data.isSwordEscapeDashing()) {
                player.setInvulnerable(false);
                player.setInvisible(false);
            }
        }
    }

    private void tickDash(Player player, PlayerSpecData data, ServerLevel sl) {
        this.tickDashWithDamage(player, data, sl, true, 2.0f);
    }

    private void tickDashWithDamage(Player player, PlayerSpecData data, ServerLevel sl, boolean hasSword, float damage) {
        Vec3 start = data.getSwordEscapeStart();
        Vec3 end = data.getSwordEscapeEnd();
        int total = data.getSwordEscapeTotalDuration();
        int remaining = data.getSwordEscapeTicksRemaining();
        if (total <= 0) {
            return;
        }
        player.setInvulnerable(true);
        player.setInvisible(true);
        float progressPrev = (float)(total - (remaining + 1)) / (float)total;
        float progressCur = (float)(total - remaining) / (float)total;
        float easedPrev = SwordEscapeMove.easeOut(progressPrev);
        float easedCur = SwordEscapeMove.easeOut(progressCur);
        Vec3 prevPos = SwordEscapeMove.lerp(start, end, easedPrev);
        Vec3 curPos = SwordEscapeMove.lerp(start, end, easedCur);
        if (player instanceof ServerPlayer) {
            ServerPlayer sp = (ServerPlayer)player;
            sp.teleportTo(curPos.x, curPos.y, curPos.z);
        } else {
            player.setPos(curPos.x, curPos.y, curPos.z);
        }
        player.setDeltaMovement(Vec3.ZERO);
        player.hurtMarked = true;
        if (hasSword && damage > 0.0f) {
            AABB segBox = new AABB(prevPos, curPos).inflate(1.0, 1.5, 1.0);
            List<LivingEntity> victims = sl.getEntitiesOfClass(LivingEntity.class, segBox, e -> e.isAlive() && !e.equals(player));
            for (LivingEntity v : victims) {
                float healthBefore = v.getHealth();
                v.hurt(sl.damageSources().playerAttack(player), damage);
                if (data.hasSEscapeBeheaded() || !v.isDeadOrDying() && !(v.getHealth() <= 0.0f)) continue;
                this.behead(player, v, data.getSwordEscapeDirection());
                data.markSwordEscapeBeheaded();
            }
        }
        data.tickSwordEscape();
    }

    private static float easeOut(float t) {
        if (t <= 0.0f) {
            return 0.0f;
        }
        if (t >= 1.0f) {
            return 1.0f;
        }
        float f = 1.0f - t;
        return 1.0f - f * f * f;
    }

    private static Vec3 lerp(Vec3 a, Vec3 b, float t) {
        return new Vec3(a.x + (b.x - a.x) * (double)t, a.y + (b.y - a.y) * (double)t, a.z + (b.z - a.z) * (double)t);
    }

    private Vec3 findSafeEndpoint(Player player, ServerLevel sl, Vec3 start, Vec3 intendedEnd) {
        Vec3 back2D;
        Vec3 horizontalOffset;
        Vec3 eyeEnd;
        Vec3 eyeStart = start.add(0.0, (double)player.getEyeHeight(), 0.0);
        ClipContext ctx = new ClipContext(eyeStart, eyeEnd = eyeStart.add(intendedEnd.subtract(start)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, (Entity)player);
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
        BlockPos footPos = BlockPos.containing((double)pos.x, (double)pos.y, (double)pos.z);
        BlockState state = sl.getBlockState(footPos);
        return !state.getCollisionShape((BlockGetter)sl, footPos).isEmpty();
    }

    private void behead(Player dasher, LivingEntity victim, Vec3 dashDir) {
        ServerLevel sl = (ServerLevel)victim.level();
        Vec3 neckPos = victim.position().add(0.0, (double)victim.getBbHeight() * 0.75, 0.0);
        ItemStack headItem = this.getHeadItemFor(victim);
        if (headItem != null) {
            FlyingHeadEntity head = new FlyingHeadEntity((EntityType)ModEntities.FLYING_HEAD.get(), (Level)sl);
            head.init(neckPos, headItem, dashDir);
            sl.addFreshEntity((Entity)head);
        }
        sl.sendParticles((ParticleOptions)BLOOD_PARTICLE, neckPos.x, neckPos.y, neckPos.z, 40, 0.5, 0.5, 0.5, 0.3);
        if (dasher instanceof ServerPlayer) {
            ServerPlayer sp = (ServerPlayer)dasher;
            sl.sendParticles(sp, (ParticleOptions)BLOOD_PARTICLE, true, neckPos.x, neckPos.y, neckPos.z, 40, 0.5, 0.5, 0.5, 0.3);
        }
        victim.remove(Entity.RemovalReason.KILLED);
        dasher.sendSystemMessage((Component)Component.literal((String)("\u00a7c\u00a7l[Sword Escape] \u00a7r\u00a7cBeheaded " + victim.getDisplayName().getString() + "\u00a7c!")));
        if (sl.getServer() != null) {
            sl.getServer().getPlayerList().broadcastSystemMessage((Component)Component.literal((String)("\u00a7c" + dasher.getDisplayName().getString() + " beheaded " + victim.getDisplayName().getString() + " with Sword Escape!")), false);
        }
    }

    private ItemStack getHeadItemFor(LivingEntity victim) {
        if (victim instanceof Player) {
            Player p = (Player)victim;
            ItemStack head = new ItemStack((ItemLike)Items.PLAYER_HEAD);
            try {
                ResolvableProfile profile = new ResolvableProfile(p.getGameProfile());
                head.set(DataComponents.PROFILE, profile);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            return head;
        }
        EntityType type = victim.getType();
        if (type == EntityType.ZOMBIE) {
            return new ItemStack((ItemLike)Items.ZOMBIE_HEAD);
        }
        if (type == EntityType.SKELETON) {
            return new ItemStack((ItemLike)Items.SKELETON_SKULL);
        }
        if (type == EntityType.CREEPER) {
            return new ItemStack((ItemLike)Items.CREEPER_HEAD);
        }
        if (type == EntityType.WITHER_SKELETON) {
            return new ItemStack((ItemLike)Items.WITHER_SKELETON_SKULL);
        }
        if (type == EntityType.PIGLIN) {
            return new ItemStack((ItemLike)Items.PIGLIN_HEAD);
        }
        if (type == EntityType.ENDER_DRAGON) {
            return new ItemStack((ItemLike)Items.DRAGON_HEAD);
        }
        return null;
    }
}

