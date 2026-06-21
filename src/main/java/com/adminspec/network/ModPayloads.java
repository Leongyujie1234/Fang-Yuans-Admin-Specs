/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.neoforged.bus.api.SubscribeEvent
 *  net.neoforged.fml.common.EventBusSubscriber
 *  net.neoforged.fml.common.EventBusSubscriber$Bus
 *  net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
 *  net.neoforged.neoforge.network.registration.PayloadRegistrar
 */
package com.adminspec.network;

import com.adminspec.network.ActivateMovePayload;
import com.adminspec.network.DragonBreathPayload;
import com.adminspec.network.DragonBreathVfxPayload;
import com.adminspec.network.DragonFlightInputPayload;
import com.adminspec.network.DragonFormPayload;
import com.adminspec.network.SpecStatePayload;
import com.adminspec.network.SwordEscapeBeamPayload;
import com.adminspec.network.TimeStopVfxPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid="adminspec", bus=EventBusSubscriber.Bus.MOD)
public final class ModPayloads {
    private ModPayloads() {
    }

    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar reg = event.registrar("7");
        reg.playToServer(ActivateMovePayload.TYPE, ActivateMovePayload.STREAM_CODEC, ActivateMovePayload::handle);
        reg.playToServer(DragonBreathPayload.TYPE, DragonBreathPayload.STREAM_CODEC, DragonBreathPayload::handle);
        reg.playToServer(DragonFlightInputPayload.TYPE, DragonFlightInputPayload.STREAM_CODEC, DragonFlightInputPayload::handle);
        reg.playToClient(SwordEscapeBeamPayload.TYPE, SwordEscapeBeamPayload.STREAM_CODEC, SwordEscapeBeamPayload::handle);
        reg.playToClient(SpecStatePayload.TYPE, SpecStatePayload.STREAM_CODEC, SpecStatePayload::handle);
        reg.playToClient(DragonFormPayload.TYPE, DragonFormPayload.STREAM_CODEC, DragonFormPayload::handle);
        reg.playToClient(DragonBreathVfxPayload.TYPE, DragonBreathVfxPayload.STREAM_CODEC, DragonBreathVfxPayload::handle);
        reg.playToClient(TimeStopVfxPayload.TYPE, TimeStopVfxPayload.STREAM_CODEC, TimeStopVfxPayload::handle);
    }
}

