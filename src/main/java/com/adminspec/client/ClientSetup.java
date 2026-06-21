/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.InputConstants
 *  com.mojang.blaze3d.platform.InputConstants$Type
 *  net.minecraft.client.KeyMapping
 *  net.minecraft.client.renderer.entity.ZombieRenderer
 *  net.minecraft.world.entity.EntityType
 *  net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
 *  net.neoforged.neoforge.client.event.EntityRenderersEvent$RegisterRenderers
 *  net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent
 *  net.neoforged.neoforge.client.settings.IKeyConflictContext
 *  net.neoforged.neoforge.client.settings.KeyConflictContext
 */
package com.adminspec.client;

import com.adminspec.AdminSpecMod;
import com.adminspec.client.FlyingHeadRenderer;
import com.adminspec.client.MoveKeybinds;
import com.adminspec.client.renderer.KnifeProjectileRenderer;
import com.adminspec.client.renderer.TheWorldStandRenderer;
import com.adminspec.entity.ModEntities;
import com.adminspec.spec.Spec;
import com.adminspec.spec.SpecMove;
import com.adminspec.spec.SpecRegistry;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.world.entity.EntityType;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.IKeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

public final class ClientSetup {
    private static final int[] DEFAULT_KEYS = new int[]{49, 50, 51, 52, 53, 54, 55};

    private ClientSetup() {
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
    }

    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        int index = 0;
        for (Spec spec : SpecRegistry.all()) {
            for (SpecMove move : spec.moves()) {
                int defaultKey = index < DEFAULT_KEYS.length ? DEFAULT_KEYS[index] : InputConstants.UNKNOWN.getValue();
                KeyMapping mapping = new KeyMapping("key.adminspec.move." + move.id(), (IKeyConflictContext)KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, defaultKey, "key.categories.adminspec");
                MoveKeybinds.register(move.id(), mapping);
                event.register(mapping);
                ++index;
            }
        }
        AdminSpecMod.LOGGER.info("Admin Spec: registered {} move keybinds.", (Object)MoveKeybinds.size());
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer((EntityType)ModEntities.FLYING_HEAD.get(), FlyingHeadRenderer::new);
        event.registerEntityRenderer((EntityType)ModEntities.YAMA_CHILD.get(), YamaChildRenderer::new);
        event.registerEntityRenderer((EntityType)ModEntities.THE_WORLD.get(), TheWorldStandRenderer::new);
        event.registerEntityRenderer((EntityType)ModEntities.KNIFE.get(), KnifeProjectileRenderer::new);
    }
}

