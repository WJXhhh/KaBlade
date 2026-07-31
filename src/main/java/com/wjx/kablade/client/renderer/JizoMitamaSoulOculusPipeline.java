package com.wjx.kablade.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.wjx.kablade.Main;
import com.wjx.kablade.client.shader.OculusFramebufferAccess;
import com.wjx.kablade.client.shader.ShaderCompat;
import com.wjx.kablade.client.shader.SkillPostShaders;
import com.wjx.kablade.client.shader.SkillShaderTarget;
import com.wjx.kablade.entity.JizoMitamaSoulEntity;
import mods.flammpfeil.slashblade.client.renderer.model.BladeModelManager;
import mods.flammpfeil.slashblade.client.renderer.model.obj.Face;
import mods.flammpfeil.slashblade.client.renderer.model.obj.GroupObject;
import mods.flammpfeil.slashblade.client.renderer.model.obj.TextureCoordinate;
import mods.flammpfeil.slashblade.client.renderer.model.obj.Vertex;
import mods.flammpfeil.slashblade.client.renderer.model.obj.WavefrontObject;
import mods.flammpfeil.slashblade.client.renderer.util.BladeRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Private Oculus/Iris path for Jizo's translucent body and red aura.
 *
 * <p>Shader packs replace entity {@code ShaderInstance}s and commonly reinterpret their alpha.
 * This path therefore draws the three soul groups with a private GL program into HDR buffers
 * sharing Oculus' live depth attachment. The solid blade remains in the ordinary entity pass.</p>
 */
