/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.DynamicOps
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.HolderLookup$Provider
 *  net.minecraft.core.Registry
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.ListTag
 *  net.minecraft.nbt.NbtOps
 *  net.minecraft.nbt.Tag
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  net.neoforged.neoforge.attachment.AttachmentType
 *  net.neoforged.neoforge.common.util.INBTSerializable
 *  net.neoforged.neoforge.registries.DeferredRegister
 *  net.neoforged.neoforge.registries.NeoForgeRegistries
 */
package com.adminspec.capability;

import com.mojang.serialization.DynamicOps;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class BlockRecoveryManager
implements INBTSerializable<CompoundTag> {
    public static final int RECOVERY_DELAY_TICKS = 2400;
    public static final int MAX_PENDING = 5000;
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create((Registry)NeoForgeRegistries.ATTACHMENT_TYPES, (String)"adminspec");
    public static final Supplier<AttachmentType<BlockRecoveryManager>> BLOCK_RECOVERY = ATTACHMENT_TYPES.register("block_recovery", () -> AttachmentType.serializable(BlockRecoveryManager::new).build());
    private final List<Entry> pending = new ArrayList<Entry>();

    public static BlockRecoveryManager get(ServerLevel level) {
        return (BlockRecoveryManager)level.getData(BLOCK_RECOVERY);
    }

    public void snapshotAndSchedule(ServerLevel level, Iterable<BlockPos> positions, long currentTick) {
        long dueTick = currentTick + 2400L;
        for (BlockPos pos : positions) {
            CompoundTag entry;
            block6: {
                BlockState state;
                if (pos.getY() < level.getMinBuildHeight() || pos.getY() > level.getMaxBuildHeight() || (state = level.getBlockState(pos)).isAir() || state.getDestroySpeed((BlockGetter)level, pos) < 0.0f) continue;
                entry = new CompoundTag();
                entry.putLong("Pos", pos.asLong());
                entry.putLong("Due", dueTick);
                try {
                    Tag stateTag = BlockState.CODEC.encodeStart(NbtOps.INSTANCE, state).result().orElse(null);
                    if (stateTag == null) break block6;
                    entry.put("State", stateTag);
                }
                catch (Throwable ignored) {
                    continue;
                }
            }
            BlockEntity be = level.getBlockEntity(pos);
            if (be != null) {
                try {
                    entry.put("BE", (Tag)be.saveWithFullMetadata((HolderLookup.Provider)level.registryAccess()));
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
            }
            this.pending.add(new Entry(entry, dueTick));
            if (this.pending.size() <= 5000) continue;
            this.pending.remove(0);
        }
    }

    public void tick(ServerLevel level) {
        if (this.pending.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        Iterator<Entry> it = this.pending.iterator();
        while (it.hasNext()) {
            Entry e = it.next();
            if (e.dueTick > now) break;
            this.restoreEntry(level, e.tag);
            it.remove();
        }
    }

    private void restoreEntry(ServerLevel level, CompoundTag entry) {
        BlockPos pos = BlockPos.of((long)entry.getLong("Pos"));
        if (pos.getY() < level.getMinBuildHeight() || pos.getY() > level.getMaxBuildHeight()) {
            return;
        }
        if (entry.contains("State")) {
            try {
                BlockState state = BlockState.CODEC.parse(NbtOps.INSTANCE, entry.get("State")).result().orElse(null);
                if (state != null) {
                    BlockEntity be;
                    level.setBlock(pos, state, 3);
                    if (entry.contains("BE") && (be = level.getBlockEntity(pos)) != null) {
                        be.loadWithComponents(entry.getCompound("BE"), (HolderLookup.Provider)level.registryAccess());
                        be.setChanged();
                    }
                }
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
    }

    public int pendingCount() {
        return this.pending.size();
    }

    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag root = new CompoundTag();
        ListTag list = new ListTag();
        for (Entry e : this.pending) {
            list.add(e.tag);
        }
        root.put("Pending", (Tag)list);
        return root;
    }

    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag root) {
        this.pending.clear();
        if (root == null) {
            return;
        }
        ListTag list = root.getList("Pending", 10);
        for (int i = 0; i < list.size(); ++i) {
            CompoundTag entry = list.getCompound(i);
            long due = entry.getLong("Due");
            this.pending.add(new Entry(entry, due));
        }
    }

    private static final class Entry {
        final CompoundTag tag;
        final long dueTick;

        Entry(CompoundTag tag, long dueTick) {
            this.tag = tag;
            this.dueTick = dueTick;
        }
    }
}

