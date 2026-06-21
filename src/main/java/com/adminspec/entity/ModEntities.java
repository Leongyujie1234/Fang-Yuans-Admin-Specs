/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.EntityType$Builder
 *  net.minecraft.world.entity.MobCategory
 *  net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent
 *  net.neoforged.neoforge.registries.DeferredHolder
 *  net.neoforged.neoforge.registries.DeferredRegister
 */
package com.adminspec.entity;

import com.adminspec.entity.FlyingHeadEntity;
import com.adminspec.entity.KnifeProjectileEntity;
import com.adminspec.entity.TheWorldStandEntity;
import com.adminspec.entity.YamaChildEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create((ResourceKey)Registries.ENTITY_TYPE, (String)"adminspec");
    public static final DeferredHolder<EntityType<?>, EntityType<FlyingHeadEntity>> FLYING_HEAD = ENTITIES.register("flying_head", () -> EntityType.Builder.of(FlyingHeadEntity::new, (MobCategory)MobCategory.MISC).sized(0.5f, 0.5f).clientTrackingRange(64).updateInterval(3).fireImmune().build("flying_head"));
    public static final DeferredHolder<EntityType<?>, EntityType<YamaChildEntity>> YAMA_CHILD = ENTITIES.register("yama_child", () -> EntityType.Builder.of(YamaChildEntity::new, (MobCategory)MobCategory.MONSTER).sized(0.6f, 1.0f).clientTrackingRange(16).build("yama_child"));

    public static final DeferredHolder<EntityType<?>, EntityType<TheWorldStandEntity>> THE_WORLD = ENTITIES.register("the_world",
        () -> EntityType.Builder.<TheWorldStandEntity>of(TheWorldStandEntity::new, MobCategory.CREATURE)
            .sized(0.6f, 1.8f).clientTrackingRange(64).updateInterval(3).build("the_world"));

    public static final DeferredHolder<EntityType<?>, EntityType<KnifeProjectileEntity>> KNIFE = ENTITIES.register("knife",
        () -> EntityType.Builder.<KnifeProjectileEntity>of((type, level) -> new KnifeProjectileEntity((EntityType<? extends AbstractArrow>) type, level), MobCategory.MISC)
            .sized(0.25f, 0.25f).clientTrackingRange(6).updateInterval(20).build("knife"));

    public static void onRegisterAttributes(EntityAttributeCreationEvent event) {
        event.put((EntityType)YAMA_CHILD.get(), YamaChildEntity.createYamaAttributes().build());
        event.put((EntityType)THE_WORLD.get(), TheWorldStandEntity.createAttributes().build());
    }
}

