/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.DeltaTracker
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.LayeredDraw$Layer
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.resources.ResourceLocation
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.bus.api.SubscribeEvent
 *  net.neoforged.fml.common.EventBusSubscriber
 *  net.neoforged.fml.common.EventBusSubscriber$Bus
 *  net.neoforged.neoforge.client.event.RegisterGuiLayersEvent
 */
package com.adminspec.client;

import com.adminspec.client.ClientSpecState;
import java.util.Objects;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

@EventBusSubscriber(modid="adminspec", bus=EventBusSubscriber.Bus.MOD, value={Dist.CLIENT})
public final class ReverseFlowHud
implements LayeredDraw.Layer {
    private static final ResourceLocation LAYER_ID = ResourceLocation.fromNamespaceAndPath((String)"adminspec", (String)"reverse_flow_bar");
    private static final int BAR_WIDTH = 120;
    private static final int BAR_HEIGHT = 8;
    private static final int BAR_Y_OFFSET = 60;
    private static final int COLOR_BG = Integer.MIN_VALUE;
    private static final int COLOR_BORDER = -14671840;
    private static final int COLOR_BAR_FULL = -12549889;
    private static final int COLOR_BAR_LOW = -49088;
    private static final int COLOR_TEXT = -1;

    private ReverseFlowHud() {
    }

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(LAYER_ID, (LayeredDraw.Layer)new ReverseFlowHud());
    }

    public void render(GuiGraphics gfx, DeltaTracker delta) {
        if (ClientSpecState.clientFlashTicks > 0) {
            float alpha = (float)ClientSpecState.clientFlashTicks / 15.0f;
            int a = (int)(alpha * 255.0f);
            int color = (a << 24) | 0xFFFFFF;
            gfx.fill(0, 0, gfx.guiWidth(), gfx.guiHeight(), color);
        }
        int fillColor;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
        ClientSpecState.Snapshot snap = ClientSpecState.get(player.getUUID());
        if (snap == null || !snap.reverseFlowActive) {
            return;
        }
        float capacity = snap.reverseFlowCapacity;
        int screenWidth = gfx.guiWidth();
        int screenHeight = gfx.guiHeight();
        int barX = (screenWidth - 120) / 2;
        int barY = screenHeight - 60;
        gfx.fill(barX - 1, barY - 1, barX + 120 + 1, barY + 8 + 1, -14671840);
        gfx.fill(barX, barY, barX + 120, barY + 8, Integer.MIN_VALUE);
        int fillWidth = (int)(120.0f * Math.max(0.0f, Math.min(1.0f, capacity)));
        int n = fillColor = capacity < 0.2f ? -49088 : -12549889;
        if (fillWidth > 0) {
            gfx.fill(barX, barY, barX + fillWidth, barY + 8, fillColor);
        }
        Font font = mc.font;
        String label = "Reverse Flow River";
        int labelX = (screenWidth - font.width(label)) / 2;
        Objects.requireNonNull(font);
        int labelY = barY - 9 - 2;
        gfx.drawString(font, label, labelX, labelY, -1, false);
        String pct = (int)(capacity * 100.0f) + "%";
        int pctX = (screenWidth - font.width(pct)) / 2;
        Objects.requireNonNull(font);
        int pctY = barY + (8 - 9) / 2 + 1;
        gfx.drawString(font, pct, pctX + 1, pctY + 1, -16777216, false);
        gfx.drawString(font, pct, pctX, pctY, -1, false);
    }
}

