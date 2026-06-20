package com.adminspec.client;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EventBusSubscriber(modid = "adminspec", bus = EventBusSubscriber.Bus.GAME, value = {Dist.CLIENT})
public final class ClientDragonFormRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger("adminspec-dragon");

    private ClientDragonFormRenderer() {}

    // Simple geo model representation
    public static class Cube {
        public float[] origin;   // [x, y, z] in pixels
        public float[] size;     // [w, h, d] in pixels
        public float[] pivot;    // optional per-cube pivot in pixels
        public float[] rotation; // optional per-cube rotation in degrees
        public int[] uv;         // [u, v] offset
    }

    public static class Bone {
        public String name;
        public String parent;
        public float[] pivot;    // pivot in pixels
        public float[] rotation; // rotation in degrees
        public List<Cube> cubes = new ArrayList<>();
    }

    public static class GeoModel {
        public List<Bone> bones = new ArrayList<>();
        public int texW = 256;
        public int texH = 256;
    }

    private static GeoModel dragonModel = null;
    private static boolean loadAttempted = false;

    private static void loadModel() {
        if (loadAttempted) return;
        loadAttempted = true;
        try {
            ResourceLocation loc = ResourceLocation.fromNamespaceAndPath("adminspec",
                "models/entity/ancient_sword_dragon.geo.json");
            var resource = Minecraft.getInstance().getResourceManager().getResource(loc);
            if (resource.isEmpty()) {
                LOGGER.error("[AdminSpec] Dragon geo.json not found at: {}", loc);
                return;
            }
            try (InputStream stream = resource.get().open();
                 InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {

                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                JsonArray geoArray = root.getAsJsonArray("minecraft:geometry");
                if (geoArray == null || geoArray.isEmpty()) {
                    LOGGER.error("[AdminSpec] 'minecraft:geometry' array missing in dragon geo.json");
                    return;
                }
                JsonObject geo = geoArray.get(0).getAsJsonObject();
                JsonObject desc = geo.has("description") ? geo.getAsJsonObject("description") : null;

                GeoModel model = new GeoModel();
                if (desc != null) {
                    if (desc.has("texture_width"))  model.texW = desc.get("texture_width").getAsInt();
                    if (desc.has("texture_height")) model.texH = desc.get("texture_height").getAsInt();
                }

                JsonArray bonesArray = geo.getAsJsonArray("bones");
                if (bonesArray == null) {
                    LOGGER.error("[AdminSpec] No 'bones' in dragon geo.json");
                    return;
                }
                for (JsonElement bElem : bonesArray) {
                    JsonObject bObj = bElem.getAsJsonObject();
                    Bone bone = new Bone();
                    bone.name   = bObj.has("name")   ? bObj.get("name").getAsString()   : "";
                    bone.parent = bObj.has("parent") ? bObj.get("parent").getAsString() : null;
                    bone.pivot    = parseVec3(bObj, "pivot",    0, 0, 0);
                    bone.rotation = parseVec3(bObj, "rotation", 0, 0, 0);

                    if (bObj.has("cubes")) {
                        for (JsonElement cElem : bObj.getAsJsonArray("cubes")) {
                            JsonObject cObj = cElem.getAsJsonObject();
                            Cube cube = new Cube();
                            cube.origin   = parseVec3(cObj, "origin", 0, 0, 0);
                            cube.size     = parseVec3(cObj, "size",   1, 1, 1);
                            cube.pivot    = cObj.has("pivot")    ? parseVec3(cObj, "pivot",    0, 0, 0) : null;
                            cube.rotation = cObj.has("rotation") ? parseVec3(cObj, "rotation", 0, 0, 0) : null;
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
                dragonModel = model;
                LOGGER.info("[AdminSpec] Dragon geo.json loaded: {} bones, tex={}x{}", model.bones.size(), model.texW, model.texH);
            }
        } catch (Exception e) {
            LOGGER.error("[AdminSpec] Failed to load dragon geo.json", e);
        }
    }

    private static float[] parseVec3(JsonObject obj, String key, float dx, float dy, float dz) {
        if (!obj.has(key)) return new float[]{dx, dy, dz};
        JsonArray arr = obj.getAsJsonArray(key);
        return new float[]{arr.get(0).getAsFloat(), arr.get(1).getAsFloat(), arr.get(2).getAsFloat()};
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        UUID uuid = player.getUUID();
        ClientSpecState.Snapshot snapshot = ClientSpecState.get(uuid);
        if (snapshot == null || !snapshot.dragonFormActive) return;

        loadModel();
        if (dragonModel == null) return;

        int ticks = snapshot.dragonFormTicks;
        PoseStack pose = event.getPoseStack();
        MultiBufferSource bufSource = event.getMultiBufferSource();

        // Build bone hierarchy
        Map<String, List<Bone>> childrenMap = new HashMap<>();
        List<Bone> rootBones = new ArrayList<>();
        for (Bone bone : dragonModel.bones) {
            if (bone.parent == null || bone.parent.isEmpty() || bone.parent.equals("root")) {
                // "root" bone itself has no cubes — but its children (front_body etc.) do
                if (!bone.name.equals("root")) {
                    rootBones.add(bone);
                }
            } else {
                childrenMap.computeIfAbsent(bone.parent, k -> new ArrayList<>()).add(bone);
            }
        }

        // Also add children of "root" explicitly since we skip the "root" bone
        // The geo has: root → front_body → middle_body → ...
        // front_body.parent = "root" so it's already in rootBones above

        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath("adminspec",
            "textures/entity/ancient_sword_dragon.png");
        VertexConsumer consumer = bufSource.getBuffer(RenderType.entityCutoutNoCull(texture));
        int light = event.getPackedLight();
        float pt = event.getPartialTick();

        if (ticks >= 60) {
            // Full dragon: hide player, show full dragon model
            event.setCanceled(true);
            pose.pushPose();

            // Center on player standing position, scale to reasonable size
            float yaw = player.getViewYRot(pt);
            pose.translate(0.0, 0.0, 0.0);
            pose.mulPose(Axis.YP.rotationDegrees(-yaw - 180f));
            // Bedrock model coordinates: 1 pixel = 1/16 block
            // The dragon body is ~100px wide; we want ~5 blocks → scale = 5/6.25 ≈ 0.8 /16
            float modelScale = 1f / 16f;
            pose.scale(modelScale, modelScale, modelScale);

            for (Bone root : rootBones) {
                renderBone(pose, consumer, root, childrenMap, light, 1f, 1f, 1f, 1f, pt, player);
            }
            pose.popPose();
        } else {
            // Progressive mutation (0-60 ticks): don't cancel player render, overlay dragon parts.
            // The actual geo skeleton is: root -> neck -> {upper_head, lower_head},
            // and root -> front_body -> middle_body -> rear_body -> tail -> tail_fin.
            // There are NO wing bones, so we phase the mutation as head / front-body / rear+tail.
            // Bone pivots in Bedrock geo are model-space absolute, and renderBoneStandalone
            // applies a net-zero pivot transform, so any subset of bones renders in place.
            pose.pushPose();
            float yaw = player.getViewYRot(pt);
            pose.mulPose(Axis.YP.rotationDegrees(-yaw - 180f));
            float modelScale = 1f / 16f;
            pose.scale(modelScale, modelScale, modelScale);

            float alpha = Math.min(1.0f, ticks / 30f); // fade in

            for (Bone b : dragonModel.bones) {
                if (isActiveInPhase(b.name, ticks)) {
                    renderBoneStandalone(pose, consumer, b, light, alpha, pt, player);
                }
            }
            pose.popPose();
        }
    }

    /**
     * Phase gating for the 0-60 tick mutation sequence, mapped to the real bones:
     * ticks >= 5  -> head emerges (neck + upper/lower head)
     * ticks >= 20 -> front + middle body grow
     * ticks >= 40 -> rear body + tail + tail_fin complete the dragon
     */
    private static boolean isActiveInPhase(String name, int ticks) {
        if (ticks >= 5 && (name.equals("neck") || name.equals("upper_head") || name.equals("lower_head"))) {
            return true;
        }
        if (ticks >= 20 && (name.equals("front_body") || name.equals("middle_body"))) {
            return true;
        }
        if (ticks >= 40 && (name.equals("rear_body") || name.equals("tail") || name.equals("tail_fin"))) {
            return true;
        }
        return false;
    }

    /**
     * Draws a single bone's cubes (with its own pivot/rotation and per-bone animation)
     * WITHOUT recursing into children. Used during the phased mutation so each phase
     * can render an exact subset of bones (e.g. head without pulling in the whole body).
     */
    private static void renderBoneStandalone(
        PoseStack pose, VertexConsumer consumer, Bone bone,
        int light, float a, float partialTick, Player player
    ) {
        pose.pushPose();

        float px = bone.pivot[0];
        float py = bone.pivot[1];
        float pz = bone.pivot[2];
        pose.translate(px, py, pz);

        float rx = bone.rotation[0];
        float ry = bone.rotation[1];
        float rz = bone.rotation[2];

        long time = player.level().getGameTime();
        if (bone.name.contains("tail")) {
            ry += (float)(Math.sin(time * 0.12) * 12.0);
        } else if (bone.name.contains("wing") || bone.name.contains("Wing")) {
            rz += (float)(Math.sin(time * 0.2) * 20.0);
        } else if (bone.name.contains("Hair") || bone.name.contains("Horn")) {
            rz += (float)(Math.sin(time * 0.1) * 4.0);
        }

        if (rx != 0) pose.mulPose(Axis.XP.rotationDegrees(rx));
        if (ry != 0) pose.mulPose(Axis.YP.rotationDegrees(ry));
        if (rz != 0) pose.mulPose(Axis.ZP.rotationDegrees(rz));

        pose.translate(-px, -py, -pz);

        for (Cube cube : bone.cubes) {
            pose.pushPose();
            if (cube.pivot != null) {
                pose.translate(cube.pivot[0], cube.pivot[1], cube.pivot[2]);
                if (cube.rotation != null) {
                    if (cube.rotation[0] != 0) pose.mulPose(Axis.XP.rotationDegrees(cube.rotation[0]));
                    if (cube.rotation[1] != 0) pose.mulPose(Axis.YP.rotationDegrees(cube.rotation[1]));
                    if (cube.rotation[2] != 0) pose.mulPose(Axis.ZP.rotationDegrees(cube.rotation[2]));
                }
                pose.translate(-cube.pivot[0], -cube.pivot[1], -cube.pivot[2]);
            }
            drawBox(pose.last(), consumer,
                cube.origin[0], cube.origin[1], cube.origin[2],
                cube.size[0], cube.size[1], cube.size[2],
                cube.uv[0], cube.uv[1],
                dragonModel.texW, dragonModel.texH,
                light, OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, a);
            pose.popPose();
        }

        pose.popPose();
    }

    private static void renderBone(
        PoseStack pose, VertexConsumer consumer, Bone bone,
        Map<String, List<Bone>> childrenMap,
        int light, float r, float g, float b, float a,
        float partialTick, Player player
    ) {
        pose.pushPose();

        // Translate to bone pivot, apply rotation, translate back
        float px = bone.pivot[0];
        float py = bone.pivot[1];
        float pz = bone.pivot[2];
        pose.translate(px, py, pz);

        float rx = bone.rotation[0];
        float ry = bone.rotation[1];
        float rz = bone.rotation[2];

        // Animate certain bones
        long time = player.level().getGameTime();
        if (bone.name.contains("tail")) {
            ry += (float)(Math.sin(time * 0.12) * 12.0);
        } else if (bone.name.contains("wing") || bone.name.contains("Wing")) {
            rz += (float)(Math.sin(time * 0.2) * 20.0);
        } else if (bone.name.contains("Hair") || bone.name.contains("Horn")) {
            rz += (float)(Math.sin(time * 0.1) * 4.0);
        }

        if (rx != 0) pose.mulPose(Axis.XP.rotationDegrees(rx));
        if (ry != 0) pose.mulPose(Axis.YP.rotationDegrees(ry));
        if (rz != 0) pose.mulPose(Axis.ZP.rotationDegrees(rz));

        pose.translate(-px, -py, -pz);

        // Draw cubes
        for (Cube cube : bone.cubes) {
            pose.pushPose();
            if (cube.pivot != null) {
                pose.translate(cube.pivot[0], cube.pivot[1], cube.pivot[2]);
                if (cube.rotation != null) {
                    if (cube.rotation[0] != 0) pose.mulPose(Axis.XP.rotationDegrees(cube.rotation[0]));
                    if (cube.rotation[1] != 0) pose.mulPose(Axis.YP.rotationDegrees(cube.rotation[1]));
                    if (cube.rotation[2] != 0) pose.mulPose(Axis.ZP.rotationDegrees(cube.rotation[2]));
                }
                pose.translate(-cube.pivot[0], -cube.pivot[1], -cube.pivot[2]);
            }
            drawBox(pose.last(), consumer,
                cube.origin[0], cube.origin[1], cube.origin[2],
                cube.size[0], cube.size[1], cube.size[2],
                cube.uv[0], cube.uv[1],
                dragonModel.texW, dragonModel.texH,
                light, OverlayTexture.NO_OVERLAY, r, g, b, a);
            pose.popPose();
        }

        // Recurse into children
        List<Bone> children = childrenMap.get(bone.name);
        if (children != null) {
            for (Bone child : children) {
                renderBone(pose, consumer, child, childrenMap, light, r, g, b, a, partialTick, player);
            }
        }

        pose.popPose();
    }

    /**
     * Draws a Bedrock-style box. Coordinates are in pixel units (not divided by 16 yet —
     * the overall modelScale (1/16) is already applied via the PoseStack).
     */
    private static void drawBox(
        PoseStack.Pose entry, VertexConsumer consumer,
        float x, float y, float z,
        float w, float h, float d,
        int u, int v, int texW, int texH,
        int light, int overlay, float red, float green, float blue, float alpha
    ) {
        float x2 = x + w, y2 = y + h, z2 = z + d;
        // Minecraft standard face layout for box:
        // West face   (-X)
        face(entry,consumer, x, y,z, x,y2,z2,  u,         v+(int)d,       (int)d,(int)h, texW,texH, -1,0,0, light,overlay,red,green,blue,alpha);
        // East face   (+X)
        face(entry,consumer, x2,y,z2, x2,y2,z, u+(int)d+(int)w, v+(int)d, (int)d,(int)h, texW,texH,  1,0,0, light,overlay,red,green,blue,alpha);
        // Bottom face (-Y)
        face(entry,consumer, x,y,z2, x2,y,z,  u+(int)d+(int)w,v,          (int)w,(int)d, texW,texH,  0,-1,0, light,overlay,red,green,blue,alpha);
        // Top face    (+Y)
        face(entry,consumer, x,y2,z, x2,y2,z2, u+(int)d,       v,          (int)w,(int)d, texW,texH,  0, 1,0, light,overlay,red,green,blue,alpha);
        // North face  (-Z)
        face(entry,consumer, x2,y,z, x,y2,z,  u+(int)d,       v+(int)d,   (int)w,(int)h, texW,texH,  0,0,-1, light,overlay,red,green,blue,alpha);
        // South face  (+Z)
        face(entry,consumer, x,y,z2, x2,y2,z2, u+2*(int)d+(int)w,v+(int)d,(int)w,(int)h, texW,texH,  0,0, 1, light,overlay,red,green,blue,alpha);
    }

    private static void face(
        PoseStack.Pose entry, VertexConsumer consumer,
        float minX, float minY, float minZ,
        float maxX, float maxY, float maxZ,
        int u, int v, int fw, int fh,
        int texW, int texH,
        float nx, float ny, float nz,
        int light, int overlay,
        float r, float g, float b, float a
    ) {
        float u0 = (float)u / texW, u1 = (float)(u + fw) / texW;
        float v0 = (float)v / texH, v1 = (float)(v + fh) / texH;
        vert(entry,consumer, minX,minY,minZ, u0,v1, nx,ny,nz, light,overlay,r,g,b,a);
        vert(entry,consumer, maxX,minY,maxZ, u1,v1, nx,ny,nz, light,overlay,r,g,b,a);
        vert(entry,consumer, maxX,maxY,maxZ, u1,v0, nx,ny,nz, light,overlay,r,g,b,a);
        vert(entry,consumer, minX,maxY,minZ, u0,v0, nx,ny,nz, light,overlay,r,g,b,a);
    }

    private static void vert(
        PoseStack.Pose entry, VertexConsumer consumer,
        float x, float y, float z,
        float u, float v,
        float nx, float ny, float nz,
        int light, int overlay,
        float r, float g, float b, float a
    ) {
        org.joml.Vector4f pos = new org.joml.Vector4f(x, y, z, 1f);
        pos.mul(entry.pose());
        org.joml.Vector3f norm = new org.joml.Vector3f(nx, ny, nz);
        norm.mul(entry.normal());

        consumer.addVertex(pos.x(), pos.y(), pos.z())
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(norm.x(), norm.y(), norm.z());
    }
}
