package com.wjx.kablade.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.wjx.kablade.Main;
import com.wjx.kablade.client.shader.OculusFramebufferAccess;
import com.wjx.kablade.client.shader.ShaderCompat;
import com.wjx.kablade.client.shader.SkillPostShaders;
import com.wjx.kablade.client.shader.SkillShaderTarget;
import com.wjx.kablade.entity.BloodfyreFrenzyEntity;
import com.wjx.kablade.entity.SwordEnlightenmentEntity;
import com.wjx.kablade.entity.UtpalaAuraEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.level.LevelEvent;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Oculus-safe frame renderer for Bloodfyre Frenzy.
 *
 * <p>The normal renderer uses Minecraft {@code ShaderInstance}s. Iris/Oculus replaces the
 * entity program around those RenderTypes, so this path renders the same analytic geometry
 * with private GL programs into an HDR effect target that shares Oculus' live depth texture.
 * Every queued instance is then composited once per frame.</p>
 */
@Mod.EventBusSubscriber(modid = Main.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BloodfyreOculusPipeline {
    private static final Map<Integer, QueuedEffect> QUEUED = new LinkedHashMap<>();
    private static final Map<Integer, QueuedUtpala> UTPALA_QUEUED = new LinkedHashMap<>();
    private static final Map<Integer, QueuedSword> SWORD_QUEUED = new LinkedHashMap<>();
    private static final BloodfyreFramebuffer FRAMEBUFFER = new BloodfyreFramebuffer(false);
    private static final BloodfyreFramebuffer HONKAI_FRAMEBUFFER = new BloodfyreFramebuffer(true);
    private static final ProgramSet PROGRAMS = new ProgramSet();
    private static final MeshDrawer MESH_DRAWER = new MeshDrawer();
    private static final int BLOOM_BLUR_PASSES = 5;

    private static boolean resourcesDirty;
    private static boolean disabledForSession;
    private static boolean loggedMissingTarget;
    private static boolean loggedFailure;
    private static boolean loggedActive;
    private static boolean privateGeometryPass;

    private BloodfyreOculusPipeline() {
    }

    /** Returns true when the normal entity render should be suppressed for this frame. */
    public static boolean enqueue(BloodfyreFrenzyEntity entity, float partialTick) {
        if (!ShaderCompat.shouldUseOculusPostPath()) {
            return false;
        }
        double x = Mth.lerp(partialTick, entity.xOld, entity.getX());
        double y = Mth.lerp(partialTick, entity.yOld, entity.getY());
        double z = Mth.lerp(partialTick, entity.zOld, entity.getZ());
        QUEUED.put(entity.getId(), new QueuedEffect(
                entity, x, y, z, entity.getYRot(), entity.getRenderAge(partialTick),
                entity.getId() * 31 + 0x5F3759DF));
        return true;
    }

    /** Queues Frozen Naraka without exposing its analytic quads to Oculus' entity programs. */
    public static boolean enqueue(UtpalaAuraEntity entity, float partialTick) {
        if (!ShaderCompat.shouldUseOculusPostPath()) {
            return false;
        }
        UTPALA_QUEUED.put(entity.getId(), new QueuedUtpala(
                entity,
                Mth.lerp(partialTick, entity.xOld, entity.getX()),
                Mth.lerp(partialTick, entity.yOld, entity.getY()),
                Mth.lerp(partialTick, entity.zOld, entity.getZ()),
                Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot()),
                entity.tickCount + partialTick));
        return true;
    }

    /** Queues Sword Enlightenment for the same private full-detail HDR path. */
    public static boolean enqueue(SwordEnlightenmentEntity entity, float partialTick) {
        if (!ShaderCompat.shouldUseOculusPostPath()) {
            return false;
        }
        SWORD_QUEUED.put(entity.getId(), new QueuedSword(
                entity,
                Mth.lerp(partialTick, entity.xOld, entity.getX()),
                Mth.lerp(partialTick, entity.yOld, entity.getY()),
                Mth.lerp(partialTick, entity.zOld, entity.getZ()),
                Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot()),
                entity.tickCount + partialTick));
        return true;
    }

    /** True only while a private program is collecting the original UV-rich geometry. */
    static boolean isPrivateGeometryPass() {
        return privateGeometryPass;
    }

    /** Mark direct GL programs/FBOs stale after a shader or resource reload. */
    public static void invalidateResources() {
        resourcesDirty = true;
        disabledForSession = false;
        loggedFailure = false;
        loggedMissingTarget = false;
        loggedActive = false;
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS
                || (QUEUED.isEmpty() && UTPALA_QUEUED.isEmpty() && SWORD_QUEUED.isEmpty())) {
            return;
        }

        List<QueuedEffect> effects = new ArrayList<>(QUEUED.values());
        List<QueuedUtpala> utpalaEffects = new ArrayList<>(UTPALA_QUEUED.values());
        List<QueuedSword> swordEffects = new ArrayList<>(SWORD_QUEUED.values());
        QUEUED.clear();
        UTPALA_QUEUED.clear();
        SWORD_QUEUED.clear();
        if (disabledForSession || !ShaderCompat.shouldUseOculusPostPath()) {
            renderFallback(event, effects);
            renderHonkaiFallback(event, utpalaEffects, swordEffects);
            return;
        }

        Optional<SkillShaderTarget> resolved = OculusFramebufferAccess.findTranslucentTarget();
        if (resolved.isEmpty() || !resolved.get().isComplete()) {
            if (!loggedMissingTarget) {
                loggedMissingTarget = true;
                Main.LOGGER.warn("Bloodfyre Oculus renderer could not resolve a complete translucent target; using safe geometry fallback.");
            }
            renderFallback(event, effects);
            renderHonkaiFallback(event, utpalaEffects, swordEffects);
            return;
        }

        SkillShaderTarget target = resolved.get();
        if (!loggedActive) {
            loggedActive = true;
            Main.LOGGER.info("Bloodfyre Oculus renderer active: fbo={}, color={}, depth={}, size={}x{}",
                    target.framebufferId(), target.colorTextureId(), target.depthTextureId(),
                    target.width(), target.height());
        }
        GlState state = GlState.capture();
        boolean failed = false;
        try {
            if (resourcesDirty) {
                PROGRAMS.close();
                FRAMEBUFFER.close();
                HONKAI_FRAMEBUFFER.close();
                MESH_DRAWER.close();
                resourcesDirty = false;
            }
            PROGRAMS.ensureLoaded();
            float gameTime = shaderGameTime(event.getPartialTick());
            DrawContext context = new DrawContext(
                    new Matrix4f(RenderSystem.getModelViewMatrix()),
                    new Matrix4f(event.getProjectionMatrix()), gameTime);

            if (!effects.isEmpty()) {
                FRAMEBUFFER.ensureAllocated(target.width(), target.height());
                FRAMEBUFFER.beginEffect(target);
                renderQueued(event, effects, context, false);
                FRAMEBUFFER.beginMask(target);
                renderQueued(event, effects, context, true);
                FRAMEBUFFER.composite(target);
            }

            if (!utpalaEffects.isEmpty() || !swordEffects.isEmpty()) {
                HONKAI_FRAMEBUFFER.ensureAllocated(target.width(), target.height());
                HONKAI_FRAMEBUFFER.beginEffect(target);
                renderHonkaiQueued(event, utpalaEffects, swordEffects, context, false);
                HONKAI_FRAMEBUFFER.beginMask(target);
                renderHonkaiQueued(event, utpalaEffects, swordEffects, context, true);
                HONKAI_FRAMEBUFFER.composite(target);
            }
        } catch (RuntimeException | IOException exception) {
            failed = true;
            disabledForSession = true;
            if (!loggedFailure) {
                loggedFailure = true;
                Main.LOGGER.warn("Disabling the Bloodfyre Oculus renderer for this session; using safe fallback.", exception);
            }
        } finally {
            state.restore();
        }

        if (failed) {
            renderFallback(event, effects);
            renderHonkaiFallback(event, utpalaEffects, swordEffects);
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            QUEUED.clear();
            UTPALA_QUEUED.clear();
            SWORD_QUEUED.clear();
            invalidateResources();
        }
    }

    private static float shaderGameTime(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return 0.0F;
        }
        // Matches Minecraft's GameTime uniform: day time expressed as a 0..1 day fraction.
        return (float) ((minecraft.level.getGameTime() + partialTick) % 24000L) / 24000.0F;
    }

    private static void renderQueued(RenderLevelStageEvent event, List<QueuedEffect> effects,
                                     DrawContext context, boolean glowMask) {
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        for (QueuedEffect effect : effects) {
            if (!effect.entity().isAlive() || effect.age() >= BloodfyreFrenzyEntity.LIFETIME) {
                continue;
            }
            poseStack.pushPose();
            poseStack.translate(effect.x() - camera.x, effect.y() - camera.y, effect.z() - camera.z);
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - effect.yaw()));
            if (glowMask) {
                BloodfyreFrenzyRenderer.renderOculusGlow(
                        effect.entity(), poseStack, context, effect.age(), effect.seed());
            } else {
                BloodfyreFrenzyRenderer.renderOculusColor(
                        effect.entity(), poseStack, context, effect.age(), effect.seed());
            }
            poseStack.popPose();
        }
    }

    private static void renderHonkaiQueued(RenderLevelStageEvent event,
                                           List<QueuedUtpala> utpalaEffects,
                                           List<QueuedSword> swordEffects,
                                           DrawContext context, boolean glowMask) {
        Vec3 camera = event.getCamera().getPosition();
        org.joml.Vector3f cameraLeft = event.getCamera().getLeftVector();
        org.joml.Vector3f cameraUp = event.getCamera().getUpVector();
        PoseStack poseStack = event.getPoseStack();

        for (QueuedUtpala effect : utpalaEffects) {
            if (!effect.entity().isAlive() || effect.age() >= effect.entity().getLifetime() + 2.0F) {
                continue;
            }
            poseStack.pushPose();
            poseStack.translate(effect.x() - camera.x, effect.y() - camera.y, effect.z() - camera.z);
            if (glowMask) {
                UtpalaAuraRenderer.renderOculusGlow(poseStack, context, effect.age(), effect.yaw());
            } else {
                UtpalaAuraRenderer.renderOculusColor(poseStack, context, effect.age(), effect.yaw());
            }
            poseStack.popPose();
        }

        for (QueuedSword effect : swordEffects) {
            if (!effect.entity().isAlive() || effect.age() >= effect.entity().getLifetime() + 2.0F) {
                continue;
            }
            poseStack.pushPose();
            poseStack.translate(effect.x() - camera.x, effect.y() - camera.y, effect.z() - camera.z);
            if (glowMask) {
                SwordEnlightenmentRenderer.renderOculusGlow(
                        poseStack, context, effect.age(), effect.yaw(), cameraLeft, cameraUp);
            } else {
                SwordEnlightenmentRenderer.renderOculusColor(
                        poseStack, context, effect.age(), effect.yaw(), cameraLeft, cameraUp);
            }
            poseStack.popPose();
        }
    }

    private static void renderFallback(RenderLevelStageEvent event, List<QueuedEffect> effects) {
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource immediate = MultiBufferSource.immediate(new BufferBuilder(262144));
        for (QueuedEffect effect : effects) {
            if (!effect.entity().isAlive() || effect.age() >= BloodfyreFrenzyEntity.LIFETIME) {
                continue;
            }
            poseStack.pushPose();
            poseStack.translate(effect.x() - camera.x, effect.y() - camera.y, effect.z() - camera.z);
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - effect.yaw()));
            BloodfyreFrenzyRenderer.renderShaderPackFallback(
                    effect.entity(), immediate, poseStack.last().pose(), effect.age(), effect.seed());
            poseStack.popPose();
        }
        immediate.endBatch();
    }

    private static void renderHonkaiFallback(RenderLevelStageEvent event,
                                             List<QueuedUtpala> utpalaEffects,
                                             List<QueuedSword> swordEffects) {
        if (utpalaEffects.isEmpty() && swordEffects.isEmpty()) {
            return;
        }
        MultiBufferSource.BufferSource immediate = MultiBufferSource.immediate(new BufferBuilder(524288));
        DrawContext context = DrawContext.safeFallback(immediate);
        renderHonkaiQueued(event, utpalaEffects, swordEffects, context, false);
        immediate.endBatch();
    }

    public enum AnalyticShader {
        FRENZY("bloodfyre_frenzy"),
        RUPTURE("bloodfyre_rupture"),
        SMOKE("bloodfyre_smoke"),
        SCAR("bloodfyre_scar"),
        PARTICLE("bloodfyre_particle"),
        UTPALA("utpala_aura"),
        SWORD_ENLIGHTENMENT("sword_enlightenment");

        private final String name;

        AnalyticShader(String name) {
            this.name = name;
        }
    }

    public enum BlendMode {
        ALPHA,
        ADDITIVE
    }

    /** Drawing facade used by the shared Bloodfyre geometry generator. */
    public static final class DrawContext {
        private final Matrix4f modelView;
        private final Matrix4f projection;
        private final float gameTime;
        private final MultiBufferSource.BufferSource fallback;

        private DrawContext(Matrix4f modelView, Matrix4f projection, float gameTime) {
            this.modelView = modelView;
            this.projection = projection;
            this.gameTime = gameTime;
            this.fallback = null;
        }

        private DrawContext(MultiBufferSource.BufferSource fallback) {
            this.modelView = null;
            this.projection = null;
            this.gameTime = 0.0F;
            this.fallback = fallback;
        }

        private static DrawContext safeFallback(MultiBufferSource.BufferSource fallback) {
            return new DrawContext(fallback);
        }

        public void draw(AnalyticShader shader, BlendMode blend,
                         Consumer<VertexConsumer> geometry) {
            QuadTriangleConsumer consumer = new QuadTriangleConsumer();
            boolean previousPass = privateGeometryPass;
            privateGeometryPass = true;
            try {
                geometry.accept(consumer);
            } finally {
                privateGeometryPass = previousPass;
            }
            if (consumer.vertices.isEmpty()) {
                return;
            }

            if (fallback != null) {
                VertexConsumer output = fallback.getBuffer(safeRenderType(shader, blend));
                emitPositionColor(output, consumer.vertices);
                return;
            }

            GlProgram program = PROGRAMS.get(shader);
            program.apply(modelView, projection, gameTime);
            applyBlend(blend);
            MESH_DRAWER.draw(consumer.vertices);
        }

        private static net.minecraft.client.renderer.RenderType safeRenderType(
                AnalyticShader shader, BlendMode blend) {
            return switch (shader) {
                case UTPALA -> blend == BlendMode.ALPHA
                        ? com.wjx.kablade.client.KabladeRenderTypes.utpalaOculusSafeAlpha()
                        : com.wjx.kablade.client.KabladeRenderTypes.utpalaOculusSafeAdditive();
                case SWORD_ENLIGHTENMENT ->
                        com.wjx.kablade.client.KabladeRenderTypes.swordEnlightenmentOculusSafe();
                default -> throw new IllegalArgumentException("No safe RenderType for " + shader);
            };
        }

        private static void applyBlend(BlendMode blend) {
            GL11.glEnable(GL11.GL_BLEND);
            if (blend == BlendMode.ALPHA) {
                // Store premultiplied color with conventional accumulated coverage.
                GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                        GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            } else {
                // Add emissive RGB without turning its whole quad into opaque coverage.
                GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE,
                        GL11.GL_ZERO, GL11.GL_ONE);
            }
            GL20.glBlendEquationSeparate(GL14.GL_FUNC_ADD, GL14.GL_FUNC_ADD);
        }
    }

    private record QueuedEffect(BloodfyreFrenzyEntity entity,
                                double x, double y, double z,
                                float yaw, float age, int seed) {
    }

    private record QueuedUtpala(UtpalaAuraEntity entity,
                                double x, double y, double z,
                                float yaw, float age) {
    }

    private record QueuedSword(SwordEnlightenmentEntity entity,
                               double x, double y, double z,
                               float yaw, float age) {
    }

    private static void emitPositionColor(VertexConsumer output, List<RawVertex> vertices) {
        for (RawVertex vertex : vertices) {
            output.vertex(vertex.x, vertex.y, vertex.z)
                    .color(vertex.red, vertex.green, vertex.blue, vertex.alpha)
                    .endVertex();
        }
    }

    private static final class ProgramSet implements AutoCloseable {
        private final Map<AnalyticShader, GlProgram> programs = new java.util.EnumMap<>(AnalyticShader.class);

        private void ensureLoaded() throws IOException {
            if (!programs.isEmpty()) {
                return;
            }
            String vertex = readShader("bloodfyre_frenzy.vsh");
            for (AnalyticShader shader : AnalyticShader.values()) {
                programs.put(shader, GlProgram.compile(vertex, readShader(shader.name + ".fsh")));
            }
        }

        private GlProgram get(AnalyticShader shader) {
            GlProgram program = programs.get(shader);
            if (program == null) {
                throw new IllegalStateException("Bloodfyre GL program not loaded: " + shader);
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

        private GlProgram(int id) {
            this.id = id;
            this.modelViewLocation = GL20.glGetUniformLocation(id, "ModelViewMat");
            this.projectionLocation = GL20.glGetUniformLocation(id, "ProjMat");
            this.colorModulatorLocation = GL20.glGetUniformLocation(id, "ColorModulator");
            this.gameTimeLocation = GL20.glGetUniformLocation(id, "GameTime");
        }

        private static GlProgram compile(String vertexSource, String fragmentSource) {
            int vertex = compileShader(GL20.GL_VERTEX_SHADER, vertexSource);
            int fragment = compileShader(GL20.GL_FRAGMENT_SHADER, fragmentSource);
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
                throw new IllegalStateException("Bloodfyre Oculus program link failed: " + log);
            }
            return new GlProgram(program);
        }

        private static int compileShader(int type, String source) {
            int shader = GL20.glCreateShader(type);
            GL20.glShaderSource(shader, source);
            GL20.glCompileShader(shader);
            if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
                String log = GL20.glGetShaderInfoLog(shader);
                GL20.glDeleteShader(shader);
                throw new IllegalStateException("Bloodfyre Oculus shader compile failed: " + log);
            }
            return shader;
        }

        private void apply(Matrix4f modelView, Matrix4f projection, float gameTime) {
            GL20.glUseProgram(id);
            uploadMatrix(modelViewLocation, modelView);
            uploadMatrix(projectionLocation, projection);
            if (colorModulatorLocation >= 0) {
                GL20.glUniform4f(colorModulatorLocation, 1.0F, 1.0F, 1.0F, 1.0F);
            }
            if (gameTimeLocation >= 0) {
                GL20.glUniform1f(gameTimeLocation, gameTime);
            }
        }

        private static void uploadMatrix(int location, Matrix4f matrix) {
            if (location < 0) {
                return;
            }
            MATRIX.clear();
            matrix.get(MATRIX);
            GL20.glUniformMatrix4fv(location, false, MATRIX);
        }

        @Override
        public void close() {
            GL20.glDeleteProgram(id);
        }
    }

    private static final class BloodfyreFramebuffer implements AutoCloseable {
        private final FloatBuffer transparent = BufferUtils.createFloatBuffer(4);
        private final boolean honkaiComposite;
        private int effectFramebuffer;
        private int copyFramebuffer;
        private int effectTexture;
        private int maskTexture;
        private int blurTextureA;
        private int blurTextureB;
        private int sceneTexture;
        private int width;
        private int height;

        private BloodfyreFramebuffer(boolean honkaiComposite) {
            this.honkaiComposite = honkaiComposite;
        }

        private void ensureAllocated(int nextWidth, int nextHeight) {
            if (effectFramebuffer == 0) effectFramebuffer = GL30.glGenFramebuffers();
            if (copyFramebuffer == 0) copyFramebuffer = GL30.glGenFramebuffers();
            if (effectTexture == 0) effectTexture = GL11.glGenTextures();
            if (maskTexture == 0) maskTexture = GL11.glGenTextures();
            if (blurTextureA == 0) blurTextureA = GL11.glGenTextures();
            if (blurTextureB == 0) blurTextureB = GL11.glGenTextures();
            if (sceneTexture == 0) sceneTexture = GL11.glGenTextures();
            if (width == nextWidth && height == nextHeight) {
                return;
            }
            width = nextWidth;
            height = nextHeight;
            allocate(effectTexture);
            allocate(maskTexture);
            allocate(blurTextureA);
            allocate(blurTextureB);
            allocate(sceneTexture);
        }

        private void beginEffect(SkillShaderTarget target) {
            bindEffectTarget(effectTexture, target.depthTextureId());
            clearColor();
            prepareGeometryState();
        }

        private void beginMask(SkillShaderTarget target) {
            bindEffectTarget(maskTexture, target.depthTextureId());
            clearColor();
            prepareGeometryState();
        }

        private void bindEffectTarget(int colorTexture, int depthTexture) {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, effectFramebuffer);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                    GL11.GL_TEXTURE_2D, colorTexture, 0);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT,
                    GL11.GL_TEXTURE_2D, depthTexture, 0);
            GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
            checkFramebuffer("Bloodfyre effect");
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
            int bloomTexture = blurMask();
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, target.framebufferId());
            GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
            GL11.glViewport(0, 0, width, height);
            if (honkaiComposite) {
                SkillPostShaders.compositeHonkai(
                        sceneTexture, effectTexture, bloomTexture, width, height);
            } else {
                SkillPostShaders.compositeBloodfyre(
                        sceneTexture, effectTexture, maskTexture, bloomTexture, width, height);
            }
        }

        private void copyScene(SkillShaderTarget target) {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, target.framebufferId());
            GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, copyFramebuffer);
            GL30.glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                    GL11.GL_TEXTURE_2D, sceneTexture, 0);
            GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
            checkDrawFramebuffer("Bloodfyre scene copy");
            GL30.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height,
                    GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        }

        private int blurMask() {
            int source = maskTexture;
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
            checkFramebuffer("Bloodfyre post");
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
            deleteTexture(maskTexture);
            deleteTexture(blurTextureA);
            deleteTexture(blurTextureB);
            deleteTexture(sceneTexture);
            effectTexture = maskTexture = blurTextureA = blurTextureB = sceneTexture = 0;
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
            if (vertexArray == 0) vertexArray = GL30.glGenVertexArrays();
            if (vertexBuffer == 0) vertexBuffer = GL15.glGenBuffers();
            int byteCount = vertices.size() * STRIDE;
            if (uploadBuffer == null || uploadBuffer.capacity() < byteCount) {
                int capacity = 4096;
                while (capacity < byteCount) {
                    capacity <<= 1;
                }
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
            if (vertexBuffer != 0) {
                GL15.glDeleteBuffers(vertexBuffer);
                vertexBuffer = 0;
            }
            if (vertexArray != 0) {
                GL30.glDeleteVertexArrays(vertexArray);
                vertexArray = 0;
            }
            uploadBuffer = null;
        }
    }

    /** Converts every renderer quad to two explicit triangles before Oculus can see it. */
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
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.alpha = alpha;
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            this.u = u;
            this.v = v;
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            return this;
        }

        @Override
        public void endVertex() {
            quad[quadSize++] = new RawVertex((float) x, (float) y, (float) z,
                    red, green, blue, alpha, u, v);
            if (quadSize == 4) {
                add(quad[0]); add(quad[1]); add(quad[2]);
                add(quad[0]); add(quad[2]); add(quad[3]);
                quadSize = 0;
            }
        }

        private void add(RawVertex vertex) {
            vertices.add(vertex);
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
                           int program, int vertexArray, int arrayBuffer,
                           int activeTexture, int[] textures,
                           int[] viewport, boolean blend, boolean depth, boolean cull,
                           boolean depthMask, int depthFunc,
                           int blendSrcRgb, int blendDstRgb, int blendSrcAlpha, int blendDstAlpha,
                           int blendEquationRgb, int blendEquationAlpha,
                           boolean[] colorMask) {

        private static GlState capture() {
            int active = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            int[] textures = new int[3];
            for (int i = 0; i < textures.length; i++) {
                GL13.glActiveTexture(GL13.GL_TEXTURE0 + i);
                textures[i] = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            }
            GL13.glActiveTexture(active);

            IntBuffer viewportBuffer = BufferUtils.createIntBuffer(4);
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewportBuffer);
            int[] viewport = new int[]{viewportBuffer.get(0), viewportBuffer.get(1),
                    viewportBuffer.get(2), viewportBuffer.get(3)};
            ByteBuffer mask = BufferUtils.createByteBuffer(4);
            GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK, mask);
            boolean[] colorMask = new boolean[]{mask.get(0) != 0, mask.get(1) != 0,
                    mask.get(2) != 0, mask.get(3) != 0};

            return new GlState(
                    GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING),
                    GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING),
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
