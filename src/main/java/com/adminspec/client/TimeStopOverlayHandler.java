package com.adminspec.client;

import com.adminspec.network.TimeStopVfxPayload;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = "adminspec", bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class TimeStopOverlayHandler {
    private static final ConcurrentHashMap<UUID, Long> ACTIVE = new ConcurrentHashMap<>();
    private static final long OVERLAY_FADE_MS = 500;
    private static final java.util.Random RANDOM = new java.util.Random();

    private TimeStopOverlayHandler() {}

    public static void update(TimeStopVfxPayload payload) {
        if (payload.active()) {
            ACTIVE.put(payload.playerId(), System.currentTimeMillis());
        } else {
            ACTIVE.remove(payload.playerId());
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        UUID myUuid = mc.player.getUUID();

        Long startTime = ACTIVE.get(myUuid);
        if (startTime == null) return;

        long elapsed = System.currentTimeMillis() - startTime;
        float alpha;
        if (elapsed < OVERLAY_FADE_MS) {
            alpha = (float) elapsed / OVERLAY_FADE_MS;
        } else {
            alpha = 1f;
        }

        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();

        PoseStack pose = event.getGuiGraphics().pose();
        pose.pushPose();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder buf = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        buf.addVertex(0, h, 0).setColor(0.1f, 0.1f, 0.1f, alpha * 0.55f);
        buf.addVertex(w, h, 0).setColor(0.05f, 0.05f, 0.1f, alpha * 0.6f);
        buf.addVertex(w, 0, 0).setColor(0.05f, 0.05f, 0.1f, alpha * 0.6f);
        buf.addVertex(0, 0, 0).setColor(0.1f, 0.1f, 0.1f, alpha * 0.55f);

        BufferUploader.drawWithShader(buf.buildOrThrow());

        // Vignette-like border darkening
        float vignette = alpha * 0.35f;
        if (vignette > 0.01f) {
            BufferBuilder vig = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            // Top bar
            vig.addVertex(0, 60, 0).setColor(0, 0, 0, vignette);
            vig.addVertex(w, 60, 0).setColor(0, 0, 0, vignette);
            vig.addVertex(w, 0, 0).setColor(0, 0, 0, vignette * 0.5f);
            vig.addVertex(0, 0, 0).setColor(0, 0, 0, vignette * 0.5f);
            // Bottom bar
            vig.addVertex(0, h, 0).setColor(0, 0, 0, vignette);
            vig.addVertex(w, h, 0).setColor(0, 0, 0, vignette);
            vig.addVertex(w, h - 60, 0).setColor(0, 0, 0, vignette * 0.5f);
            vig.addVertex(0, h - 60, 0).setColor(0, 0, 0, vignette * 0.5f);
            BufferUploader.drawWithShader(vig.buildOrThrow());
        }

        RenderSystem.disableBlend();
        pose.popPose();
    }
}
