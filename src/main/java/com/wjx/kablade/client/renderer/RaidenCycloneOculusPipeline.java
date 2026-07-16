package com.wjx.kablade.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wjx.kablade.Main;
import com.wjx.kablade.client.KabladeRenderTypes;
import com.wjx.kablade.client.shader.OculusFramebufferAccess;
import com.wjx.kablade.client.shader.ShaderCompat;
import com.wjx.kablade.client.shader.SkillPostShaders;
import com.wjx.kablade.client.shader.SkillShaderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
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
import java.util.List;
import java.util.Optional;

/**
 * Full-detail Oculus path for Raiden's Cyclone.
 *
 * <p>Minecraft {@code ShaderInstance} RenderTypes are deliberately not used here. Oculus may
 * replace those programs and reinterpret textured quads, which produced yellow rectangles and
 * diagonal triangles. This path collects the existing analytic geometry once, converts every
 * quad to explicit triangles, and draws it with a private program into HDR targets sharing the
 * live Oculus depth texture.</p>
 */
@Mod.EventBusSubscriber(modid = Main.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RaidenCycloneOculusPipeline {
    private static final int BLOOM_BLUR_PASSES = 5;
    private static final RaidenFramebuffer FRAMEBUFFER = new RaidenFramebuffer();
    private static final RaidenProgram PROGRAM = new RaidenProgram();
    private static final MeshDrawer MESH_DRAWER = new MeshDrawer();

    private static boolean resourcesDirty;
    private static boolean disabledForSession;
    private static boolean loggedMissingTarget;
    private static boolean loggedFailure;
    private static boolean loggedActive;

    private RaidenCycloneOculusPipeline() {
    }

    @FunctionalInterface
    public interface GeometryEmitter {
        void emit(VertexConsumer bright, VertexConsumer dark);
    }

    /** Draws one complete frame. Returns false only when the normal renderer should be used. */
    public static boolean render(RenderLevelStageEvent event, GeometryEmitter emitter) {
        if (!ShaderCompat.shouldUseOculusPostPath()) {
            return false;
        }

        GeometryBatch batch = GeometryBatch.collect(emitter);
        if (batch.isEmpty()) {
            return true;
        }

        if (disabledForSession) {
            renderSafeFallback(batch);
            return true;
        }

        Optional<SkillShaderTarget> resolved = OculusFramebufferAccess.findTranslucentTarget();
        if (resolved.isEmpty() || !resolved.get().isComplete()) {
            if (!loggedMissingTarget) {
                loggedMissingTarget = true;
                Main.LOGGER.warn("Raiden Cyclone Oculus renderer could not resolve a complete "
                        + "translucent target; using explicit-triangle fallback.");
            }
            renderSafeFallback(batch);
            return true;
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
            FRAMEBUFFER.ensureAllocated(target.width(), target.height());

            if (!loggedActive) {
                loggedActive = true;
                Main.LOGGER.info("Raiden Cyclone Oculus renderer active: fbo={}, color={}, "
                                + "depth={}, size={}x{}, brightTriangles={}, darkTriangles={}",
                        target.framebufferId(), target.colorTextureId(), target.depthTextureId(),
                        target.width(), target.height(), batch.bright.size() / 3,
                        batch.dark.size() / 3);
            }

            Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewMatrix());
            Matrix4f projection = new Matrix4f(event.getProjectionMatrix());
            float gameTime = shaderGameTime(event.getPartialTick());

            FRAMEBUFFER.beginEffect(target);
            PROGRAM.apply(modelView, projection, gameTime, false);
            applyAlphaBlend();
            MESH_DRAWER.draw(batch.dark);
            PROGRAM.apply(modelView, projection, gameTime, false);
            applyAdditiveBlend(true);
            MESH_DRAWER.draw(batch.bright);

            FRAMEBUFFER.beginGlow(target);
            PROGRAM.apply(modelView, projection, gameTime, true);
            applyAdditiveBlend(false);
            MESH_DRAWER.draw(batch.bright);

            FRAMEBUFFER.composite(target);
        } catch (RuntimeException | IOException exception) {
            failed = true;
            disabledForSession = true;
            if (!loggedFailure) {
                loggedFailure = true;
                Main.LOGGER.warn("Disabling the Raiden Cyclone Oculus renderer for this "
                        + "session; using explicit-triangle fallback.", exception);
            }
        } finally {
            state.restore();
        }

        if (failed) {
            renderSafeFallback(batch);
        }
        return true;
    }

    /** Marks direct GL resources stale after resource or shader-pack reload. */
    public static void invalidateResources() {
        resourcesDirty = true;
        disabledForSession = false;
        loggedMissingTarget = false;
        loggedFailure = false;
        loggedActive = false;
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            invalidateResources();
        }
    }

    private static float shaderGameTime(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return 0.0F;
        return (float) ((minecraft.level.getGameTime() + partialTick) % 24000.0D) / 24000.0F;
    }

    private static void applyAlphaBlend() {
        GL11.glEnable(GL11.GL_BLEND);
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL20.glBlendEquationSeparate(GL14.GL_FUNC_ADD, GL14.GL_FUNC_ADD);
    }

    private static void applyAdditiveBlend(boolean preserveAlpha) {
        GL11.glEnable(GL11.GL_BLEND);
        if (preserveAlpha) {
            GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE,
                    GL11.GL_ZERO, GL11.GL_ONE);
        } else {
            GL14.glBlendFuncSeparate(GL11.GL_ONE, GL11.GL_ONE,
                    GL11.GL_ONE, GL11.GL_ONE);
        }
        GL20.glBlendEquationSeparate(GL14.GL_FUNC_ADD, GL14.GL_FUNC_ADD);
    }

    private static void renderSafeFallback(GeometryBatch batch) {
        MultiBufferSource.BufferSource immediate = MultiBufferSource.immediate(new BufferBuilder(524288));
        VertexConsumer dark = immediate.getBuffer(KabladeRenderTypes.raidenCycloneShaderPackDark());
        emitPositionColor(dark, batch.dark);
        immediate.endBatch(KabladeRenderTypes.raidenCycloneShaderPackDark());
        VertexConsumer bright = immediate.getBuffer(KabladeRenderTypes.raidenCycloneShaderPackBright());
        emitPositionColor(bright, batch.bright);
        immediate.endBatch(KabladeRenderTypes.raidenCycloneShaderPackBright());
    }

    private static void emitPositionColor(VertexConsumer output, List<RawVertex> vertices) {
        for (RawVertex vertex : vertices) {
            output.vertex(vertex.x, vertex.y, vertex.z)
                    .color(vertex.red, vertex.green, vertex.blue, vertex.alpha)
                    .endVertex();
        }
    }

    private static void closeResources() {
        PROGRAM.close();
        FRAMEBUFFER.close();
        MESH_DRAWER.close();
    }

    private static final class GeometryBatch {
        private final List<RawVertex> bright;
        private final List<RawVertex> dark;

        private GeometryBatch(List<RawVertex> bright, List<RawVertex> dark) {
            this.bright = bright;
            this.dark = dark;
        }

        private static GeometryBatch collect(GeometryEmitter emitter) {
            QuadTriangleConsumer bright = new QuadTriangleConsumer();
            QuadTriangleConsumer dark = new QuadTriangleConsumer();
            emitter.emit(bright, dark);
            bright.finish();
            dark.finish();
            return new GeometryBatch(bright.vertices, dark.vertices);
        }

        private boolean isEmpty() {
            return bright.isEmpty() && dark.isEmpty();
        }
    }

    private static final class RaidenProgram implements AutoCloseable {
        private static final FloatBuffer MATRIX = BufferUtils.createFloatBuffer(16);
        private int id;
        private int modelViewLocation;
        private int projectionLocation;
        private int gameTimeLocation;
        private int passModeLocation;

        private void ensureLoaded() throws IOException {
            if (id != 0) return;
            String vertexSource = readShader("raiden_cyclone_oculus.vsh");
            String fragmentSource = readShader("raiden_cyclone_oculus.fsh");
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
                GL20.glDeleteProgram(id);
                id = 0;
                throw new IllegalStateException("Raiden Cyclone Oculus program link failed: " + log);
            }
            modelViewLocation = GL20.glGetUniformLocation(id, "ModelViewMat");
            projectionLocation = GL20.glGetUniformLocation(id, "ProjMat");
            gameTimeLocation = GL20.glGetUniformLocation(id, "GameTime");
            passModeLocation = GL20.glGetUniformLocation(id, "PassMode");
        }

        private void apply(Matrix4f modelView, Matrix4f projection,
                           float gameTime, boolean maskPass) {
            GL20.glUseProgram(id);
            uploadMatrix(modelViewLocation, modelView);
            uploadMatrix(projectionLocation, projection);
            if (gameTimeLocation >= 0) GL20.glUniform1f(gameTimeLocation, gameTime);
            if (passModeLocation >= 0) GL20.glUniform1i(passModeLocation, maskPass ? 1 : 0);
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
                throw new IllegalStateException("Raiden Cyclone Oculus shader compile failed: " + log);
            }
            return shader;
        }

        private static void uploadMatrix(int location, Matrix4f matrix) {
            if (location < 0) return;
            MATRIX.clear();
            matrix.get(MATRIX);
            GL20.glUniformMatrix4fv(location, false, MATRIX);
        }

        @Override
        public void close() {
            if (id != 0) GL20.glDeleteProgram(id);
            id = 0;
        }
    }

    private static final class RaidenFramebuffer implements AutoCloseable {
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

        private void beginEffect(SkillShaderTarget target) {
            bindGeometryTarget(effectTexture, target.depthTextureId(), "effect");
            clearColor();
            prepareGeometryState();
        }

        private void beginGlow(SkillShaderTarget target) {
            bindGeometryTarget(glowTexture, target.depthTextureId(), "glow");
            clearColor();
            prepareGeometryState();
        }

        private void bindGeometryTarget(int colorTexture, int depthTexture, String stage) {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, effectFramebuffer);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                    GL11.GL_TEXTURE_2D, colorTexture, 0);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT,
                    GL11.GL_TEXTURE_2D, depthTexture, 0);
            GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
            checkFramebuffer("Raiden Cyclone " + stage);
            GL11.glViewport(0, 0, width, height);
        }

        private void prepareGeometryState() {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            GL11.glDepthMask(false);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glColorMask(true, true, true, true);
        }

        private void clearColor() {
            transparent.clear();
            transparent.put(0.0F).put(0.0F).put(0.0F).put(0.0F).flip();
            GL30.glClearBufferfv(GL11.GL_COLOR, 0, transparent);
        }

        private void composite(SkillShaderTarget target) {
            copyScene(target);
            int bloomTexture = blurGlow();
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, target.framebufferId());
            GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
            GL11.glViewport(0, 0, width, height);
            SkillPostShaders.compositeRaiden(sceneTexture, effectTexture,
                    glowTexture, bloomTexture, width, height);
        }

        private void copyScene(SkillShaderTarget target) {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, target.framebufferId());
            GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, copyFramebuffer);
            GL30.glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                    GL11.GL_TEXTURE_2D, sceneTexture, 0);
            GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
            checkDrawFramebuffer("Raiden Cyclone scene copy");
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
            checkFramebuffer("Raiden Cyclone bloom");
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

    private static final class MeshDrawer {
        private static final int STRIDE = 24;
        private int vertexArray;
        private int vertexBuffer;
        private ByteBuffer uploadBuffer;

        private void draw(List<RawVertex> vertices) {
            if (vertices.isEmpty()) return;
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

        private void close() {
            if (vertexBuffer != 0) GL15.glDeleteBuffers(vertexBuffer);
            if (vertexArray != 0) GL30.glDeleteVertexArrays(vertexArray);
            vertexBuffer = vertexArray = 0;
            uploadBuffer = null;
        }
    }

    /** Converts each logical quad to two explicit triangles before Oculus can inspect it. */
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
                throw new IllegalStateException("Raiden Cyclone emitted an incomplete quad: " + quadSize);
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

    private record RawVertex(float x, float y, float z,
                             int red, int green, int blue, int alpha,
                             float u, float v) {
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
                    GL11.glGetInteger(GL11.GL_DRAW_BUFFER),
                    GL11.glGetInteger(GL11.GL_READ_BUFFER),
                    GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM),
                    GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING),
                    GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING),
                    active, textures, viewport,
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
