package com.wjx.kablade.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.wjx.kablade.Main;
import com.wjx.kablade.client.KabladeRenderTypes;
import com.wjx.kablade.client.shader.OculusFramebufferAccess;
import com.wjx.kablade.client.shader.ShaderCompat;
import com.wjx.kablade.client.shader.SkillShaderTarget;
import com.wjx.kablade.entity.ValkyrieImpactEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Dedicated Oculus/Iris pipeline for Valkyrie Impact (女武神冲击).
 *
 * <p>When shader packs are active via Oculus/Iris, vanilla core shaders in entity render passes
 * are replaced. This pipeline intercepts visible Valkyrie Impact instances, converts quads to
 * explicit triangles (eliminating quad diagonal seams), and renders the 5-layer multi-row effect
 * directly into the Oculus translucent HDR framebuffer with private shaders sharing live depth.</p>
 */
@Mod.EventBusSubscriber(modid = Main.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ValkyrieImpactOculusPipeline {
    private static final Map<Integer, QueuedImpact> QUEUED = new LinkedHashMap<>();
    private static final GlProgram PROGRAM = new GlProgram();
    private static final MeshDrawer MESH_DRAWER = new MeshDrawer();

    private static boolean resourcesDirty;
    private static boolean disabledForSession;
    private static boolean loggedMissingTarget;
    private static boolean loggedFailure;
    private static boolean loggedActive;

    private ValkyrieImpactOculusPipeline() {
    }

    /** Returns true when the entity renderer should suppress the vanilla pass for this frame. */
    public static boolean enqueue(ValkyrieImpactEntity entity, float partialTick) {
        if (!ShaderCompat.shouldUseOculusPostPath()) {
            return false;
        }

        double x = Mth.lerp(partialTick, entity.xOld, entity.getX());
        double y = Mth.lerp(partialTick, entity.yOld, entity.getY());
        double z = Mth.lerp(partialTick, entity.zOld, entity.getZ());
        float timeSeconds = (entity.getLifetime() + partialTick) / 30.0F;

        if (timeSeconds < 0.56F || timeSeconds > 2.24F) {
            return false;
        }

        QUEUED.put(entity.getId(), new QueuedImpact(entity, x, y, z, entity.getYRot(), partialTick, timeSeconds));
        return true;
    }

    public static void invalidateResources() {
        resourcesDirty = true;
        disabledForSession = false;
        loggedMissingTarget = false;
        loggedFailure = false;
        loggedActive = false;
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || QUEUED.isEmpty()) {
            return;
        }

        List<QueuedImpact> impacts = new ArrayList<>(QUEUED.values());
        QUEUED.clear();

        if (disabledForSession || !ShaderCompat.shouldUseOculusPostPath()) {
            renderFallback(event, impacts);
            return;
        }

        Optional<SkillShaderTarget> resolved = OculusFramebufferAccess.findTranslucentTarget();
        if (resolved.isEmpty() || !resolved.get().isComplete()) {
            if (!loggedMissingTarget) {
                loggedMissingTarget = true;
                Main.LOGGER.warn("Valkyrie Impact Oculus renderer could not resolve translucent target; applying fallback path");
            }
            renderFallback(event, impacts);
            return;
        }

        SkillShaderTarget target = resolved.get();
        if (!loggedActive) {
            loggedActive = true;
            Main.LOGGER.info("Valkyrie Impact Oculus pipeline active: fbo={}, color={}, depth={}, size={}x{}",
                    target.framebufferId(), target.colorTextureId(), target.depthTextureId(),
                    target.width(), target.height());
        }

        GlState state = GlState.capture();
        boolean failed = false;
        try {
            if (resourcesDirty) {
                closeResources();
                resourcesDirty = false;
            }
            PROGRAM.ensureLoaded();
            bindTarget(target);

            Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewMatrix());
            Matrix4f projection = new Matrix4f(event.getProjectionMatrix());
            float gameTime = shaderGameTime(event.getPartialTick());

            PROGRAM.apply(modelView, projection, gameTime);
            renderQueued(event, impacts);
        } catch (RuntimeException | IOException exception) {
            failed = true;
            disabledForSession = true;
            if (!loggedFailure) {
                loggedFailure = true;
                Main.LOGGER.warn("Disabling Valkyrie Impact Oculus pipeline for this session; applying fallback path", exception);
            }
        } finally {
            state.restore();
        }

        if (failed) {
            renderFallback(event, impacts);
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            QUEUED.clear();
            invalidateResources();
        }
    }

    private static void bindTarget(SkillShaderTarget target) {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, target.framebufferId());
        GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
        int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
        if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException("Valkyrie Impact Oculus framebuffer incomplete: 0x" + Integer.toHexString(status));
        }
        GL11.glViewport(0, 0, target.width(), target.height());
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glDepthMask(false);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glColorMask(true, true, true, true);
        GL11.glEnable(GL11.GL_BLEND);
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO, GL11.GL_ONE);
        GL20.glBlendEquationSeparate(GL14.GL_FUNC_ADD, GL14.GL_FUNC_ADD);
    }

    private static void renderQueued(RenderLevelStageEvent event, List<QueuedImpact> impacts) {
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        QuadTriangleConsumer additiveConsumer = new QuadTriangleConsumer();
        QuadTriangleConsumer solidConsumer = new QuadTriangleConsumer();

        for (QueuedImpact impact : impacts) {
            if (!impact.entity().isAlive()) {
                continue;
            }
            poseStack.pushPose();
            try {
                poseStack.translate(impact.x() - camera.x, impact.y() - camera.y, impact.z() - camera.z);
                poseStack.mulPose(Axis.YP.rotationDegrees(-impact.yaw()));
                ValkyrieImpactRenderer.renderLayers(poseStack, additiveConsumer, solidConsumer, 15728880, impact.timeSeconds());
            } finally {
                poseStack.popPose();
            }
        }
        additiveConsumer.finish();
        solidConsumer.finish();
        MESH_DRAWER.draw(solidConsumer.vertices);
        MESH_DRAWER.draw(additiveConsumer.vertices);
    }

    private static void renderFallback(RenderLevelStageEvent event, List<QueuedImpact> impacts) {
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource immediate = MultiBufferSource.immediate(new BufferBuilder(131072));

        for (QueuedImpact impact : impacts) {
            if (!impact.entity().isAlive()) {
                continue;
            }
            poseStack.pushPose();
            try {
                poseStack.translate(impact.x() - camera.x, impact.y() - camera.y, impact.z() - camera.z);
                poseStack.mulPose(Axis.YP.rotationDegrees(-impact.yaw()));
                VertexConsumer additiveVc = immediate.getBuffer(KabladeRenderTypes.nuclearShockOculusSafe());
                VertexConsumer solidVc = immediate.getBuffer(RenderType.entityCutout(ValkyrieImpactRenderer.DEEPSLATE_TEXTURE));
                ValkyrieImpactRenderer.renderLayers(poseStack, additiveVc, solidVc, 15728880, impact.timeSeconds());
            } finally {
                poseStack.popPose();
            }
        }
        immediate.endBatch();
    }

    private static float shaderGameTime(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return 0.0F;
        return (float) ((minecraft.level.getGameTime() + partialTick) % 24000.0D) / 24000.0F;
    }

    private static void closeResources() {
        PROGRAM.close();
        MESH_DRAWER.close();
    }

    private record QueuedImpact(ValkyrieImpactEntity entity, double x, double y, double z,
                                float yaw, float partialTick, float timeSeconds) {
    }

    private static final class GlProgram implements AutoCloseable {
        private static final FloatBuffer MATRIX = BufferUtils.createFloatBuffer(16);
        private int id;
        private int modelViewLocation;
        private int projectionLocation;
        private int gameTimeLocation;

        private void ensureLoaded() throws IOException {
            if (id != 0) return;
            String vertexSource = readShader("valkyrie_impact_oculus.vsh");
            String fragmentSource = readShader("valkyrie_impact_oculus.fsh");
            int vertex = compileShader(GL20.GL_VERTEX_SHADER, vertexSource);
            int fragment = compileShader(GL20.GL_FRAGMENT_SHADER, fragmentSource);
            id = GL20.glCreateProgram();
            GL20.glAttachShader(id, vertex);
            GL20.glAttachShader(id, fragment);
            GL20.glBindAttribLocation(id, 0, "Position");
            GL20.glBindAttribLocation(id, 1, "Color");
            GL20.glBindAttribLocation(id, 2, "UV0");
            GL20.glLinkProgram(id);
            GL20.glDeleteShader(vertex);
            GL20.glDeleteShader(fragment);
            if (GL20.glGetProgrami(id, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
                String log = GL20.glGetProgramInfoLog(id);
                close();
                throw new IllegalStateException("Valkyrie Impact Oculus program link failed: " + log);
            }
            modelViewLocation = GL20.glGetUniformLocation(id, "ModelViewMat");
            projectionLocation = GL20.glGetUniformLocation(id, "ProjMat");
            gameTimeLocation = GL20.glGetUniformLocation(id, "GameTime");
        }

        private void apply(Matrix4f modelView, Matrix4f projection, float gameTime) {
            GL20.glUseProgram(id);
            uploadMatrix(modelViewLocation, modelView);
            uploadMatrix(projectionLocation, projection);
            if (gameTimeLocation >= 0) GL20.glUniform1f(gameTimeLocation, gameTime);
        }

        private static void uploadMatrix(int location, Matrix4f matrix) {
            if (location < 0) return;
            MATRIX.clear();
            matrix.get(MATRIX);
            GL20.glUniformMatrix4fv(location, false, MATRIX);
        }

        private static String readShader(String file) throws IOException {
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath(Main.MODID, "shaders/core/" + file);
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
                throw new IllegalStateException("Valkyrie Impact Oculus shader compile failed: " + log);
            }
            return shader;
        }

        @Override
        public void close() {
            if (id != 0) {
                GL20.glDeleteProgram(id);
                id = 0;
            }
        }
    }

    private static final class MeshDrawer implements AutoCloseable {
        private static final int STRIDE = 24;
        private int vertexArray;
        private int vertexBuffer;
        private ByteBuffer uploadBuffer;

        private void draw(List<RawVertex> vertices) {
            if (vertices.isEmpty()) {
                return;
            }
            if (vertexArray == 0) vertexArray = GL30.glGenVertexArrays();
            if (vertexBuffer == 0) vertexBuffer = GL15.glGenBuffers();
            int byteCount = vertices.size() * STRIDE;
            if (uploadBuffer == null || uploadBuffer.capacity() < byteCount) {
                int capacity = 4096;
                while (capacity < byteCount) capacity <<= 1;
                uploadBuffer = BufferUtils.createByteBuffer(capacity);
            }
            uploadBuffer.clear();
            for (RawVertex vertex : vertices) {
                uploadBuffer.putFloat(vertex.x).putFloat(vertex.y).putFloat(vertex.z);
                uploadBuffer.put((byte) vertex.red).put((byte) vertex.green)
                        .put((byte) vertex.blue).put((byte) vertex.alpha);
                uploadBuffer.putFloat(vertex.u).putFloat(vertex.v);
            }
            uploadBuffer.flip();

            GL30.glBindVertexArray(vertexArray);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vertexBuffer);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, uploadBuffer, GL15.GL_STREAM_DRAW);
            GL20.glEnableVertexAttribArray(0);
            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, STRIDE, 0L);
            GL20.glEnableVertexAttribArray(1);
            GL20.glVertexAttribPointer(1, 4, GL11.GL_UNSIGNED_BYTE, true, STRIDE, 12L);
            GL20.glEnableVertexAttribArray(2);
            GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, STRIDE, 16L);
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, vertices.size());
        }

        @Override
        public void close() {
            if (vertexBuffer != 0) GL15.glDeleteBuffers(vertexBuffer);
            if (vertexArray != 0) GL30.glDeleteVertexArrays(vertexArray);
            vertexBuffer = vertexArray = 0;
            uploadBuffer = null;
        }
    }

    private static final class QuadTriangleConsumer implements VertexConsumer {
        private final List<RawVertex> vertices = new ArrayList<>();
        private final RawVertex[] quad = new RawVertex[4];
        private int quadSize;
        private double x;
        private double y;
        private double z;
        private int red = 255;
        private int green = 255;
        private int blue = 255;
        private int alpha = 255;
        private float u;
        private float v;
        private boolean defaultColor;
        private int defaultRed;
        private int defaultGreen;
        private int defaultBlue;
        private int defaultAlpha;

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
            if (defaultColor) {
                red = defaultRed;
                green = defaultGreen;
                blue = defaultBlue;
                alpha = defaultAlpha;
            }
            return this;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            this.red = Mth.clamp(red, 0, 255);
            this.green = Mth.clamp(green, 0, 255);
            this.blue = Mth.clamp(blue, 0, 255);
            this.alpha = Mth.clamp(alpha, 0, 255);
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            this.u = u;
            this.v = v;
            return this;
        }

        @Override public VertexConsumer overlayCoords(int u, int v) { return this; }
        @Override public VertexConsumer uv2(int u, int v) { return this; }
        @Override public VertexConsumer normal(float x, float y, float z) { return this; }

        @Override
        public void endVertex() {
            quad[quadSize++] = new RawVertex((float) x, (float) y, (float) z,
                    red, green, blue, alpha, u, v);
            if (quadSize == 4) {
                vertices.add(quad[0]);
                vertices.add(quad[1]);
                vertices.add(quad[2]);
                vertices.add(quad[0]);
                vertices.add(quad[2]);
                vertices.add(quad[3]);
                quadSize = 0;
            }
        }

        private void finish() {
            if (quadSize != 0) {
                throw new IllegalStateException("Valkyrie Impact Oculus pipeline emitted an incomplete quad: " + quadSize);
            }
        }

        @Override
        public void defaultColor(int red, int green, int blue, int alpha) {
            defaultColor = true;
            defaultRed = red;
            defaultGreen = green;
            defaultBlue = blue;
            defaultAlpha = alpha;
        }

        @Override
        public void unsetDefaultColor() {
            defaultColor = false;
        }
    }

    private record RawVertex(float x, float y, float z, int red, int green, int blue, int alpha, float u, float v) {
    }

    private static final class GlState {
        private final int previousProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        private final int previousFbo = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        private final int previousVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        private final int previousArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        private final int previousDrawBuffer = GL11.glGetInteger(GL11.GL_DRAW_BUFFER);
        private final boolean depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        private final boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        private final boolean cullFace = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        private final boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        private final int blendSrcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        private final int blendDstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        private final int blendSrcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        private final int blendDstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        private final int blendEquationRgb = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB);
        private final int blendEquationAlpha = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_ALPHA);
        private final int[] viewport = new int[4];

        private GlState() {
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        }

        private static GlState capture() {
            return new GlState();
        }

        private void restore() {
            GL20.glUseProgram(previousProgram);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, previousFbo);
            GL11.glDrawBuffer(previousDrawBuffer);
            GL30.glBindVertexArray(previousVao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);
            GL11.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
            if (depthTest) GL11.glEnable(GL11.GL_DEPTH_TEST); else GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(depthMask);
            if (cullFace) GL11.glEnable(GL11.GL_CULL_FACE); else GL11.glDisable(GL11.GL_CULL_FACE);
            if (blend) GL11.glEnable(GL11.GL_BLEND); else GL11.glDisable(GL11.GL_BLEND);
            GL14.glBlendFuncSeparate(blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha);
            GL20.glBlendEquationSeparate(blendEquationRgb, blendEquationAlpha);
        }
    }
}
