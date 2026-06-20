/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.HolderLookup$Provider
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.phys.Vec3
 *  net.neoforged.neoforge.common.util.INBTSerializable
 */
package com.adminspec.capability;

import com.adminspec.spec.MoveContext;
import com.adminspec.spec.Spec;
import com.adminspec.spec.SpecMove;
import com.adminspec.spec.SpecRegistry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.INBTSerializable;

public final class PlayerSpecData
implements INBTSerializable<CompoundTag> {
    private String currentSpecId = null;
    private int swordEscapeCooldown = 0;
    private boolean swordEscapeDashing = false;
    private Vec3 swordEscapeDir = Vec3.ZERO;
    private int swordEscapeTicksRemaining = 0;
    private boolean swordEscapeBeheaded = false;
    private Vec3 swordEscapeStart = Vec3.ZERO;
    private Vec3 swordEscapeEnd = Vec3.ZERO;
    private int swordEscapeTotalDuration = 0;
    private boolean reverseFlowActive = false;
    private float reverseFlowCapacity = 1.0f;
    private boolean dragonFormActive = false;
    private int dragonFormTicks = 0;
    private static final int DRAGON_FORM_MAX_DURATION = 6000;
    private int dragonBreathCooldown = 0;
    private static final int DRAGON_BREATH_COOLDOWN_TICKS = 200;
    private List<ItemStack> dragonSavedInventory = new ArrayList<ItemStack>();
    private int yamaChildrenCooldown = 0;
    // Transient: set each tick by DragonFlightInputPayload from client
    private boolean dragonJumping = false;
    private boolean dragonSneaking = false;

    public String getSpecId() {
        return this.currentSpecId;
    }

    public void setSpecId(String id) {
        if (id == null && this.currentSpecId != null) {
            this.swordEscapeDashing = false;
            this.swordEscapeTicksRemaining = 0;
            this.swordEscapeBeheaded = false;
        }
        this.currentSpecId = id;
    }

    public int getSwordEscapeCooldown() {
        return this.swordEscapeCooldown;
    }

    public void setSwordEscapeCooldown(int v) {
        this.swordEscapeCooldown = Math.max(0, v);
    }

    public boolean isSwordEscapeDashing() {
        return this.swordEscapeDashing;
    }

    public Vec3 getSwordEscapeDirection() {
        return this.swordEscapeDir;
    }

    public boolean hasSEscapeBeheaded() {
        return this.swordEscapeBeheaded;
    }

    public void markSwordEscapeBeheaded() {
        this.swordEscapeBeheaded = true;
    }

    public Vec3 getSwordEscapeStart() {
        return this.swordEscapeStart;
    }

    public Vec3 getSwordEscapeEnd() {
        return this.swordEscapeEnd;
    }

    public int getSwordEscapeTotalDuration() {
        return this.swordEscapeTotalDuration;
    }

    public int getSwordEscapeTicksRemaining() {
        return this.swordEscapeTicksRemaining;
    }

    public boolean isReverseFlowActive() {
        return this.reverseFlowActive;
    }

    public void setReverseFlowActive(boolean v) {
        this.reverseFlowActive = v;
    }

    public float getReverseFlowCapacity() {
        return this.reverseFlowCapacity;
    }

    public void setReverseFlowCapacity(float v) {
        this.reverseFlowCapacity = Math.max(0.0f, Math.min(1.0f, v));
    }

    public static int getDragonFormMaxDuration() {
        return 6000;
    }

    public static int getDragonBreathCooldownTicks() {
        return 200;
    }

    public boolean isDragonFormActive() {
        return this.dragonFormActive;
    }

    public void setDragonFormActive(boolean v) {
        this.dragonFormActive = v;
    }

    public int getDragonFormTicks() {
        return this.dragonFormTicks;
    }

    public void setDragonFormTicks(int v) {
        this.dragonFormTicks = v;
    }

    public void incrementDragonFormTicks() {
        ++this.dragonFormTicks;
    }

    public int getDragonBreathCooldown() {
        return this.dragonBreathCooldown;
    }

    public void setDragonBreathCooldown(int v) {
        this.dragonBreathCooldown = Math.max(0, v);
    }

    public List<ItemStack> getDragonSavedInventory() {
        return this.dragonSavedInventory;
    }

    public void setDragonSavedInventory(List<ItemStack> inv) {
        this.dragonSavedInventory = inv;
    }

    public int getYamaChildrenCooldown() {
        return this.yamaChildrenCooldown;
    }

    public void setYamaChildrenCooldown(int v) {
        this.yamaChildrenCooldown = Math.max(0, v);
    }

    public boolean isDragonJumping() {
        return this.dragonJumping;
    }

    public void setDragonJumping(boolean v) {
        this.dragonJumping = v;
    }

    public boolean isDragonSneaking() {
        return this.dragonSneaking;
    }

    public void setDragonSneaking(boolean v) {
        this.dragonSneaking = v;
    }

    public void startSwordEscape(Vec3 dir, Vec3 start, Vec3 end, int durationTicks) {
        this.swordEscapeDir = dir;
        this.swordEscapeStart = start;
        this.swordEscapeEnd = end;
        this.swordEscapeTotalDuration = durationTicks;
        this.swordEscapeTicksRemaining = durationTicks;
        this.swordEscapeDashing = true;
        this.swordEscapeBeheaded = false;
    }

    public void tickSwordEscape() {
        if (!this.swordEscapeDashing) {
            return;
        }
        if (this.swordEscapeTicksRemaining > 0) {
            --this.swordEscapeTicksRemaining;
        }
        if (this.swordEscapeTicksRemaining <= 0) {
            this.endSwordEscape();
        }
    }

    public void endSwordEscape() {
        this.swordEscapeDashing = false;
        this.swordEscapeTicksRemaining = 0;
        this.swordEscapeBeheaded = false;
    }

    public void serverTick(Player player, Spec spec) {
        for (SpecMove m : spec.moves()) {
            m.tick(player);
        }
    }

    public void activateMove(Player player, String moveId, boolean pressed) {
        if (this.currentSpecId == null) {
            return;
        }
        Spec spec = SpecRegistry.get(this.currentSpecId);
        if (spec == null) {
            return;
        }
        for (SpecMove move : spec.moves()) {
            if (!move.id().equals(moveId)) continue;
            move.activate(new MoveContext(player, pressed));
            return;
        }
    }

    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        if (this.currentSpecId != null) {
            tag.putString("SpecId", this.currentSpecId);
        }
        tag.putInt("SwordEscapeCD", this.swordEscapeCooldown);
        tag.putBoolean("SwordEscapeDashing", this.swordEscapeDashing);
        tag.putInt("SwordEscapeTicks", this.swordEscapeTicksRemaining);
        tag.putBoolean("SwordEscapeBeheaded", this.swordEscapeBeheaded);
        tag.putDouble("SwordEscapeDX", this.swordEscapeDir.x);
        tag.putDouble("SwordEscapeDY", this.swordEscapeDir.y);
        tag.putDouble("SwordEscapeDZ", this.swordEscapeDir.z);
        tag.putDouble("SwordEscapeSX", this.swordEscapeStart.x);
        tag.putDouble("SwordEscapeSY", this.swordEscapeStart.y);
        tag.putDouble("SwordEscapeSZ", this.swordEscapeStart.z);
        tag.putDouble("SwordEscapeEX", this.swordEscapeEnd.x);
        tag.putDouble("SwordEscapeEY", this.swordEscapeEnd.y);
        tag.putDouble("SwordEscapeEZ", this.swordEscapeEnd.z);
        tag.putInt("SwordEscapeTotal", this.swordEscapeTotalDuration);
        tag.putBoolean("ReverseFlowActive", this.reverseFlowActive);
        tag.putFloat("ReverseFlowCap", this.reverseFlowCapacity);
        return tag;
    }

    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        if (tag == null) {
            return;
        }
        if (tag.contains("SpecId")) {
            this.currentSpecId = tag.getString("SpecId");
        }
        if (tag.contains("SwordEscapeCD")) {
            this.swordEscapeCooldown = tag.getInt("SwordEscapeCD");
        }
        if (tag.contains("SwordEscapeDashing")) {
            this.swordEscapeDashing = tag.getBoolean("SwordEscapeDashing");
        }
        if (tag.contains("SwordEscapeTicks")) {
            this.swordEscapeTicksRemaining = tag.getInt("SwordEscapeTicks");
        }
        if (tag.contains("SwordEscapeBeheaded")) {
            this.swordEscapeBeheaded = tag.getBoolean("SwordEscapeBeheaded");
        }
        double dx = tag.contains("SwordEscapeDX") ? tag.getDouble("SwordEscapeDX") : 0.0;
        double dy = tag.contains("SwordEscapeDY") ? tag.getDouble("SwordEscapeDY") : 0.0;
        double dz = tag.contains("SwordEscapeDZ") ? tag.getDouble("SwordEscapeDZ") : 0.0;
        this.swordEscapeDir = new Vec3(dx, dy, dz);
        double sx = tag.contains("SwordEscapeSX") ? tag.getDouble("SwordEscapeSX") : 0.0;
        double sy = tag.contains("SwordEscapeSY") ? tag.getDouble("SwordEscapeSY") : 0.0;
        double sz = tag.contains("SwordEscapeSZ") ? tag.getDouble("SwordEscapeSZ") : 0.0;
        this.swordEscapeStart = new Vec3(sx, sy, sz);
        double ex = tag.contains("SwordEscapeEX") ? tag.getDouble("SwordEscapeEX") : 0.0;
        double ey = tag.contains("SwordEscapeEY") ? tag.getDouble("SwordEscapeEY") : 0.0;
        double ez = tag.contains("SwordEscapeEZ") ? tag.getDouble("SwordEscapeEZ") : 0.0;
        this.swordEscapeEnd = new Vec3(ex, ey, ez);
        if (tag.contains("SwordEscapeTotal")) {
            this.swordEscapeTotalDuration = tag.getInt("SwordEscapeTotal");
        }
        if (tag.contains("ReverseFlowActive")) {
            this.reverseFlowActive = tag.getBoolean("ReverseFlowActive");
        }
        if (tag.contains("ReverseFlowCap")) {
            this.reverseFlowCapacity = tag.getFloat("ReverseFlowCap");
        }
    }

    public void copyFrom(PlayerSpecData other) {
        this.currentSpecId = other.currentSpecId;
        this.swordEscapeCooldown = other.swordEscapeCooldown;
        this.swordEscapeDashing = other.swordEscapeDashing;
        this.swordEscapeDir = other.swordEscapeDir;
        this.swordEscapeTicksRemaining = other.swordEscapeTicksRemaining;
        this.swordEscapeBeheaded = other.swordEscapeBeheaded;
        this.swordEscapeStart = other.swordEscapeStart;
        this.swordEscapeEnd = other.swordEscapeEnd;
        this.swordEscapeTotalDuration = other.swordEscapeTotalDuration;
        this.reverseFlowActive = other.reverseFlowActive;
        this.reverseFlowCapacity = other.reverseFlowCapacity;
    }
}

