/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.logging.LogUtils
 *  net.minecraft.commands.CommandSourceStack
 *  net.neoforged.bus.api.IEventBus
 *  net.neoforged.fml.common.Mod
 *  net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
 *  net.neoforged.fml.loading.FMLEnvironment
 *  net.neoforged.neoforge.common.NeoForge
 *  net.neoforged.neoforge.event.RegisterCommandsEvent
 *  org.slf4j.Logger
 */
package com.adminspec;

import com.adminspec.ModSounds;
import com.adminspec.capability.BlockRecoveryManager;
import com.adminspec.capability.PlayerSpecCapability;
import com.adminspec.client.ClientSetup;
import com.adminspec.command.AdminSpecCommand;
import com.adminspec.entity.ModEntities;
import com.adminspec.item.ModItems;
import com.adminspec.moves.ModMoves;
import com.adminspec.spec.SpecRegistry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

@Mod(value="adminspec")
public class AdminSpecMod {
    public static final String MOD_ID = "adminspec";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AdminSpecMod(IEventBus modBus) {
        ModEntities.ENTITIES.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModMoves.MOVES.register(modBus);
        ModSounds.SOUND_EVENTS.register(modBus);
        PlayerSpecCapability.ATTACHMENT_TYPES.register(modBus);
        BlockRecoveryManager.ATTACHMENT_TYPES.register(modBus);
        SpecRegistry.registerDefaults();
        LOGGER.info("Admin Spec mod: registered {} specs.", (Object)SpecRegistry.size());
        modBus.addListener(this::commonSetup);
        modBus.addListener(ModEntities::onRegisterAttributes);
        if (FMLEnvironment.dist.isClient()) {
            modBus.addListener(ClientSetup::onClientSetup);
            modBus.addListener(ClientSetup::onRegisterKeyMappings);
            modBus.addListener(ClientSetup::onRegisterRenderers);
        }
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(PlayerSpecCapability::onPlayerClone);
        NeoForge.EVENT_BUS.addListener(PlayerSpecCapability::onPlayerRespawn);
        NeoForge.EVENT_BUS.addListener(PlayerSpecCapability::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(PlayerSpecCapability::onPlayerStartTracking);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        AdminSpecCommand.register((CommandDispatcher<CommandSourceStack>)event.getDispatcher());
    }
}

