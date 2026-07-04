package com.wjx.kablade.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.wjx.kablade.Main;
import com.wjx.kablade.client.KabladeRenderTypes;
import com.wjx.kablade.entity.RainUmbrellaEntity;
import mods.flammpfeil.slashblade.client.renderer.model.BladeModelManager;
import mods.flammpfeil.slashblade.client.renderer.model.obj.WavefrontObject;
import mods.flammpfeil.slashblade.client.renderer.util.BladeRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Renderer for the Love is War umbrella field. */
public class RainUmbrellaRenderer extends EntityRenderer<RainUmbrellaEntity> {

    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(Main.MODID, "effects/rain/umbrella.obj");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Main.MODID, "effects/rain/umb.png");
    private static final ResourceLocation PETAL_MODEL =
            ResourceLocation.fromNamespaceAndPath(Main.MODID, "effects/rain/petal.obj");
    private static final ResourceLocation[] PETAL_TEXTURES = {
            ResourceLocation.fromNamespaceAndPath(Main.MODID, "effects/rain/1.png"),
            ResourceLocation.fromNamespaceAndPath(Main.MODID, "effects/rain/2.png"),
            ResourceLocation.fromNamespaceAndPath(Main.MODID, "effects/rain/3.png")
    };
    private static final ResourceLocation EMPTY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Main.MODID, "textures/entity/empty.png");
    private static final int SEGMENTS = 64;
    private static final float RING_RADIUS = 4.0F;
    private static final float RING_THICKNESS = 0.05F;
    private static final float RING_Y_OFFSET = 0.08F;
    private static final int RING_START_TICK = 70;
    private static final int RING_DURATION = 20;
    private static final int PETAL_COUNT = 48;
    private static final int PETAL_MAX_AGE = 80;
    private static final int PETAL_FADE_AGE = 35;
    private static final int PETAL_LIGHT = 0xF000F0;
    private static final float PETAL_BLOOM_SCALE = 1.65F;
    private static PetalMesh petalMesh;
    private static final Map<Integer, PetalCloud> PETAL_CLOUDS = new HashMap<>();

    public RainUmbrellaRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(RainUmbrellaEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = entity.tickCount + partialTick;
        renderUmbrella(age, poseStack, buffer, packedLight);
        renderPetals(entity, partialTick, poseStack, buffer, packedLight);
        renderRing(age, poseStack, buffer);
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private static void renderUmbrella(float age, PoseStack poseStack,
                                       MultiBufferSource buffer, int packedLight) {
        float scale;
        if (age <= 20.0F) {
            scale = (float) Math.sin(age / 40.0D * Math.PI) * 140.0F;
        } else if (age > 70.0F && age <= 75.0F) {
            scale = (float) Math.sin((75.0F - age) / 40.0D * Math.PI) * 140.0F;
        } else if (age > 75.0F) {
            return;
        } else {
            scale = 140.0F;
        }
        if (scale <= 0.001F) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.0D, 1.0D, 0.0D);
        poseStack.scale(scale, scale, scale);
        poseStack.mulPose(Axis.YP.rotationDegrees(age * 10.0F));

        BladeRenderState.setCol(0xFFFFFFFF);
        WavefrontObject model = BladeModelManager.getInstance().getModel(MODEL);
        BladeRenderState.renderOverridedLuminous(
                ItemStack.EMPTY, model, "Katana_M27_L_Open_Model", TEXTURE, poseStack, buffer, packedLight);
        BladeRenderState.renderOverridedLuminous(
                ItemStack.EMPTY, model, "Katana_M27_L_Open_Model_2", TEXTURE, poseStack, buffer, packedLight);
        BladeRenderState.renderOverridedLuminous(
                ItemStack.EMPTY, model, "Katana_M27_L_Open_Model_3", TEXTURE, poseStack, buffer, packedLight);
        poseStack.popPose();
    }

    private static void renderPetals(RainUmbrellaEntity entity, float partialTick, PoseStack poseStack,
                                     MultiBufferSource buffer, int packedLight) {
        PetalMesh model = getPetalMesh();
        if (model.faces.isEmpty()) {
            return;
        }
        PetalCloud cloud = PETAL_CLOUDS.computeIfAbsent(entity.getId(), id -> new PetalCloud(id));
        if (cloud.lastTick > entity.tickCount) {
            cloud = new PetalCloud(entity.getId());
            PETAL_CLOUDS.put(entity.getId(), cloud);
        }
        cloud.tickTo(entity.tickCount);
        if (cloud.petals.isEmpty()) {
            if (entity.tickCount > 75 + PETAL_MAX_AGE) {
                PETAL_CLOUDS.remove(entity.getId());
            }
            return;
        }

        for (Petal petal : cloud.petals) {
            poseStack.pushPose();
            poseStack.translate(
                    Mth.lerp(partialTick, petal.prevX, petal.x),
                    Mth.lerp(partialTick, petal.prevY, petal.y),
                    Mth.lerp(partialTick, petal.prevZ, petal.z));
            poseStack.mulPose(Axis.YP.rotationDegrees(petal.rotationY));
            poseStack.mulPose(Axis.XP.rotationDegrees(petal.rotationX));
            poseStack.mulPose(Axis.ZP.rotationDegrees(petal.rotationZ));
            float renderScale = petal.renderScale();
            poseStack.scale(renderScale, renderScale, renderScale);

            ResourceLocation texture = PETAL_TEXTURES[petal.texture];
            model.render(poseStack, buffer, KabladeRenderTypes.rainPetal(texture), petal.alpha(), PETAL_LIGHT);
            poseStack.scale(PETAL_BLOOM_SCALE, PETAL_BLOOM_SCALE, PETAL_BLOOM_SCALE);
            model.render(poseStack, buffer, KabladeRenderTypes.rainPetalBloom(texture), petal.bloomAlpha(), PETAL_LIGHT);
            poseStack.popPose();
        }
    }

    private static PetalMesh getPetalMesh() {
        if (petalMesh == null) {
            petalMesh = PetalMesh.load(PETAL_MODEL);
        }
        return petalMesh;
    }

    private static void renderRing(float age, PoseStack poseStack, MultiBufferSource buffer) {
        if (age <= RING_START_TICK) {
            return;
        }
        float progress = Mth.clamp((age - RING_START_TICK) / RING_DURATION, 0.0F, 1.0F);
        if (progress >= 1.0F) {
            return;
        }

        float radius = RING_RADIUS * (float) Math.sin(progress * Math.PI / 2.0F);
        float alpha = 1.0F - progress;
        float r = 1.0F - progress * 0.3F;
        float g = 1.0F - progress * 0.1F;
        float b = 1.0F;

        poseStack.pushPose();
        poseStack.translate(0.0D, RING_Y_OFFSET, 0.0D);
        poseStack.scale(5.0F, 5.0F, 5.0F);
        Matrix4f mat = poseStack.last().pose();
        VertexConsumer vc = buffer.getBuffer(KabladeRenderTypes.rainEndingRing());

        float inner = radius - RING_THICKNESS * 0.5F;
        float outer = radius + RING_THICKNESS * 0.5F;
        for (int i = 0; i < SEGMENTS; i++) {
            float a0 = (float) (Math.PI * 2.0D * i / SEGMENTS);
            float a1 = (float) (Math.PI * 2.0D * (i + 1) / SEGMENTS);
            quad(vc, mat,
                    Mth.cos(a0) * outer, Mth.sin(a0) * outer,
                    Mth.cos(a1) * outer, Mth.sin(a1) * outer,
                    Mth.cos(a1) * inner, Mth.sin(a1) * inner,
                    Mth.cos(a0) * inner, Mth.sin(a0) * inner,
                    r, g, b, alpha);
        }
        poseStack.popPose();
    }

    private static void quad(VertexConsumer vc, Matrix4f mat,
                             float x0, float z0, float x1, float z1,
                             float x2, float z2, float x3, float z3,
                             float r, float g, float b, float a) {
        vertex(vc, mat, x0, z0, r, g, b, a);
        vertex(vc, mat, x1, z1, r, g, b, a);
        vertex(vc, mat, x2, z2, r, g, b, a);
        vertex(vc, mat, x3, z3, r, g, b, a);
    }

    private static void vertex(VertexConsumer vc, Matrix4f mat, float x, float z,
                               float r, float g, float b, float a) {
        vc.vertex(mat, x, 0.0F, z).color(r, g, b, a).endVertex();
    }

    private static final class PetalMesh {
        private static final PetalMesh EMPTY = new PetalMesh(List.of());

        private final List<PetalVertex[]> faces;

        private PetalMesh(List<PetalVertex[]> faces) {
            this.faces = faces;
        }

        private static PetalMesh load(ResourceLocation location) {
            List<float[]> positions = new ArrayList<>();
            List<float[]> uvs = new ArrayList<>();
            List<PetalVertex[]> faces = new ArrayList<>();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    Minecraft.getInstance().getResourceManager().open(location), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("v ")) {
                        String[] parts = line.split("\\s+");
                        positions.add(new float[] {
                                Float.parseFloat(parts[1]),
                                Float.parseFloat(parts[2]),
                                Float.parseFloat(parts[3])
                        });
                    } else if (line.startsWith("vt ")) {
                        String[] parts = line.split("\\s+");
                        uvs.add(new float[] {
                                Float.parseFloat(parts[1]),
                                1.0F - Float.parseFloat(parts[2])
                        });
                    } else if (line.startsWith("f ")) {
                        String[] parts = line.substring(2).trim().split("\\s+");
                        if (parts.length >= 3) {
                            PetalVertex[] face = new PetalVertex[parts.length];
                            for (int i = 0; i < parts.length; i++) {
                                face[i] = parseVertex(parts[i], positions, uvs);
                            }
                            faces.add(face);
                        }
                    }
                }
            } catch (Exception ignored) {
                return EMPTY;
            }

            return new PetalMesh(faces);
        }

        private static PetalVertex parseVertex(String token, List<float[]> positions, List<float[]> uvs) {
            String[] parts = token.split("/");
            float[] position = positions.get(resolveIndex(parts[0], positions.size()));
            float[] uv = parts.length > 1 && !parts[1].isEmpty()
                    ? uvs.get(resolveIndex(parts[1], uvs.size()))
                    : new float[] {0.0F, 0.0F};
            return new PetalVertex(position[0], position[1], position[2], uv[0], uv[1]);
        }

        private static int resolveIndex(String raw, int size) {
            int index = Integer.parseInt(raw);
            return index < 0 ? size + index : index - 1;
        }

        private void render(PoseStack poseStack, MultiBufferSource buffer,
                            RenderType renderType, int alpha, int packedLight) {
            PoseStack.Pose pose = poseStack.last();
            Matrix4f mat = pose.pose();
            Matrix3f normal = pose.normal();
            VertexConsumer vc = buffer.getBuffer(renderType);
            for (PetalVertex[] face : faces) {
                if (face.length == 4) {
                    emit(vc, mat, normal, face[0], alpha, packedLight);
                    emit(vc, mat, normal, face[1], alpha, packedLight);
                    emit(vc, mat, normal, face[2], alpha, packedLight);
                    emit(vc, mat, normal, face[3], alpha, packedLight);
                } else if (face.length == 3) {
                    emit(vc, mat, normal, face[0], alpha, packedLight);
                    emit(vc, mat, normal, face[1], alpha, packedLight);
                    emit(vc, mat, normal, face[2], alpha, packedLight);
                    emit(vc, mat, normal, face[2], alpha, packedLight);
                } else if (face.length > 4) {
                    for (int i = 1; i < face.length - 2; i++) {
                        emit(vc, mat, normal, face[0], alpha, packedLight);
                        emit(vc, mat, normal, face[i], alpha, packedLight);
                        emit(vc, mat, normal, face[i + 1], alpha, packedLight);
                        emit(vc, mat, normal, face[i + 2], alpha, packedLight);
                    }
                }
            }
        }

        private static void emit(VertexConsumer vc, Matrix4f mat, Matrix3f normal,
                                 PetalVertex vertex, int alpha, int packedLight) {
            vc.vertex(mat, vertex.x, vertex.y, vertex.z)
                    .color(255, 255, 255, alpha)
                    .uv(vertex.u, vertex.v)
                    .overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(packedLight)
                    .normal(normal, 0.0F, 1.0F, 0.0F)
                    .endVertex();
        }
    }

    private record PetalVertex(float x, float y, float z, float u, float v) {
    }

    private static final class PetalCloud {
        private final Random random;
        private final List<Petal> petals = new ArrayList<>();
        private int lastTick = -1;

        private PetalCloud(int entityId) {
            this.random = new Random(entityId * 341873128712L + 132897987541L);
        }

        private void tickTo(int tick) {
            if (lastTick < 0) {
                lastTick = tick - 1;
            }
            while (lastTick < tick) {
                lastTick++;
                tickOne(lastTick);
            }
        }

        private void tickOne(int tick) {
            if (tick >= 0 && tick <= 5) {
                spawn(randomInt(3, 10));
            }
            if (tick > 70 && tick < 75) {
                spawn(randomInt(5, 20));
            }

            Iterator<Petal> iterator = petals.iterator();
            while (iterator.hasNext()) {
                Petal petal = iterator.next();
                petal.tick();
                if (petal.expired()) {
                    iterator.remove();
                }
            }
        }

        private int randomInt(int minInclusive, int maxInclusive) {
            return minInclusive + random.nextInt(maxInclusive - minInclusive + 1);
        }

        private void spawn(int count) {
            for (int i = 0; i < count; i++) {
                float speed = random.nextInt(1500) / 7000.0F;
                float direction = (float) (random.nextFloat() * Math.PI * 2.0F);
                float vx = Mth.cos(direction) * speed;
                float vz = Mth.sin(direction) * speed;
                float vy = (random.nextFloat() - 0.5F) / 10.0F;
                petals.add(new Petal(random, vx, vy, vz));
            }
        }
    }

    private static final class Petal {
        private float prevX;
        private float prevY;
        private float prevZ;
        private float x;
        private float y;
        private float z;
        private float vx;
        private float vy;
        private float vz;
        private float rotationX;
        private float rotationY;
        private float rotationZ;
        private final float rotationSpeedX;
        private final float rotationSpeedY;
        private final float rotationSpeedZ;
        private float scale = 0.05F;
        private final float shrink;
        private final int texture;
        private int age;

        private Petal(Random random, float vx, float vy, float vz) {
            this.x = 0.0F;
            this.y = 1.0F + random.nextFloat() / 10.0F;
            this.z = 0.0F;
            this.prevX = x;
            this.prevY = y;
            this.prevZ = z;
            this.vx = vx;
            this.vy = vy;
            this.vz = vz;
            this.rotationSpeedX = random.nextFloat() * 5.0F - 2.5F;
            this.rotationSpeedY = random.nextFloat() * 5.0F - 2.5F;
            this.rotationSpeedZ = random.nextFloat() * 5.0F - 2.5F;
            this.shrink = 0.00012F + random.nextInt(10) / 20000.0F;
            this.texture = random.nextInt(PETAL_TEXTURES.length);
        }

        private void tick() {
            prevX = x;
            prevY = y;
            prevZ = z;
            age++;

            vy -= 0.0001F;
            vx *= 0.98F;
            vy *= 0.98F;
            vz *= 0.98F;
            x += vx;
            y += vy;
            z += vz;

            rotationX = wrapDegrees(rotationX + rotationSpeedX);
            rotationY = wrapDegrees(rotationY + rotationSpeedY);
            rotationZ = wrapDegrees(rotationZ + rotationSpeedZ);
            scale = Math.max(0.018F, scale - shrink);
        }

        private static float wrapDegrees(float value) {
            if (value > 360.0F) {
                return value - 360.0F;
            }
            if (value < 0.0F) {
                return value + 360.0F;
            }
            return value;
        }

        private int alpha() {
            float ratio = age / (float) PETAL_MAX_AGE;
            float fade = age <= PETAL_FADE_AGE
                    ? 1.0F
                    : 1.0F - (age - PETAL_FADE_AGE) / (float) (PETAL_MAX_AGE - PETAL_FADE_AGE);
            float alpha = Mth.clamp(fade, 0.0F, 1.0F);
            return (int) (Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F);
        }

        private int bloomAlpha() {
            return (int) (alpha() * 0.62F);
        }

        private float renderScale() {
            float fadeRatio = age <= PETAL_FADE_AGE
                    ? 1.0F
                    : 1.0F - (age - PETAL_FADE_AGE) / (float) (PETAL_MAX_AGE - PETAL_FADE_AGE);
            return scale * (0.55F + 0.45F * Mth.clamp(fadeRatio, 0.0F, 1.0F));
        }

        private boolean expired() {
            return age >= PETAL_MAX_AGE;
        }
    }

    @Override
    public ResourceLocation getTextureLocation(RainUmbrellaEntity entity) {
        return EMPTY_TEXTURE;
    }
}
