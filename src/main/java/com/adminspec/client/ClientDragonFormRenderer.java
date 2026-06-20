package com.adminspec.client;

import com.adminspec.client.ClientSpecState;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

@EventBusSubscriber(modid = "adminspec", bus = EventBusSubscriber.Bus.GAME, value = {Dist.CLIENT})
public final class ClientDragonFormRenderer {

    private ClientDragonFormRenderer() {}

    public static class GeoModel {
        public static class Cube {
            public float[] origin;
            public float[] size;
            public float[] pivot;
            public float[] rotation;
            public int[] uv;
        }

        public static class Bone {
            public String name;
            public String parent;
            public float[] pivot;
            public float[] rotation;
            public List<Cube> cubes = new ArrayList<>();
        }

        public List<Bone> bones = new ArrayList<>();
    }

    private static GeoModel dragonModel = null;

    private static void loadModel() {
        if (dragonModel != null) return;
        try {
            ResourceLocation loc = ResourceLocation.fromNamespaceAndPath("adminspec", "models/entity/ancient_sword_dragon.geo.json");
            var resource = Minecraft.getInstance().getResourceManager().getResource(loc);
            if (resource.isPresent()) {
                try (InputStream stream = resource.get().open();
                     InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    JsonObject rootObj = JsonParser.parseReader(reader).getAsJsonObject();
                    JsonArray geoArray = rootObj.getAsJsonArray("minecraft:geometry");
                    if (geoArray != null && geoArray.size() > 0) {
                        JsonObject geo = geoArray.get(0).getAsJsonObject();
                        JsonArray bonesArray = geo.getAsJsonArray("bones");
                        GeoModel model = new GeoModel();
                        if (bonesArray != null) {
                            for (JsonElement bElem : bonesArray) {
                                JsonObject bObj = bElem.getAsJsonObject();
                                GeoModel.Bone bone = new GeoModel.Bone();
                                bone.name = bObj.has("name") ? bObj.get("name").getAsString() : "";
                                bone.parent = bObj.has("parent") ? bObj.get("parent").getAsString() : null;

                                if (bObj.has("pivot")) {
                                    JsonArray pArr = bObj.getAsJsonArray("pivot");
                                    bone.pivot = new float[]{pArr.get(0).getAsFloat(), pArr.get(1).getAsFloat(), pArr.get(2).getAsFloat()};
                                } else {
                                    bone.pivot = new float[]{0, 0, 0};
                                }

                                if (bObj.has("rotation")) {
                                    JsonArray rArr = bObj.getAsJsonArray("rotation");
                                    bone.rotation = new float[]{rArr.get(0).getAsFloat(), rArr.get(1).getAsFloat(), rArr.get(2).getAsFloat()};
                                } else {
                                    bone.rotation = new float[]{0, 0, 0};
                                }

                                if (bObj.has("cubes")) {
                                    JsonArray cArr = bObj.getAsJsonArray("cubes");
                                    for (JsonElement cElem : cArr) {
                                        JsonObject cObj = cElem.getAsJsonObject();
                                        GeoModel.Cube cube = new GeoModel.Cube();

                                        JsonArray oArr = cObj.getAsJsonArray("origin");
                                        cube.origin = new float[]{oArr.get(0).getAsFloat(), oArr.get(1).getAsFloat(), oArr.get(2).getAsFloat()};

                                        JsonArray sArr = cObj.getAsJsonArray("size");
                                        cube.size = new float[]{sArr.get(0).getAsFloat(), sArr.get(1).getAsFloat(), sArr.get(2).getAsFloat()};

                                        if (cObj.has("pivot")) {
                                            JsonArray cpArr = cObj.getAsJsonArray("pivot");
                                            cube.pivot = new float[]{cpArr.get(0).getAsFloat(), cpArr.get(1).getAsFloat(), cpArr.get(2).getAsFloat()};
                                        }

                                        if (cObj.has("rotation")) {
                                            JsonArray crArr = cObj.getAsJsonArray("rotation");
                                            cube.rotation = new float[]{crArr.get(0).getAsFloat(), crArr.get(1).getAsFloat(), crArr.get(2).getAsFloat()};
                                        }

                                        if (cObj.has("uv")) {
                                            JsonArray uArr = cObj.getAsJsonArray("uv");
                                            cube.uv = new int[]{uArr.get(0).getAsInt(), uArr.get(1).getAsInt()};
                                        } else {
                                            cube.uv = new int[]{0, 0};
                                        }

                                        bone.cubes.add(cube);
                                    }
                                }
                                model.bones.add(bone);
                            }
                        }
                        dragonModel = model;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        UUID uuid = player.getUUID();
        ClientSpecState.Snapshot snapshot = ClientSpecState.get(uuid);
        if (snapshot != null && snapshot.dragonFormActive) {
            int ticks = snapshot.dragonFormTicks;
            PoseStack pose = event.getPoseStack();

            loadModel();
            if (dragonModel == null) return;

            // Group bones by parent
            Map<String, List<GeoModel.Bone>> childrenMap = new HashMap<>();
            List<GeoModel.Bone> roots = new ArrayList<>();
            for (GeoModel.Bone bone : dragonModel.bones) {
                if (bone.parent == null || bone.parent.isEmpty() || bone.parent.equals("root")) {
                    roots.add(bone);
                } else {
                    childrenMap.computeIfAbsent(bone.parent, k -> new ArrayList<>()).add(bone);
                }
            }

            ResourceLocation texture = ResourceLocation.fromNamespaceAndPath("adminspec", "textures/entity/ancient_sword_dragon.png");
            VertexConsumer consumer = event.getMultiBufferSource().getBuffer(RenderType.entityCutoutNoCull(texture));

            if (ticks >= 60) {
                event.setCanceled(true);
                pose.pushPose();
                pose.translate(0.0, 0.75, 0.0);
                float yaw = player.getViewYRot(event.getPartialTick());
                float pitch = player.getViewXRot(event.getPartialTick());
                pose.mulPose(Axis.YP.rotationDegrees(-yaw - 180.0f));
                pose.mulPose(Axis.XP.rotationDegrees(pitch));
                pose.scale(0.85f, 0.85f, 0.85f);
                for (GeoModel.Bone root : roots) {
                    renderBone(pose, consumer, root, childrenMap, event.getPackedLight(), 1.0f, 1.0f, 1.0f, 1.0f, event.getPartialTick(), player);
                }
                pose.popPose();
            } else {
                // Progressive mutation attached to player model
                pose.pushPose();

                // 1. Head/Neck mutation (ticks 10-25)
                if (ticks >= 10) {
                    pose.pushPose();
                    pose.translate(0.0, 1.9, 0.0);
                    pose.scale(0.35f, 0.35f, 0.35f);
                    for (GeoModel.Bone root : roots) {
                        if (root.name.equals("neck")) {
                            renderBone(pose, consumer, root, childrenMap, event.getPackedLight(), 1.0f, 1.0f, 1.0f, 1.0f, event.getPartialTick(), player);
                        }
                    }
                    pose.popPose();
                }

                // 2. Body mutation (ticks 25-45)
                if (ticks >= 25) {
                    pose.pushPose();
                    pose.translate(0.0, 1.0, 0.15);
                    pose.scale(0.35f, 0.35f, 0.35f);
                    for (GeoModel.Bone root : roots) {
                        if (root.name.equals("front_body")) {
                            renderBone(pose, consumer, root, childrenMap, event.getPackedLight(), 1.0f, 1.0f, 1.0f, 1.0f, event.getPartialTick(), player);
                        }
                    }
                    pose.popPose();
                }

                pose.popPose();
            }
        }
    }

    private static void renderBone(PoseStack pose, VertexConsumer consumer, GeoModel.Bone bone, Map<String, List<GeoModel.Bone>> childrenMap, int light, float r, float g, float b, float a, float partialTick, Player player) {
        pose.pushPose();

        float px = bone.pivot[0] / 16.0f;
        float py = bone.pivot[1] / 16.0f;
        float pz = bone.pivot[2] / 16.0f;

        pose.translate(px, py, pz);

        float rx = bone.rotation[0];
        float ry = bone.rotation[1];
        float rz = bone.rotation[2];

        // Animated body, tail and hair flapping based on time
        if (bone.name.equals("tail") || bone.name.equals("tail_fin")) {
            double tailSwing = Math.sin((double)(player.level().getGameTime()) * 0.15) * 8.0;
            ry += (float) tailSwing;
        } else if (bone.name.contains("Hair") || bone.name.contains("Horn")) {
            double hairFlap = Math.sin((double)(player.level().getGameTime()) * 0.1) * 3.0;
            rz += (float) hairFlap;
        }

        if (rx != 0) pose.mulPose(Axis.XP.rotationDegrees(rx));
        if (ry != 0) pose.mulPose(Axis.YP.rotationDegrees(ry));
        if (rz != 0) pose.mulPose(Axis.ZP.rotationDegrees(rz));

        pose.translate(-px, -py, -pz);

        for (GeoModel.Cube cube : bone.cubes) {
            pose.pushPose();
            float ox = cube.origin[0] / 16.0f;
            float oy = cube.origin[1] / 16.0f;
            float oz = cube.origin[2] / 16.0f;
            float cx = cube.size[0] / 16.0f;
            float cy = cube.size[1] / 16.0f;
            float cz = cube.size[2] / 16.0f;

            if (cube.pivot != null) {
                float cpx = cube.pivot[0] / 16.0f;
                float cpy = cube.pivot[1] / 16.0f;
                float cpz = cube.pivot[2] / 16.0f;
                pose.translate(cpx, cpy, cpz);
                if (cube.rotation != null) {
                    if (cube.rotation[0] != 0) pose.mulPose(Axis.XP.rotationDegrees(cube.rotation[0]));
                    if (cube.rotation[1] != 0) pose.mulPose(Axis.YP.rotationDegrees(cube.rotation[1]));
                    if (cube.rotation[2] != 0) pose.mulPose(Axis.ZP.rotationDegrees(cube.rotation[2]));
                }
                pose.translate(-cpx, -cpy, -cpz);
            }

            drawBox(pose.last(), consumer, ox, oy, oz, cx, cy, cz, cube.uv[0], cube.uv[1], 256, 256, light, OverlayTexture.NO_OVERLAY, r, g, b, a);
            pose.popPose();
        }

        List<GeoModel.Bone> children = childrenMap.get(bone.name);
        if (children != null) {
            for (GeoModel.Bone child : children) {
                renderBone(pose, consumer, child, childrenMap, light, r, g, b, a, partialTick, player);
            }
        }

        pose.popPose();
    }

    private static void drawBox(PoseStack.Pose entry, VertexConsumer consumer, float x, float y, float z, float w, float h, float d, int u, int v, int texW, int texH, int light, int overlay, float red, float green, float blue, float alpha) {
        float x1 = x;
        float x2 = x + w;
        float y1 = y;
        float y2 = y + h;
        float z1 = z;
        float z2 = z + d;

        drawFace(entry, consumer, x1, y1, z1, x1, y2, z2, u, v + (int)d, d, h, texW, texH, -1.0f, 0.0f, 0.0f, light, overlay, red, green, blue, alpha);
        drawFace(entry, consumer, x2, y1, z2, x2, y2, z1, u + (int)d + (int)w, v + (int)d, d, h, texW, texH, 1.0f, 0.0f, 0.0f, light, overlay, red, green, blue, alpha);
        drawFace(entry, consumer, x1, y1, z2, x2, y1, z1, u + (int)d + (int)w, v, w, d, texW, texH, 0.0f, -1.0f, 0.0f, light, overlay, red, green, blue, alpha);
        drawFace(entry, consumer, x1, y2, z1, x2, y2, z2, u + (int)d, v, w, d, texW, texH, 0.0f, 1.0f, 0.0f, light, overlay, red, green, blue, alpha);
        drawFace(entry, consumer, x2, y1, z1, x1, y2, z1, u + (int)d, v + (int)d, w, h, texW, texH, 0.0f, 0.0f, -1.0f, light, overlay, red, green, blue, alpha);
        drawFace(entry, consumer, x1, y1, z2, x2, y2, z2, u + 2 * (int)d + (int)w, v + (int)d, w, h, texW, texH, 0.0f, 0.0f, 1.0f, light, overlay, red, green, blue, alpha);
    }

    private static void drawFace(PoseStack.Pose entry, VertexConsumer consumer, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, int u, int v, float w, float h, int texW, int texH, float nx, float ny, float nz, int light, int overlay, float r, float g, float b, float a) {
        float minU = (float)u / (float)texW;
        float minV = (float)v / (float)texH;
        float maxU = (float)(u + w) / (float)texW;
        float maxV = (float)(v + h) / (float)texH;

        vertex(entry, consumer, minX, minY, minZ, minU, maxV, nx, ny, nz, light, overlay, r, g, b, a);
        vertex(entry, consumer, maxX, minY, maxZ, maxU, maxV, nx, ny, nz, light, overlay, r, g, b, a);
        vertex(entry, consumer, maxX, maxY, maxZ, maxU, minV, nx, ny, nz, light, overlay, r, g, b, a);
        vertex(entry, consumer, minX, maxY, minZ, minU, minV, nx, ny, nz, light, overlay, r, g, b, a);
    }

    private static void vertex(PoseStack.Pose entry, VertexConsumer consumer, float x, float y, float z, float u, float v, float nx, float ny, float nz, int light, int overlay, float r, float g, float b, float a) {
        org.joml.Vector4f posVec = new org.joml.Vector4f(x, y, z, 1.0f);
        posVec.mul(entry.pose());

        org.joml.Vector3f normVec = new org.joml.Vector3f(nx, ny, nz);
        normVec.mul(entry.normal());

        consumer.addVertex(posVec.x(), posVec.y(), posVec.z())
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(normVec.x(), normVec.y(), normVec.z());
    }
}
