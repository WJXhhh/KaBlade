package com.wjx.kablade.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.wjx.kablade.Main;
import com.wjx.kablade.client.RaizanCleaveAnimation;
import com.wjx.kablade.client.shader.ShaderCompat;
import com.wjx.kablade.client.shader.SkillPostShaders;
import com.wjx.kablade.client.shader.SkillShaderTarget;
import com.wjx.kablade.config.KabladeClientConfig;
import com.wjx.kablade.entity.RaizanCleaveEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
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

/**
 * Frame-batched HDR presentation for every visible Raizan Cleave cast.
 *
 * <p>The effect geometry is rendered into a private RGBA16F color buffer and a second
 * white-hot glow buffer. Only the latter is blurred. Composition samples the original
 * scene unchanged outside the effect mask, so Raizan never changes global exposure or tint.</p>
 */
@Mod.EventBusSubscriber(modid = Main.MODID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RaizanCleavePostPipeline {
    private static final Map<Integer, QueuedCast> QUEUED = new LinkedHashMap<>();
    private static final RaizanFramebuffer FRAMEBUFFER = new RaizanFramebuffer();
    private static final ProgramSet OCULUS_PROGRAMS = new ProgramSet();
    private static final MeshDrawer OCULUS_MESH = new MeshDrawer();
    // BufferBuilder owns native memory.  Creating the old 786432-sized builder
    // twice per frame left allocation to the direct-buffer cleaner and eventually
    // exhausted native memory after repeated casts.  Keep one modest builder for
    // the lifetime of the client and flush it after every cast so multiplayer does
    // not make its peak capacity grow with the number of simultaneous skills.
    private static final BufferBuilder EFFECT_BUILDER = new BufferBuilder(131072);
    private static final MultiBufferSource.BufferSource EFFECT_BUFFER =
            MultiBufferSource.immediate(EFFECT_BUILDER);
    private static boolean disabledForSession;
    private static boolean resourcesDirty;
    private static boolean loggedActive;
    private static boolean loggedFailure;
    private static Boolean lastShaderPackTarget;
    private static boolean privateGeometryPass;

    private RaizanCleavePostPipeline() {
    }

    /** Queues this cast and suppresses the renderer's immediate effect pass. */
    public static boolean enqueue(RaizanCleaveEntity entity, float partialTick) {
        Entity owner = entity.level().getEntity(entity.getOwnerId());
        Entity anchor = owner != null ? owner : entity;
        Vec3 position = anchor.getPosition(partialTick);
        Vec3 relative = entity.getTargetAnchor().subtract(entity.position());
        float yaw = entity.getYRot() * Mth.DEG_TO_RAD;
        double cos = Mth.cos(yaw);
        double sin = Mth.sin(yaw);
        Vec3 localTarget = new Vec3(cos * relative.x + sin * relative.z,
                relative.y, -sin * relative.x + cos * relative.z);
        QUEUED.put(entity.getId(), new QueuedCast(entity, position,
                entity.getReferenceFrame(partialTick), entity.getYRot(), localTarget));
        return true;
    }

    public static void invalidateResources() {
        resourcesDirty = true;
        disabledForSession = false;
        loggedActive = false;
        loggedFailure = false;
    }

    /** True while the private HDR target owns the shader and framebuffer state. */
    public static boolean isPrivateGeometryPass() {
        return privateGeometryPass;
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS
                || QUEUED.isEmpty()) {
            return;
        }
        List<QueuedCast> casts = new ArrayList<>(QUEUED.values());
        QUEUED.clear();

        boolean oculusRequested = ShaderCompat.shouldUseOculusPostPath();
        SkillShaderTarget target = ShaderCompat.currentTarget();
        if (lastShaderPackTarget == null
                || lastShaderPackTarget.booleanValue() != target.shaderPackTarget()) {
            lastShaderPackTarget = target.shaderPackTarget();
            resourcesDirty = true;
            disabledForSession = false;
            loggedActive = false;
            loggedFailure = false;
        }
        if (disabledForSession) {
            renderFallbackSafely(event, casts);
            return;
        }
        if (!target.isComplete() || (oculusRequested && !target.shaderPackTarget())) {
            renderFallbackSafely(event, casts);
            return;
        }

        GlState state = GlState.capture();
        boolean failed = false;
        boolean memoryFailure = false;
        try {
            if (resourcesDirty) {
                FRAMEBUFFER.close();
                OCULUS_PROGRAMS.close();
                OCULUS_MESH.close();
                SkillPostShaders.resetRaizan();
                resourcesDirty = false;
            }
            RawDrawContext rawContext = null;
            if (target.shaderPackTarget()) {
                OCULUS_PROGRAMS.ensureLoaded();
                rawContext = new RawDrawContext(
                        new org.joml.Matrix4f(RenderSystem.getModelViewMatrix()),
                        new org.joml.Matrix4f(event.getProjectionMatrix()),
                        shaderGameTime(event.getPartialTick()));
            }
            FRAMEBUFFER.ensureAllocated(target.width(), target.height());
            FRAMEBUFFER.beginEffect(target);
            renderQueued(event, casts, rawContext);
            FRAMEBUFFER.beginGlow(target);
            renderQueued(event, casts, rawContext);
            FRAMEBUFFER.composite(target, blurPasses(), glowStrength(),
                    chromaticStrength(), flashScale());

            if (!loggedActive) {
                loggedActive = true;
                Main.LOGGER.info("Raizan Cleave HDR pipeline active: fbo={}, size={}x{}, quality={}, geometry={}",
                        target.framebufferId(), target.width(), target.height(),
                        KabladeClientConfig.RAIZAN_CLEAVE_QUALITY.get(),
                        target.shaderPackTarget() ? "private-opengl" : "shader-instance");
            }
        } catch (OutOfMemoryError error) {
            failed = true;
            memoryFailure = true;
            disabledForSession = true;
            if (!loggedFailure) {
                loggedFailure = true;
                Main.LOGGER.warn("Disabling the Raizan Cleave HDR path after a native-memory "
                        + "allocation failure; the next cast will use layered geometry.", error);
            }
        } catch (RuntimeException | IOException exception) {
            failed = true;
            disabledForSession = true;
            if (!loggedFailure) {
                loggedFailure = true;
                Main.LOGGER.warn("Disabling the Raizan Cleave HDR path for this session; "
                        + "using layered geometry fallback.", exception);
            }
        } finally {
            state.restore();
        }

        if (failed && !memoryFailure) {
            renderFallbackSafely(event, casts);
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            QUEUED.clear();
            invalidateResources();
        }
    }

    private static void renderQueued(RenderLevelStageEvent event, List<QueuedCast> casts,
                                     RawDrawContext rawContext) {
        if (rawContext != null) {
            renderRawCasts(event, casts, rawContext);
            return;
        }
        privateGeometryPass = true;
        try {
            renderCasts(event, casts, EFFECT_BUFFER);
        } finally {
            try {
                // Also clears a partially started batch if effect generation threw.
                EFFECT_BUFFER.endBatch();
            } finally {
                privateGeometryPass = false;
            }
        }
    }

    private static float shaderGameTime(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return 0.0F;
        }
        return (float) ((minecraft.level.getGameTime() + partialTick) % 24000L) / 24000.0F;
    }

    /** Oculus path: collect the same quads, convert them to explicit triangles, and
     * submit each material with a private GL program that Oculus cannot replace. */
    private static void renderRawCasts(RenderLevelStageEvent event, List<QueuedCast> casts,
                                       RawDrawContext context) {
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        RaizanCleaveAnimation.Animation animation = RaizanCleaveAnimation.INSTANCE.current();
        privateGeometryPass = true;
        try {
            for (QueuedCast cast : casts) {
                if (!cast.entity().isAlive()) {
                    continue;
                }
                poseStack.pushPose();
                try {
                    poseStack.translate(cast.position().x - camera.x,
                            cast.position().y - camera.y, cast.position().z - camera.z);
                    poseStack.mulPose(Axis.YP.rotationDegrees(-cast.yaw()));
                    RawGeometry geometry = new RawGeometry();
                    RaizanCleaveRenderer.renderEffects(cast.entity(), animation, cast.frame(),
                            cast.localTarget(), poseStack.last().pose(),
                            geometry.energy, geometry.lightning, geometry.heart,
                            geometry.particle, geometry.composite);
                    context.draw(Material.WEAPON_ENERGY, geometry.energy);
                    context.draw(Material.LIGHTNING, geometry.lightning);
                    context.draw(Material.HEART_SLASH, geometry.heart);
                    context.draw(Material.PARTICLE, geometry.particle);
                    context.draw(Material.COMPOSITE, geometry.composite);
                } finally {
                    poseStack.popPose();
                }
            }
        } finally {
            privateGeometryPass = false;
        }
    }

    private static void renderFallbackSafely(RenderLevelStageEvent event,
                                             List<QueuedCast> casts) {
        try {
            renderFallback(event, casts);
        } catch (OutOfMemoryError error) {
            // Dropping one visual frame is preferable to terminating the client.
            // The shared builder remains available for a later, less dense frame.
            if (!loggedFailure) {
                loggedFailure = true;
                Main.LOGGER.warn("Skipping a Raizan Cleave fallback frame after a "
                        + "native-memory allocation failure.", error);
            }
        }
    }

    private static void renderFallback(RenderLevelStageEvent event, List<QueuedCast> casts) {
        try {
            renderCasts(event, casts, EFFECT_BUFFER);
        } finally {
            EFFECT_BUFFER.endBatch();
        }
    }

    private static void renderCasts(RenderLevelStageEvent event, List<QueuedCast> casts,
                                    MultiBufferSource.BufferSource buffer) {
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        RaizanCleaveAnimation.Animation animation = RaizanCleaveAnimation.INSTANCE.current();
        for (QueuedCast cast : casts) {
            if (!cast.entity().isAlive()) {
                continue;
            }
            poseStack.pushPose();
            try {
                poseStack.translate(cast.position().x - camera.x,
                        cast.position().y - camera.y, cast.position().z - camera.z);
                poseStack.mulPose(Axis.YP.rotationDegrees(-cast.yaw()));
                RaizanCleaveRenderer.renderEffects(cast.entity(), animation, cast.frame(),
                        cast.localTarget(), poseStack.last().pose(), buffer);
            } finally {
                poseStack.popPose();
                // Bound native-buffer growth to the geometry of one cast instead
                // of accumulating every simultaneously visible cast in one batch.
                buffer.endBatch();
            }
        }
    }

    private static int blurPasses() {
        return switch (KabladeClientConfig.RAIZAN_CLEAVE_QUALITY.get()) {
            case LOW -> 1;
            case MEDIUM -> 2;
            case HIGH -> 4;
        };
    }

    private static float glowStrength() {
        float base = switch (KabladeClientConfig.RAIZAN_CLEAVE_QUALITY.get()) {
            case LOW -> 0.96F;
            case MEDIUM -> 1.22F;
            case HIGH -> 1.52F;
        };
        return KabladeClientConfig.RAIZAN_CLEAVE_REDUCED_FLASH.get() ? base * 0.68F : base;
    }

    private static float chromaticStrength() {
        if (KabladeClientConfig.RAIZAN_CLEAVE_REDUCED_FLASH.get()) {
            return 0.0F;
        }
        return switch (KabladeClientConfig.RAIZAN_CLEAVE_QUALITY.get()) {
            case LOW -> 0.0F;
            case MEDIUM -> 0.52F;
            case HIGH -> 1.0F;
        };
    }

    private static float flashScale() {
        return KabladeClientConfig.RAIZAN_CLEAVE_REDUCED_FLASH.get() ? 0.42F : 1.0F;
    }

    private record QueuedCast(RaizanCleaveEntity entity, Vec3 position,
                              float frame, float yaw, Vec3 localTarget) {
    }

    private enum Material {
        WEAPON_ENERGY("raizan_weapon_energy", "textures/effect/raizan_noise.png", true),
        LIGHTNING("raizan_lightning", "textures/effect/raizan_noise.png", true),
        HEART_SLASH("raizan_heart_slash", "textures/effect/raizan_slash_gradient.png", true),
        PARTICLE("raizan_particle", "textures/effect/raizan_particle_mask.png", true),
        COMPOSITE("raizan_composite", "textures/effect/raizan_slash_gradient.png", false);

        private final String shader;
        private final ResourceLocation texture;
        private final boolean additive;

        Material(String shader, String texture, boolean additive) {
            this.shader = shader;
            this.texture = ResourceLocation.fromNamespaceAndPath(Main.MODID, texture);
            this.additive = additive;
        }
    }

    private static final class RawGeometry {
        private final QuadTriangleConsumer energy = new QuadTriangleConsumer();
        private final QuadTriangleConsumer lightning = new QuadTriangleConsumer();
        private final QuadTriangleConsumer heart = new QuadTriangleConsumer();
        private final QuadTriangleConsumer particle = new QuadTriangleConsumer();
        private final QuadTriangleConsumer composite = new QuadTriangleConsumer();
    }

    private record RawDrawContext(org.joml.Matrix4f modelView,
                                  org.joml.Matrix4f projection,
                                  float gameTime) {
        private void draw(Material material, QuadTriangleConsumer geometry) {
            if (geometry.vertices.isEmpty()) {
                return;
            }
            GlProgram program = OCULUS_PROGRAMS.get(material);
            int texture = Minecraft.getInstance().getTextureManager()
                    .getTexture(material.texture).getId();
            program.apply(modelView, projection, gameTime, texture);

            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            GL11.glDepthMask(false);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glColorMask(true, true, true, true);
            GL11.glEnable(GL11.GL_BLEND);
            if (material.additive) {
                GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE,
                        GL11.GL_ZERO, GL11.GL_ONE);
            } else {
                GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                        GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            }
            GL20.glBlendEquationSeparate(GL14.GL_FUNC_ADD, GL14.GL_FUNC_ADD);
            OCULUS_MESH.draw(geometry.vertices);
        }
    }

    private static final class ProgramSet implements AutoCloseable {
        private final Map<Material, GlProgram> programs = new java.util.EnumMap<>(Material.class);

        private void ensureLoaded() throws IOException {
            if (!programs.isEmpty()) {
                return;
            }
            for (Material material : Material.values()) {
                programs.put(material, GlProgram.compile(
                        readShader(material.shader + ".vsh"),
                        readShader(material.shader + ".fsh"), material.shader));
            }
        }

        private GlProgram get(Material material) {
            GlProgram program = programs.get(material);
            if (program == null) {
                throw new IllegalStateException("Raizan Oculus program not loaded: " + material);
            }
            return program;
        }

        private static String readShader(String file) throws IOException {
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath(
                    Main.MODID, "shaders/core/" + file);
            try (var stream = Minecraft.getInstance().getResourceManager().open(location)) {
                return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            }
        }

        @Override
        public void close() {
            programs.values().forEach(GlProgram::close);
            programs.clear();
        }
    }

    private static final class GlProgram implements AutoCloseable {
        private static final FloatBuffer MATRIX = BufferUtils.createFloatBuffer(16);
        private final int id;
        private final int modelViewLocation;
        private final int projectionLocation;
        private final int colorModulatorLocation;
        private final int gameTimeLocation;
        private final int samplerLocation;

        private GlProgram(int id) {
            this.id = id;
            modelViewLocation = GL20.glGetUniformLocation(id, "ModelViewMat");
            projectionLocation = GL20.glGetUniformLocation(id, "ProjMat");
            colorModulatorLocation = GL20.glGetUniformLocation(id, "ColorModulator");
            gameTimeLocation = GL20.glGetUniformLocation(id, "GameTime");
            samplerLocation = GL20.glGetUniformLocation(id, "Sampler0");
        }

        private static GlProgram compile(String vertexSource, String fragmentSource,
                                         String name) {
            int vertex = compileShader(GL20.GL_VERTEX_SHADER, vertexSource, name);
            int fragment = compileShader(GL20.GL_FRAGMENT_SHADER, fragmentSource, name);
            int program = GL20.glCreateProgram();
            GL20.glAttachShader(program, vertex);
            GL20.glAttachShader(program, fragment);
            GL20.glBindAttribLocation(program, 0, "Position");
            GL20.glBindAttribLocation(program, 1, "Color");
            GL20.glBindAttribLocation(program, 2, "UV0");
            GL20.glLinkProgram(program);
            GL20.glDeleteShader(vertex);
            GL20.glDeleteShader(fragment);
            if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
                String log = GL20.glGetProgramInfoLog(program);
                GL20.glDeleteProgram(program);
                throw new IllegalStateException("Raizan Oculus program link failed ("
                        + name + "): " + log);
            }
            return new GlProgram(program);
        }

        private static int compileShader(int type, String source, String name) {
            int shader = GL20.glCreateShader(type);
            GL20.glShaderSource(shader, source);
            GL20.glCompileShader(shader);
            if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
                String log = GL20.glGetShaderInfoLog(shader);
                GL20.glDeleteShader(shader);
                throw new IllegalStateException("Raizan Oculus shader compile failed ("
                        + name + "): " + log);
            }
            return shader;
        }

        private void apply(org.joml.Matrix4f modelView, org.joml.Matrix4f projection,
                           float gameTime, int texture) {
            GL20.glUseProgram(id);
            uploadMatrix(modelViewLocation, modelView);
            uploadMatrix(projectionLocation, projection);
            if (colorModulatorLocation >= 0) {
                GL20.glUniform4f(colorModulatorLocation, 1.0F, 1.0F, 1.0F, 1.0F);
            }
            if (gameTimeLocation >= 0) {
                GL20.glUniform1f(gameTimeLocation, gameTime);
            }
            if (samplerLocation >= 0) {
                GL13.glActiveTexture(GL13.GL_TEXTURE0);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
                GL20.glUniform1i(samplerLocation, 0);
            }
        }

        private static void uploadMatrix(int location, org.joml.Matrix4f matrix) {
            if (location < 0) {
                return;
            }
            MATRIX.clear();
            matrix.get(MATRIX);
            GL20.glUniformMatrix4fv(location, false, MATRIX);
        }

        @Override
        public void close() {
            if (id != 0) {
                GL20.glDeleteProgram(id);
            }
        }
    }

    private static final class MeshDrawer {
        private static final int STRIDE = 24;
        private int vertexArray;
        private int vertexBuffer;
        private ByteBuffer uploadBuffer;

        private void draw(List<RawVertex> vertices) {
            if (vertexArray == 0) vertexArray = GL30.glGenVertexArrays();
            if (vertexBuffer == 0) vertexBuffer = GL15.glGenBuffers();
            int byteCount = vertices.size() * STRIDE;
            if (uploadBuffer == null || uploadBuffer.capacity() < byteCount) {
                int capacity = 4096;
                while (capacity < byteCount) capacity <<= 1;
                uploadBuffer = BufferUtils.createByteBuffer(capacity);
            }
            ByteBuffer data = uploadBuffer;
            data.clear();
            for (RawVertex vertex : vertices) {
                data.putFloat(vertex.x).putFloat(vertex.y).putFloat(vertex.z);
                data.put((byte) vertex.red).put((byte) vertex.green)
                        .put((byte) vertex.blue).put((byte) vertex.alpha);
                data.putFloat(vertex.u).putFloat(vertex.v);
            }
            data.flip();

            GL30.glBindVertexArray(vertexArray);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vertexBuffer);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, data, GL15.GL_STREAM_DRAW);
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

    /** Converts renderer quads to explicit triangles before Oculus sees them. */
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
            this.x = x; this.y = y; this.z = z;
            if (defaultColor) {
                red = defaultRed; green = defaultGreen;
                blue = defaultBlue; alpha = defaultAlpha;
            }
            return this;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            this.red = red; this.green = green; this.blue = blue; this.alpha = alpha;
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            this.u = u; this.v = v;
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
                vertices.add(quad[0]); vertices.add(quad[1]); vertices.add(quad[2]);
                vertices.add(quad[0]); vertices.add(quad[2]); vertices.add(quad[3]);
                quadSize = 0;
            }
        }

        @Override
        public void defaultColor(int red, int green, int blue, int alpha) {
            defaultColor = true;
            defaultRed = red; defaultGreen = green;
            defaultBlue = blue; defaultAlpha = alpha;
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

    private static final class RaizanFramebuffer implements AutoCloseable {
        private final IntBuffer drawBuffer = BufferUtils.createIntBuffer(1);
        private final FloatBuffer transparent = BufferUtils.createFloatBuffer(4);
        private int geometryFbo;
        private int postFbo;
        private int effectTexture;
        private int glowTexture;
        private int bloomTexture;
        private int blurTexture;
        private int sceneTexture;
        private int width;
        private int height;

        private void ensureAllocated(int nextWidth, int nextHeight) {
            if (geometryFbo == 0) geometryFbo = GL30.glGenFramebuffers();
            if (postFbo == 0) postFbo = GL30.glGenFramebuffers();
            if (effectTexture == 0) effectTexture = GL11.glGenTextures();
            if (glowTexture == 0) glowTexture = GL11.glGenTextures();
            if (bloomTexture == 0) bloomTexture = GL11.glGenTextures();
            if (blurTexture == 0) blurTexture = GL11.glGenTextures();
            if (sceneTexture == 0) sceneTexture = GL11.glGenTextures();
            if (width == nextWidth && height == nextHeight) return;
            width = nextWidth;
            height = nextHeight;
            allocate(effectTexture, GL30.GL_RGBA16F, GL11.GL_FLOAT);
            allocate(glowTexture, GL30.GL_RGBA16F, GL11.GL_FLOAT);
            allocate(bloomTexture, GL30.GL_RGBA16F, GL11.GL_FLOAT);
            allocate(blurTexture, GL30.GL_RGBA16F, GL11.GL_FLOAT);
            // Oculus shader packs commonly keep the live scene in a floating-point
            // HDR attachment.  An RGBA8 scene copy clamps that exposure before the
            // fullscreen composite and makes the entire frame look grey.  A float
            // copy preserves both vanilla LDR and shader-pack HDR values losslessly.
            allocate(sceneTexture, GL30.GL_RGBA16F, GL11.GL_FLOAT);
        }

        private void beginEffect(SkillShaderTarget target) {
            bindGeometry(effectTexture, target.depthTextureId());
            clearColor();
        }

        private void beginGlow(SkillShaderTarget target) {
            bindGeometry(glowTexture, target.depthTextureId());
            clearColor();
        }

        private void composite(SkillShaderTarget target, int passes, float glowStrength,
                               float chromaticStrength, float flashScale) {
            copyScene(target);
            blurGlow(passes);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, target.framebufferId());
            setDrawAttachment();
            GL11.glViewport(0, 0, target.width(), target.height());
            SkillPostShaders.compositeRaizan(sceneTexture, effectTexture, glowTexture,
                    bloomTexture, width, height, glowStrength, chromaticStrength, flashScale);
        }

        private void bindGeometry(int color, int depth) {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, geometryFbo);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                    GL11.GL_TEXTURE_2D, color, 0);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT,
                    GL11.GL_TEXTURE_2D, depth, 0);
            setDrawAttachment();
            check("Raizan geometry");
            GL11.glViewport(0, 0, width, height);
        }

        private void copyScene(SkillShaderTarget target) {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, geometryFbo);
            GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                    GL11.GL_TEXTURE_2D, target.colorTextureId(), 0);
            GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, postFbo);
            GL30.glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                    GL11.GL_TEXTURE_2D, sceneTexture, 0);
            setDrawAttachment();
            check("Raizan scene copy");
            GL30.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height,
                    GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        }

        private void blurGlow(int passes) {
            int source = glowTexture;
            for (int i = 0; i < passes; i++) {
                bindPost(blurTexture);
                SkillPostShaders.blurBloodfyre(source, width, height, true);
                bindPost(bloomTexture);
                SkillPostShaders.blurBloodfyre(blurTexture, width, height, false);
                source = bloomTexture;
            }
        }

        private void bindPost(int texture) {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, postFbo);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                    GL11.GL_TEXTURE_2D, texture, 0);
            setDrawAttachment();
            check("Raizan blur");
            GL11.glViewport(0, 0, width, height);
        }

        private void allocate(int texture, int internalFormat, int type) {
            int previous = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL30.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL30.GL_CLAMP_TO_EDGE);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, internalFormat, width, height,
                    0, GL11.GL_RGBA, type, 0L);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, previous);
        }

        private void clearColor() {
            transparent.clear();
            transparent.put(0.0F).put(0.0F).put(0.0F).put(0.0F).flip();
            GL30.glClearBufferfv(GL11.GL_COLOR, 0, transparent);
        }

        private void setDrawAttachment() {
            drawBuffer.clear();
            drawBuffer.put(GL30.GL_COLOR_ATTACHMENT0).flip();
            GL20.glDrawBuffers(drawBuffer);
        }

        @Override
        public void close() {
            deleteTexture(effectTexture);
            deleteTexture(glowTexture);
            deleteTexture(bloomTexture);
            deleteTexture(blurTexture);
            deleteTexture(sceneTexture);
            effectTexture = glowTexture = bloomTexture = blurTexture = sceneTexture = 0;
            if (geometryFbo != 0) GL30.glDeleteFramebuffers(geometryFbo);
            if (postFbo != 0) GL30.glDeleteFramebuffers(postFbo);
            geometryFbo = postFbo = width = height = 0;
        }

        private static void deleteTexture(int texture) {
            if (texture != 0) GL11.glDeleteTextures(texture);
        }

        private static void check(String stage) {
            int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
            if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
                throw new IllegalStateException(stage + " framebuffer incomplete: 0x"
                        + Integer.toHexString(status));
            }
        }
    }

    private record GlState(int drawFramebuffer, int readFramebuffer,
                           int drawBuffer, int readBuffer, int program,
                           int vertexArray, int arrayBuffer, int activeTexture,
                           int[] textures, int[] viewport, boolean blend,
                           boolean depth, boolean cull, boolean depthMask,
                           int depthFunc, int blendSrcRgb, int blendDstRgb,
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
