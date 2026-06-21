package com.adminspec.item;

import com.adminspec.AdminSpecMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(
        Registries.ITEM, AdminSpecMod.MOD_ID);

    public static final DeferredHolder<Item, KnifeItem> KNIFE = ITEMS.register("knife",
        () -> new KnifeItem(new Item.Properties().stacksTo(16)));

    private ModItems() {}
}