@Mod.EventBusSubscriber(modid = Main.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class JizoMitamaSoulOculusPipeline {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(
            Main.MODID, "model/util/jizo_skill_boss/mdl.obj");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Main.MODID, "model/util/jizo_skill_boss/tex.png");
    private static final Vector3f BODY_PIVOT = new Vector3f(0.0F, 1.28F, -0.16F);
    private static final Vector3f HAND_L_PIVOT = new Vector3f(0.82F, 1.04F, 0.28F);
    private static final Vector3f HAND_R_PIVOT = new Vector3f(-0.82F, 1.04F, 0.28F);
    private static final float OCULUS_BODY_OPACITY = 0.68F;
    private static final int BLOOM_BLUR_PASSES = 4;

    private static final Map<Integer, QueuedSoul> QUEUED = new LinkedHashMap<>();
    private static final SoulFramebuffer FRAMEBUFFER = new SoulFramebuffer();
    private static final SoulProgram PROGRAM = new SoulProgram();
    private static final SoulMeshes MESHES = new SoulMeshes();

    private static boolean resourcesDirty;
    private static boolean disabledForSession;
    private static boolean loggedMissingTarget;
    private static boolean loggedFailure;
    private static boolean loggedActive;

    private JizoMitamaSoulOculusPipeline() {
    }

    /** Returns true when the normal renderer must draw only the solid blade. */
    public static boolean enqueue(JizoMitamaSoulEntity entity, float partialTick,
                                  JizoMitamaSoulRenderer.AnimationPose animation) {
        if (!ShaderCompat.shouldUseOculusPostPath()) {
            return false;
        }
        QUEUED.put(entity.getId(), new QueuedSoul(
                entity,
                Mth.lerp(partialTick, entity.xOld, entity.getX()),
                Mth.lerp(partialTick, entity.yOld, entity.getY()),
                Mth.lerp(partialTick, entity.zOld, entity.getZ()),
                Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot()),
                animation));
        return true;
    }

    /** Marks private GL objects stale after a resource or shader-pack reload. */
    public static void invalidateResources() {
        resourcesDirty = true;
        disabledForSession = false;
        loggedMissingTarget = false;
        loggedFailure = false;
        loggedActive = false;
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS
                || QUEUED.isEmpty()) {
            return;
        }

        List<QueuedSoul> souls = new ArrayList<>(QUEUED.values());
        QUEUED.clear();
        if (disabledForSession || !ShaderCompat.shouldUseOculusPostPath()) {
            renderSafeFallback(event, souls);
            return;
        }

        Optional<SkillShaderTarget> resolved = OculusFramebufferAccess.findTranslucentTarget();
        if (resolved.isEmpty() || !resolved.get().isComplete()) {
            if (!loggedMissingTarget) {
                loggedMissingTarget = true;
                Main.LOGGER.warn("Jizo Soul Oculus renderer could not resolve a complete "
                        + "translucent target; using the high-opacity safe fallback.");
            }
            renderSafeFallback(event, souls);
            return;
        }

        SkillShaderTarget target = resolved.get();
        GlState state = GlState.capture();
        boolean failed = false;
        try {
            if (resourcesDirty) {
                closeResources();
                resourcesDirty = false;
            }
            PROGRAM.ensureLoaded();
            MESHES.ensureLoaded();
            FRAMEBUFFER.ensureAllocated(target.width(), target.height());

            if (!loggedActive) {
                loggedActive = true;
                Main.LOGGER.info("Jizo Soul Oculus pipeline active: fbo={}, color={}, depth={}, "
                                + "size={}x{}",
                        target.framebufferId(), target.colorTextureId(), target.depthTextureId(),
                        target.width(), target.height());
            }

            Matrix4f projection = new Matrix4f(event.getProjectionMatrix());
            float gameTime = shaderGameTime(event.getPartialTick());
            int texture = soulTextureId();

            FRAMEBUFFER.beginDepth(target);
            renderQueued(event, souls, projection, gameTime, texture, Pass.DEPTH);

            FRAMEBUFFER.beginSurface(target);
            renderQueued(event, souls, projection, gameTime, texture, Pass.SURFACE);

            FRAMEBUFFER.beginGlow(target);
            renderQueued(event, souls, projection, gameTime, texture, Pass.GLOW);

            FRAMEBUFFER.composite(target);
        } catch (RuntimeException | IOException exception) {
            failed = true;
            disabledForSession = true;
            if (!loggedFailure) {
                loggedFailure = true;
                Main.LOGGER.warn("Disabling the Jizo Soul Oculus pipeline for this session; "
                        + "using the high-opacity safe fallback.", exception);
            }
        } finally {
            state.restore();
        }

        if (failed) {
            renderSafeFallback(event, souls);
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            QUEUED.clear();
            invalidateResources();
        }
    }

    private static void renderQueued(RenderLevelStageEvent event, List<QueuedSoul> souls,
                                     Matrix4f projection, float gameTime, int texture, Pass pass) {
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        Matrix4f globalModelView = new Matrix4f(RenderSystem.getModelViewMatrix());
        for (QueuedSoul soul : souls) {
            if (!soul.entity.isAlive()) {
                continue;
            }
            poseStack.pushPose();
            poseStack.translate(soul.x - camera.x, soul.y - camera.y, soul.z - camera.z);
            poseStack.mulPose(Axis.YP.rotationDegrees(-soul.yaw));
            applyRigRoot(poseStack, soul.animation);

            drawBodyPart(poseStack, globalModelView, projection, gameTime, texture,
                    soul.animation.alpha() * OCULUS_BODY_OPACITY,
                    soul.animation.bodyRotation(), BODY_PIVOT, MESHES.body, pass);
            drawHandPart(poseStack, globalModelView, projection, gameTime, texture,
                    soul.animation.alpha() * OCULUS_BODY_OPACITY,
                    soul.animation.bodyRotation(), soul.animation.handLRotation(),
                    HAND_L_PIVOT, MESHES.handL, pass);
            drawHandPart(poseStack, globalModelView, projection, gameTime, texture,
                    soul.animation.alpha() * OCULUS_BODY_OPACITY,
                    soul.animation.bodyRotation(), soul.animation.handRRotation(),
                    HAND_R_PIVOT, MESHES.handR, pass);
            poseStack.popPose();
        }
    }

    private static void applyRigRoot(PoseStack poseStack,
                                     JizoMitamaSoulRenderer.AnimationPose animation) {
        poseStack.translate(animation.rootX(), animation.correctedRootY(), animation.rootZ());
        poseStack.mulPose(animation.rootRotation());
        poseStack.scale(animation.scale(), animation.scale(), animation.scale());
        poseStack.mulPose(animation.rigRootRotation());
    }

    private static void drawBodyPart(PoseStack poseStack, Matrix4f globalModelView,
                                     Matrix4f projection, float gameTime, int texture,
                                     float opacity, org.joml.Quaternionf body,
                                     Vector3f pivot, SoulMesh mesh, Pass pass) {
        poseStack.pushPose();
        translate(poseStack, pivot);
        poseStack.mulPose(body);
        translate(poseStack, new Vector3f(pivot).negate());
        drawMesh(poseStack, globalModelView, projection, gameTime, texture, opacity, mesh, pass);
        poseStack.popPose();
    }

    private static void drawHandPart(PoseStack poseStack, Matrix4f globalModelView,
                                     Matrix4f projection, float gameTime, int texture,
                                     float opacity, org.joml.Quaternionf body,
                                     org.joml.Quaternionf hand, Vector3f pivot,
                                     SoulMesh mesh, Pass pass) {
        poseStack.pushPose();
        translate(poseStack, BODY_PIVOT);
        poseStack.mulPose(body);
        translate(poseStack, pivot);
        poseStack.mulPose(hand);
        translate(poseStack, new Vector3f(BODY_PIVOT).add(pivot).negate());
        drawMesh(poseStack, globalModelView, projection, gameTime, texture, opacity, mesh, pass);
        poseStack.popPose();
    }

    private static void drawMesh(PoseStack poseStack, Matrix4f globalModelView,
                                 Matrix4f projection, float gameTime, int texture,
                                 float opacity, SoulMesh mesh, Pass pass) {
        Matrix4f modelView = new Matrix4f(globalModelView).mul(poseStack.last().pose());
        PROGRAM.apply(modelView, projection, gameTime, opacity, texture, pass);
        mesh.draw();
    }

    private static void renderSafeFallback(RenderLevelStageEvent event, List<QueuedSoul> souls) {
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource immediate = MultiBufferSource.immediate(new BufferBuilder(524288));
        for (QueuedSoul soul : souls) {
            if (!soul.entity.isAlive()) {
                continue;
            }
            poseStack.pushPose();
            poseStack.translate(soul.x - camera.x, soul.y - camera.y, soul.z - camera.z);
            poseStack.mulPose(Axis.YP.rotationDegrees(-soul.yaw));
            JizoMitamaSoulRenderer.renderOculusFallbackSoul(
                    soul.animation, poseStack, immediate, BladeRenderState.MAX_LIGHT);
            poseStack.popPose();
        }
        immediate.endBatch();
    }

    private static int soulTextureId() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getTextureManager().bindForSetup(TEXTURE);
        return GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
    }

    private static float shaderGameTime(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return 0.0F;
        }
        return (float) ((minecraft.level.getGameTime() + partialTick) % 24000.0D) / 24000.0F;
    }

    private static void translate(PoseStack poseStack, Vector3f value) {
        poseStack.translate(value.x, value.y, value.z);
    }

    private static void closeResources() {
        PROGRAM.close();
        MESHES.close();
        FRAMEBUFFER.close();
    }

    private enum Pass {
        SURFACE(0), GLOW(1), DEPTH(2);

        private final int shaderMode;

        Pass(int shaderMode) {
            this.shaderMode = shaderMode;
        }
    }

    private static final class SoulProgram implements AutoCloseable {
        private static final FloatBuffer MATRIX = BufferUtils.createFloatBuffer(16);
        private int id;
        private int modelViewLocation;
        private int projectionLocation;
        private int gameTimeLocation;
        private int opacityLocation;
        private int passModeLocation;
        private int samplerLocation;

        private void ensureLoaded() throws IOException {
            if (id != 0) {
                return;
            }
            int vertex = compileShader(GL20.GL_VERTEX_SHADER, readShader("jizo_soul_oculus.vsh"));
            int fragment = compileShader(GL20.GL_FRAGMENT_SHADER, readShader("jizo_soul_oculus.fsh"));
            id = GL20.glCreateProgram();
            GL20.glAttachShader(id, vertex);
            GL20.glAttachShader(id, fragment);
            GL20.glBindAttribLocation(id, 0, "Position");
            GL20.glBindAttribLocation(id, 1, "UV0");
            GL20.glBindAttribLocation(id, 2, "Normal");
            GL20.glLinkProgram(id);
            GL20.glDeleteShader(vertex);
            GL20.glDeleteShader(fragment);
            if (GL20.glGetProgrami(id, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
                String log = GL20.glGetProgramInfoLog(id);
                GL20.glDeleteProgram(id);
                id = 0;
                throw new IllegalStateException("Jizo Soul Oculus program link failed: " + log);
            }
            modelViewLocation = GL20.glGetUniformLocation(id, "ModelViewMat");
            projectionLocation = GL20.glGetUniformLocation(id, "ProjMat");
            gameTimeLocation = GL20.glGetUniformLocation(id, "GameTime");
            opacityLocation = GL20.glGetUniformLocation(id, "Opacity");
            passModeLocation = GL20.glGetUniformLocation(id, "PassMode");
            samplerLocation = GL20.glGetUniformLocation(id, "Sampler0");
        }

        private void apply(Matrix4f modelView, Matrix4f projection, float gameTime,
                           float opacity, int texture, Pass pass) {
            GL20.glUseProgram(id);
            uploadMatrix(modelViewLocation, modelView);
            uploadMatrix(projectionLocation, projection);
            GL20.glUniform1f(gameTimeLocation, gameTime);
            GL20.glUniform1f(opacityLocation, Mth.clamp(opacity, 0.0F, 1.0F));
            GL20.glUniform1i(passModeLocation, pass.shaderMode);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
            GL20.glUniform1i(samplerLocation, 0);
        }

        private static String readShader(String file) throws IOException {
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath(
                    Main.MODID, "shaders/core/" + file);
            try (var stream = Minecraft.getInstance().getResourceManager().open(location)) {
                return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            }
        }

        private static int compileShader(int type, String source) {
            int shader = GL20.glCreateShader(type);
            GL20.glShaderSource(shader, source);
            GL20.glCompileShader(shader);
            if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
                String log = GL20.glGetShaderInfoLog(shader);
                GL20.glDeleteShader(shader);
                throw new IllegalStateException("Jizo Soul Oculus shader compile failed: " + log);
            }
            return shader;
        }

        private static void uploadMatrix(int location, Matrix4f matrix) {
            MATRIX.clear();
            matrix.get(MATRIX);
            GL20.glUniformMatrix4fv(location, false, MATRIX);
        }

        @Override
        public void close() {
            if (id != 0) {
                GL20.glDeleteProgram(id);
            }
            id = 0;
        }
    }

    private static final class SoulMeshes implements AutoCloseable {
        private WavefrontObject source;
        private SoulMesh body;
        private SoulMesh handL;
        private SoulMesh handR;

        private void ensureLoaded() {
            WavefrontObject model = BladeModelManager.getInstance().getModel(MODEL);
            if (source == model && body != null) {
                return;
            }
            close();
            source = model;
            body = SoulMesh.from(model, "Body");
            handL = SoulMesh.from(model, "Hand_L");
            handR = SoulMesh.from(model, "Hand_R");
        }

        @Override
        public void close() {
            if (body != null) body.close();
            if (handL != null) handL.close();
            if (handR != null) handR.close();
            body = handL = handR = null;
            source = null;
        }
    }

    private static final class SoulMesh implements AutoCloseable {
        private static final int STRIDE = 32;
        private final ByteBuffer vertices;
        private final int vertexCount;
        private int vertexArray;
        private int vertexBuffer;

        private SoulMesh(ByteBuffer vertices, int vertexCount) {
            this.vertices = vertices;
            this.vertexCount = vertexCount;
        }

        private static SoulMesh from(WavefrontObject model, String name) {
            GroupObject group = model.groupObjects.stream()
                    .filter(candidate -> name.equals(candidate.name))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Missing Jizo OBJ group: " + name));
            int vertexCount = group.faces.stream().mapToInt(face -> face.vertices.length).sum();
            ByteBuffer data = BufferUtils.createByteBuffer(vertexCount * STRIDE);
            for (Face face : group.faces) {
                if (face.vertices.length != 3) {
                    throw new IllegalStateException("Jizo Oculus mesh requires triangles: " + name);
                }
                Vertex faceNormal = face.faceNormal != null ? face.faceNormal : face.calculateFaceNormal();
                for (int i = 0; i < face.vertices.length; i++) {
                    Vertex position = face.vertices[i];
                    TextureCoordinate uv = face.textureCoordinates != null
                            && i < face.textureCoordinates.length ? face.textureCoordinates[i] : null;
                    Vertex normal = face.vertexNormals != null && i < face.vertexNormals.length
                            && face.vertexNormals[i] != null ? face.vertexNormals[i] : faceNormal;
                    data.putFloat(position.x).putFloat(position.y).putFloat(position.z);
                    data.putFloat(uv == null ? 0.0F : uv.u).putFloat(uv == null ? 0.0F : uv.v);
                    data.putFloat(normal.x).putFloat(normal.y).putFloat(normal.z);
                }
            }
            data.flip();
            return new SoulMesh(data, vertexCount);
        }

        private void draw() {
            if (vertexArray == 0) {
                upload();
            }
            GL30.glBindVertexArray(vertexArray);
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, vertexCount);
        }

        private void upload() {
            vertexArray = GL30.glGenVertexArrays();
            vertexBuffer = GL15.glGenBuffers();
            GL30.glBindVertexArray(vertexArray);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vertexBuffer);
            vertices.rewind();
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertices, GL15.GL_STATIC_DRAW);
            GL20.glEnableVertexAttribArray(0);
            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, STRIDE, 0L);
            GL20.glEnableVertexAttribArray(1);
            GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, STRIDE, 12L);
            GL20.glEnableVertexAttribArray(2);
            GL20.glVertexAttribPointer(2, 3, GL11.GL_FLOAT, false, STRIDE, 20L);
        }

        @Override
        public void close() {
            if (vertexBuffer != 0) GL15.glDeleteBuffers(vertexBuffer);
            if (vertexArray != 0) GL30.glDeleteVertexArrays(vertexArray);
            vertexBuffer = vertexArray = 0;
        }
    }

    private static final class SoulFramebuffer implements AutoCloseable {
        private final FloatBuffer transparent = BufferUtils.createFloatBuffer(4);
        private int effectFramebuffer;
        private int copyFramebuffer;
        private int effectTexture;
        private int glowTexture;
        private int blurTextureA;
        private int blurTextureB;
        private int sceneTexture;
        private int width;
        private int height;

        private void ensureAllocated(int nextWidth, int nextHeight) {
            if (effectFramebuffer == 0) effectFramebuffer = GL30.glGenFramebuffers();
            if (copyFramebuffer == 0) copyFramebuffer = GL30.glGenFramebuffers();
            if (effectTexture == 0) effectTexture = GL11.glGenTextures();
            if (glowTexture == 0) glowTexture = GL11.glGenTextures();
            if (blurTextureA == 0) blurTextureA = GL11.glGenTextures();
            if (blurTextureB == 0) blurTextureB = GL11.glGenTextures();
            if (sceneTexture == 0) sceneTexture = GL11.glGenTextures();
            if (width == nextWidth && height == nextHeight) return;
            width = nextWidth;
            height = nextHeight;
            allocate(effectTexture);
            allocate(glowTexture);
            allocate(blurTextureA);
            allocate(blurTextureB);
            allocate(sceneTexture);
        }

        private void beginDepth(SkillShaderTarget target) {
            bindGeometryTarget(effectTexture, target.depthTextureId(), "depth");
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            GL11.glDepthMask(true);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glColorMask(false, false, false, false);
        }

        private void beginSurface(SkillShaderTarget target) {
            bindGeometryTarget(effectTexture, target.depthTextureId(), "surface");
            GL11.glColorMask(true, true, true, true);
            clearColor();
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthFunc(GL11.GL_EQUAL);
            GL11.glDepthMask(false);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_BLEND);
            GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                    GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        }

        private void beginGlow(SkillShaderTarget target) {
            bindGeometryTarget(glowTexture, target.depthTextureId(), "glow");
            GL11.glColorMask(true, true, true, true);
            clearColor();
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            GL11.glDepthMask(false);
            GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_BLEND);
            GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE,
                    GL11.GL_ZERO, GL11.GL_ONE);
        }

        private void bindGeometryTarget(int colorTexture, int depthTexture, String stage) {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, effectFramebuffer);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                    GL11.GL_TEXTURE_2D, colorTexture, 0);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT,
                    GL11.GL_TEXTURE_2D, depthTexture, 0);
            GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
            checkFramebuffer("Jizo Soul " + stage);
            GL11.glViewport(0, 0, width, height);
        }

        private void clearColor() {
            transparent.clear();
            transparent.put(0.0F).put(0.0F).put(0.0F).put(0.0F).flip();
            GL30.glClearBufferfv(GL11.GL_COLOR, 0, transparent);
        }

        private void composite(SkillShaderTarget target) {
            copyScene(target);
            int bloom = blurGlow();
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, target.framebufferId());
            GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
            GL11.glViewport(0, 0, width, height);
            SkillPostShaders.compositeJizo(sceneTexture, effectTexture, glowTexture,
                    bloom, width, height);
        }

        private void copyScene(SkillShaderTarget target) {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, target.framebufferId());
            GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, copyFramebuffer);
            GL30.glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                    GL11.GL_TEXTURE_2D, sceneTexture, 0);
            GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
            checkDrawFramebuffer("Jizo Soul scene copy");
            GL30.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height,
                    GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        }

        private int blurGlow() {
            int source = glowTexture;
            for (int i = 0; i < BLOOM_BLUR_PASSES; i++) {
                bindPostTarget(blurTextureA);
                SkillPostShaders.blurBloodfyre(source, width, height, true);
                bindPostTarget(blurTextureB);
                SkillPostShaders.blurBloodfyre(blurTextureA, width, height, false);
                source = blurTextureB;
            }
            return source;
        }

        private void bindPostTarget(int texture) {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, copyFramebuffer);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                    GL11.GL_TEXTURE_2D, texture, 0);
            GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
            checkFramebuffer("Jizo Soul bloom");
            GL11.glViewport(0, 0, width, height);
        }

        private void allocate(int texture) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL13.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL13.GL_CLAMP_TO_EDGE);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA16F,
                    width, height, 0, GL11.GL_RGBA, GL11.GL_FLOAT, 0L);
        }

        private static void checkFramebuffer(String stage) {
            int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
            if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
                throw new IllegalStateException(stage + " framebuffer incomplete: 0x"
                        + Integer.toHexString(status));
            }
        }

        private static void checkDrawFramebuffer(String stage) {
            int status = GL30.glCheckFramebufferStatus(GL30.GL_DRAW_FRAMEBUFFER);
            if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
                throw new IllegalStateException(stage + " framebuffer incomplete: 0x"
                        + Integer.toHexString(status));
            }
        }

        @Override
        public void close() {
            deleteTexture(effectTexture);
            deleteTexture(glowTexture);
            deleteTexture(blurTextureA);
            deleteTexture(blurTextureB);
            deleteTexture(sceneTexture);
            effectTexture = glowTexture = blurTextureA = blurTextureB = sceneTexture = 0;
            if (effectFramebuffer != 0) GL30.glDeleteFramebuffers(effectFramebuffer);
            if (copyFramebuffer != 0) GL30.glDeleteFramebuffers(copyFramebuffer);
            effectFramebuffer = copyFramebuffer = 0;
            width = height = 0;
        }

        private static void deleteTexture(int texture) {
            if (texture != 0) GL11.glDeleteTextures(texture);
        }
    }

    private record QueuedSoul(JizoMitamaSoulEntity entity, double x, double y, double z,
                              float yaw, JizoMitamaSoulRenderer.AnimationPose animation) {
    }

    private record GlState(int drawFramebuffer, int readFramebuffer,
                           int drawBuffer, int readBuffer,
                           int program, int vertexArray, int arrayBuffer,
                           int activeTexture, int[] textures, int[] viewport,
                           boolean blend, boolean depth, boolean cull,
                           boolean depthMask, int depthFunc,
                           int blendSrcRgb, int blendDstRgb,
                           int blendSrcAlpha, int blendDstAlpha,
                           int blendEquationRgb, int blendEquationAlpha,
                           boolean[] colorMask) {

        private static GlState capture() {
            int active = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            int[] textures = new int[4];
            for (int i = 0; i < textures.length; i++) {
                GL13.glActiveTexture(GL13.GL_TEXTURE0 + i);
                textures[i] = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            }
            GL13.glActiveTexture(active);
            IntBuffer viewportBuffer = BufferUtils.createIntBuffer(4);
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewportBuffer);
            int[] viewport = {viewportBuffer.get(0), viewportBuffer.get(1),
                    viewportBuffer.get(2), viewportBuffer.get(3)};
            ByteBuffer mask = BufferUtils.createByteBuffer(4);
            GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK, mask);
            boolean[] colorMask = {mask.get(0) != 0, mask.get(1) != 0,
                    mask.get(2) != 0, mask.get(3) != 0};
            return new GlState(
                    GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING),
                    GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING),
                    GL11.glGetInteger(GL11.GL_DRAW_BUFFER), GL11.glGetInteger(GL11.GL_READ_BUFFER),
                    GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM),
                    GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING),
                    GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING), active, textures, viewport,
                    GL11.glIsEnabled(GL11.GL_BLEND), GL11.glIsEnabled(GL11.GL_DEPTH_TEST),
                    GL11.glIsEnabled(GL11.GL_CULL_FACE), GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK),
                    GL11.glGetInteger(GL11.GL_DEPTH_FUNC),
                    GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB), GL11.glGetInteger(GL14.GL_BLEND_DST_RGB),
                    GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA), GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA),
                    GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB),
                    GL11.glGetInteger(GL20.GL_BLEND_EQUATION_ALPHA), colorMask);
        }

        private void restore() {
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
            GL11.glDrawBuffer(drawBuffer);
            GL11.glReadBuffer(readBuffer);
            GL11.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
            GL20.glUseProgram(program);
            GL30.glBindVertexArray(vertexArray);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, arrayBuffer);
            for (int i = 0; i < textures.length; i++) {
                GL13.glActiveTexture(GL13.GL_TEXTURE0 + i);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, textures[i]);
            }
            GL13.glActiveTexture(activeTexture);
            GL14.glBlendFuncSeparate(blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha);
            GL20.glBlendEquationSeparate(blendEquationRgb, blendEquationAlpha);
            GL11.glDepthMask(depthMask);
            GL11.glDepthFunc(depthFunc);
            GL11.glColorMask(colorMask[0], colorMask[1], colorMask[2], colorMask[3]);
            setEnabled(GL11.GL_BLEND, blend);
            setEnabled(GL11.GL_DEPTH_TEST, depth);
            setEnabled(GL11.GL_CULL_FACE, cull);
        }

        private static void setEnabled(int capability, boolean enabled) {
            if (enabled) GL11.glEnable(capability); else GL11.glDisable(capability);
        }
    }
}
