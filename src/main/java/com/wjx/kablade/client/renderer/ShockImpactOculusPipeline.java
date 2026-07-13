package com.wjx.kablade.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.wjx.kablade.Main;
import com.wjx.kablade.client.shader.OculusFramebufferAccess;
import com.wjx.kablade.client.shader.ShaderCompat;
import com.wjx.kablade.client.shader.SkillShaderTarget;
import com.wjx.kablade.entity.ShockImpactEntity;
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
 * Full-detail shader-pack renderer for Shock Impact.
 *
 * <p>Oculus replaces Minecraft entity programs, so the normal analytic shader cannot be
 * submitted through a RenderType reliably. This renderer queues visible instances and draws
 * them once per frame with a private program into HDR targets sharing Oculus' live depth.</p>
 */
@Mod.EventBusSubscriber(modid = Main.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ShockImpactOculusPipeline {
    private static final Map<Integer, QueuedImpact> QUEUED = new LinkedHashMap<>();
    private static final ShockFramebuffer FRAMEBUFFER = new ShockFramebuffer();
    private static final MeshDrawer MESH_DRAWER = new MeshDrawer();
    private static final GlProgram PROGRAM = new GlProgram();
    private static final PostPrograms POST = new PostPrograms();
    private static final int NARROW_BLUR_PASSES = 2;
    private static final int WIDE_BLUR_PASSES = 5;

    private static boolean resourcesDirty;
    private static boolean disabledForSession;
    private static boolean loggedMissingTarget;
    private static boolean loggedFailure;
    private static boolean loggedActive;

    private ShockImpactOculusPipeline() {
    }

    /** Returns true when the normal entity renderer should be suppressed for this frame. */
    public static boolean enqueue(ShockImpactEntity entity, float partialTick) {
        if (!ShaderCompat.shouldUseOculusPostPath()) {
            return false;
        }

        Entity owner = entity.getOwner();
        double x;
        double y;
        double z;
        if (owner != null) {
            x = Mth.lerp(partialTick, owner.xOld, owner.getX());
            y = Mth.lerp(partialTick, owner.yOld, owner.getY());
            z = Mth.lerp(partialTick, owner.zOld, owner.getZ());
            float yawRadians = entity.getYRot() * Mth.DEG_TO_RAD;
            x += -Mth.sin(yawRadians) * entity.getForwardOffset();
            y += entity.getUpOffset();
            z += Mth.cos(yawRadians) * entity.getForwardOffset();
        } else {
            x = Mth.lerp(partialTick, entity.xOld, entity.getX());
            y = Mth.lerp(partialTick, entity.yOld, entity.getY());
            z = Mth.lerp(partialTick, entity.zOld, entity.getZ());
        }

        QUEUED.put(entity.getId(), new QueuedImpact(entity, x, y, z, entity.getYRot(),
                entity.tickCount + partialTick, entity.getLifetime(), entity.getScale()));
        return true;
    }

    /** Marks all direct GL resources stale after a resource/shader reload. */
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
                Main.LOGGER.warn("Shock Impact Oculus renderer could not resolve a complete translucent target; using safe fallback.");
            }
            renderFallback(event, impacts);
            return;
        }

        SkillShaderTarget target = resolved.get();
        if (!loggedActive) {
            loggedActive = true;
            Main.LOGGER.info("Shock Impact Oculus renderer active: fbo={}, color={}, depth={}, size={}x{}",
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
            POST.ensureLoaded();
            FRAMEBUFFER.ensureAllocated(target.width(), target.height());

            DrawContext context = new DrawContext(
                    new Matrix4f(RenderSystem.getModelViewMatrix()),
                    new Matrix4f(event.getProjectionMatrix()), shaderGameTime(event.getPartialTick()));

            FRAMEBUFFER.beginEffect(target);
            renderQueued(event, impacts, context, false);

            FRAMEBUFFER.beginGlow(target);
            renderQueued(event, impacts, context, true);

            FRAMEBUFFER.prepareBloom();
            FRAMEBUFFER.blurNarrow();
            FRAMEBUFFER.blurWide();
            FRAMEBUFFER.composite(target);
        } catch (RuntimeException | IOException exception) {
            failed = true;
            disabledForSession = true;
            if (!loggedFailure) {
                loggedFailure = true;
                Main.LOGGER.warn("Disabling the Shock Impact Oculus renderer for this session; using safe fallback.", exception);
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

    private static float shaderGameTime(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return 0.0F;
        }
        return (float) ((minecraft.level.getGameTime() + partialTick) % 24000L) / 24000.0F;
    }

    private static void renderQueued(RenderLevelStageEvent event, List<QueuedImpact> impacts,
                                     DrawContext context, boolean glow) {
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        for (QueuedImpact impact : impacts) {
            if (!impact.entity().isAlive() || impact.age() >= impact.lifetime()) {
                continue;
            }
            poseStack.pushPose();
            poseStack.translate(impact.x() - camera.x, impact.y() - camera.y, impact.z() - camera.z);
            poseStack.mulPose(Axis.YP.rotationDegrees(-impact.yaw()));
            poseStack.translate(0.0F,
                    ShockImpactRenderer.oculusVerticalOffset(impact.age(), impact.lifetime()), 0.18F);
            float scale = ShockImpactRenderer.oculusScale(
                    impact.age(), impact.lifetime(), impact.scale());
            poseStack.scale(scale, scale, scale);
            Matrix4f matrix = poseStack.last().pose();
            if (glow) {
                ShockImpactRenderer.renderOculusGlow(context, matrix, impact.age(), impact.lifetime());
            } else {
                ShockImpactRenderer.renderOculusColor(context, matrix, impact.age(), impact.lifetime());
            }
            poseStack.popPose();
        }
    }

    private static void renderFallback(RenderLevelStageEvent event, List<QueuedImpact> impacts) {
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource immediate = MultiBufferSource.immediate(new BufferBuilder(131072));
        for (QueuedImpact impact : impacts) {
            if (!impact.entity().isAlive() || impact.age() >= impact.lifetime()) {
                continue;
            }
            poseStack.pushPose();
            poseStack.translate(impact.x() - camera.x, impact.y() - camera.y, impact.z() - camera.z);
            poseStack.mulPose(Axis.YP.rotationDegrees(-impact.yaw()));
            poseStack.translate(0.0F,
                    ShockImpactRenderer.oculusVerticalOffset(impact.age(), impact.lifetime()), 0.18F);
            float scale = ShockImpactRenderer.oculusScale(
                    impact.age(), impact.lifetime(), impact.scale());
            poseStack.scale(scale, scale, scale);
            ShockImpactRenderer.renderOculusFallback(
                    immediate, poseStack.last().pose(), impact.age(), impact.lifetime());
            poseStack.popPose();
        }
        immediate.endBatch();
    }

    private static void closeResources() {
        PROGRAM.close();
        POST.close();
        FRAMEBUFFER.close();
        MESH_DRAWER.close();
    }

    /** Facade allowing the renderer to feed its existing quad geometry into the private program. */
    public static final class DrawContext {
        private final Matrix4f modelView;
        private final Matrix4f projection;
        private final float gameTime;

        private DrawContext(Matrix4f modelView, Matrix4f projection, float gameTime) {
            this.modelView = modelView;
            this.projection = projection;
            this.gameTime = gameTime;
        }

        public void draw(Consumer<VertexConsumer> geometry) {
            QuadTriangleConsumer consumer = new QuadTriangleConsumer();
            geometry.accept(consumer);
            if (consumer.vertices.isEmpty()) {
                return;
            }
            PROGRAM.apply(modelView, projection, gameTime);
            GL11.glEnable(GL11.GL_BLEND);
            GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ONE);
            GL20.glBlendEquationSeparate(GL14.GL_FUNC_ADD, GL14.GL_FUNC_ADD);
            MESH_DRAWER.draw(consumer.vertices);
        }
    }

    private record QueuedImpact(ShockImpactEntity entity, double x, double y, double z,
                                float yaw, float age, float lifetime, float scale) {
    }

    private static final class GlProgram implements AutoCloseable {
        private static final FloatBuffer MATRIX = BufferUtils.createFloatBuffer(16);
        private int id;
        private int modelViewLocation;
        private int projectionLocation;
        private int colorLocation;
        private int gameTimeLocation;

        private void ensureLoaded() throws IOException {
            if (id != 0) {
                return;
            }
            String vertex = readShader("shock_impact.vsh");
            String fragment = readShader("shock_impact.fsh");
            int vertexShader = compileShader(GL20.GL_VERTEX_SHADER, vertex);
            int fragmentShader = compileShader(GL20.GL_FRAGMENT_SHADER, fragment);
            id = GL20.glCreateProgram();
            GL20.glAttachShader(id, vertexShader);
            GL20.glAttachShader(id, fragmentShader);
            GL20.glBindAttribLocation(id, 0, "Position");
            GL20.glBindAttribLocation(id, 1, "Color");
            GL20.glBindAttribLocation(id, 2, "UV0");
            GL20.glLinkProgram(id);
            GL20.glDeleteShader(vertexShader);
            GL20.glDeleteShader(fragmentShader);
            if (GL20.glGetProgrami(id, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
                String log = GL20.glGetProgramInfoLog(id);
                close();
                throw new IllegalStateException("Shock Impact Oculus program link failed: " + log);
            }
            modelViewLocation = GL20.glGetUniformLocation(id, "ModelViewMat");
            projectionLocation = GL20.glGetUniformLocation(id, "ProjMat");
            colorLocation = GL20.glGetUniformLocation(id, "ColorModulator");
            gameTimeLocation = GL20.glGetUniformLocation(id, "GameTime");
        }

        private static String readShader(String file) throws IOException {
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath(
                    Main.MODID, "shaders/core/" + file);
            try (var stream = Minecraft.getInstance().getResourceManager().open(location)) {
                return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            }
        }

        private void apply(Matrix4f modelView, Matrix4f projection, float gameTime) {
            GL20.glUseProgram(id);
            uploadMatrix(modelViewLocation, modelView);
            uploadMatrix(projectionLocation, projection);
            GL20.glUniform4f(colorLocation, 1.0F, 1.0F, 1.0F, 1.0F);
            GL20.glUniform1f(gameTimeLocation, gameTime);
        }

        private static void uploadMatrix(int location, Matrix4f matrix) {
            MATRIX.clear();
            matrix.get(MATRIX);
            GL20.glUniformMatrix4fv(location, false, MATRIX);
        }

        private static int compileShader(int type, String source) {
            int shader = GL20.glCreateShader(type);
            GL20.glShaderSource(shader, source);
            GL20.glCompileShader(shader);
            if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
                String log = GL20.glGetShaderInfoLog(shader);
                GL20.glDeleteShader(shader);
                throw new IllegalStateException("Shock Impact Oculus shader compile failed: " + log);
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

    private static final class ShockFramebuffer implements AutoCloseable {
        private final FloatBuffer transparent = BufferUtils.createFloatBuffer(4);
        private int geometryFramebuffer;
        private int postFramebuffer;
        private int effectTexture;
        private int glowTexture;
        private int narrowTexture;
        private int wideTexture;
        private int blurTexture;
        private int sceneTexture;
        private int width;
        private int height;

        private void ensureAllocated(int nextWidth, int nextHeight) {
            if (geometryFramebuffer == 0) geometryFramebuffer = GL30.glGenFramebuffers();
            if (postFramebuffer == 0) postFramebuffer = GL30.glGenFramebuffers();
            if (effectTexture == 0) effectTexture = GL11.glGenTextures();
            if (glowTexture == 0) glowTexture = GL11.glGenTextures();
            if (narrowTexture == 0) narrowTexture = GL11.glGenTextures();
            if (wideTexture == 0) wideTexture = GL11.glGenTextures();
            if (blurTexture == 0) blurTexture = GL11.glGenTextures();
            if (sceneTexture == 0) sceneTexture = GL11.glGenTextures();
            if (width == nextWidth && height == nextHeight) {
                return;
            }
            width = nextWidth;
            height = nextHeight;
            allocate(effectTexture);
            allocate(glowTexture);
            allocate(narrowTexture);
            allocate(wideTexture);
            allocate(blurTexture);
            allocate(sceneTexture);
        }

        private void beginEffect(SkillShaderTarget target) {
            bindGeometry(effectTexture, target.depthTextureId());
        }

        private void beginGlow(SkillShaderTarget target) {
            bindGeometry(glowTexture, target.depthTextureId());
        }

        private void bindGeometry(int colorTexture, int depthTexture) {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, geometryFramebuffer);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                    GL11.GL_TEXTURE_2D, colorTexture, 0);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT,
                    GL11.GL_TEXTURE_2D, depthTexture, 0);
            GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
            checkFramebuffer("Shock Impact geometry");
            GL11.glViewport(0, 0, width, height);
            clearColor();
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            GL11.glDepthMask(false);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glColorMask(true, true, true, true);
        }

        private void prepareBloom() {
            copyTexture(glowTexture, narrowTexture);
            copyTexture(glowTexture, wideTexture);
        }

        private void blurNarrow() {
            blur(narrowTexture, NARROW_BLUR_PASSES);
        }

        private void blurWide() {
            blur(wideTexture, WIDE_BLUR_PASSES);
        }

        private void blur(int texture, int passes) {
            for (int i = 0; i < passes; i++) {
                bindPost(blurTexture);
                POST.blur(texture, width, height, true);
                bindPost(texture);
                POST.blur(blurTexture, width, height, false);
            }
        }

        private void composite(SkillShaderTarget target) {
            copyScene(target);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, target.framebufferId());
            GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
            GL11.glViewport(0, 0, width, height);
            POST.composite(sceneTexture, effectTexture, glowTexture,
                    narrowTexture, wideTexture, width, height);
        }

        private void copyScene(SkillShaderTarget target) {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, target.framebufferId());
            GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, postFramebuffer);
            GL30.glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                    GL11.GL_TEXTURE_2D, sceneTexture, 0);
            GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
            checkDrawFramebuffer("Shock Impact scene copy");
            GL30.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height,
                    GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        }

        private void copyTexture(int source, int destination) {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, geometryFramebuffer);
            GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                    GL11.GL_TEXTURE_2D, source, 0);
            GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, postFramebuffer);
            GL30.glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                    GL11.GL_TEXTURE_2D, destination, 0);
            GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
            checkDrawFramebuffer("Shock Impact bloom copy");
            GL30.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height,
                    GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        }

        private void bindPost(int texture) {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, postFramebuffer);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                    GL11.GL_TEXTURE_2D, texture, 0);
            GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
            checkFramebuffer("Shock Impact post");
            GL11.glViewport(0, 0, width, height);
        }

        private void allocate(int texture) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL13.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL13.GL_CLAMP_TO_EDGE);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA16F,
                    width, height, 0, GL11.GL_RGBA, GL11.GL_FLOAT, 0L);
        }

        private void clearColor() {
            transparent.clear();
            transparent.put(0.0F).put(0.0F).put(0.0F).put(0.0F).flip();
            GL30.glClearBufferfv(GL11.GL_COLOR, 0, transparent);
        }

        @Override
        public void close() {
            deleteTexture(effectTexture);
            deleteTexture(glowTexture);
            deleteTexture(narrowTexture);
            deleteTexture(wideTexture);
            deleteTexture(blurTexture);
            deleteTexture(sceneTexture);
            effectTexture = glowTexture = narrowTexture = wideTexture = blurTexture = sceneTexture = 0;
            if (geometryFramebuffer != 0) GL30.glDeleteFramebuffers(geometryFramebuffer);
            if (postFramebuffer != 0) GL30.glDeleteFramebuffers(postFramebuffer);
            geometryFramebuffer = postFramebuffer = 0;
            width = height = 0;
        }

        private static void deleteTexture(int texture) {
            if (texture != 0) GL11.glDeleteTextures(texture);
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
    }

    private static final class PostPrograms implements AutoCloseable {
        private static final String FULLSCREEN_VERTEX = """
                #version 150
                out vec2 vUv;
                void main() {
                    vec2 p = gl_VertexID == 0 ? vec2(-1.0, -1.0)
                            : (gl_VertexID == 1 ? vec2(3.0, -1.0) : vec2(-1.0, 3.0));
                    vUv = p * 0.5 + 0.5;
                    gl_Position = vec4(p, 0.0, 1.0);
                }
                """;
        private static final String BLUR_FRAGMENT = """
                #version 150
                uniform sampler2D Source;
                uniform vec2 TexelStep;
                in vec2 vUv;
                out vec4 fragColor;
                void main() {
                    vec4 sum = texture(Source, vUv) * 0.227027;
                    sum += texture(Source, vUv + TexelStep * 1.384615) * 0.316216;
                    sum += texture(Source, vUv - TexelStep * 1.384615) * 0.316216;
                    sum += texture(Source, vUv + TexelStep * 3.230769) * 0.070270;
                    sum += texture(Source, vUv - TexelStep * 3.230769) * 0.070270;
                    fragColor = sum;
                }
                """;
        private static final String COMPOSITE_FRAGMENT = """
                #version 150
                uniform sampler2D Scene;
                uniform sampler2D Effect;
                uniform sampler2D Glow;
                uniform sampler2D Narrow;
                uniform sampler2D Wide;
                uniform vec2 TexelSize;
                in vec2 vUv;
                out vec4 fragColor;
                float powerOf(vec4 c) { return max(c.a, max(max(c.r, c.g), c.b)); }
                void main() {
                    vec4 sharp = texture(Glow, vUv);
                    float m = clamp(powerOf(sharp), 0.0, 4.0);
                    float left = powerOf(texture(Glow, vUv - vec2(TexelSize.x, 0.0)));
                    float right = powerOf(texture(Glow, vUv + vec2(TexelSize.x, 0.0)));
                    float down = powerOf(texture(Glow, vUv - vec2(0.0, TexelSize.y)));
                    float up = powerOf(texture(Glow, vUv + vec2(0.0, TexelSize.y)));
                    float impact = smoothstep(0.18, 1.10, m);
                    vec2 warp = vec2(right - left, up - down) * TexelSize * 13.0 * 0.16 * impact;
                    vec4 scene = texture(Scene, clamp(vUv + warp, vec2(0.001), vec2(0.999)));
                    vec3 effect = max(texture(Effect, vUv).rgb, vec3(0.0));
                    vec3 narrow = max(texture(Narrow, vUv).rgb, vec3(0.0));
                    vec3 wide = max(texture(Wide, vUv).rgb, vec3(0.0));
                    float peak = max(max(sharp.r, sharp.g), sharp.b);
                    vec3 hue = peak > 0.0001 ? sharp.rgb / peak : vec3(0.12, 0.82, 1.0);
                    vec3 cyanFloor = vec3(0.05, 0.46, 0.72);
                    vec3 bloom = mix(cyanFloor, max(hue, cyanFloor), 0.72)
                            * (narrow * 0.72 + wide * 0.52);
                    fragColor = vec4(scene.rgb + effect + bloom, scene.a);
                }
                """;

        private int blurProgram;
        private int compositeProgram;
        private int vertexArray;

        private void ensureLoaded() {
            if (vertexArray == 0) vertexArray = GL30.glGenVertexArrays();
            if (blurProgram == 0) blurProgram = createProgram(BLUR_FRAGMENT);
            if (compositeProgram == 0) compositeProgram = createProgram(COMPOSITE_FRAGMENT);
        }

        private void blur(int source, int width, int height, boolean horizontal) {
            beginFullscreen(blurProgram);
            bindTexture(0, source);
            GL20.glUniform1i(GL20.glGetUniformLocation(blurProgram, "Source"), 0);
            GL20.glUniform2f(GL20.glGetUniformLocation(blurProgram, "TexelStep"),
                    horizontal ? 1.0F / Math.max(1, width) : 0.0F,
                    horizontal ? 0.0F : 1.0F / Math.max(1, height));
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);
        }

        private void composite(int scene, int effect, int glow, int narrow, int wide,
                               int width, int height) {
            beginFullscreen(compositeProgram);
            bindTexture(0, scene);
            bindTexture(1, effect);
            bindTexture(2, glow);
            bindTexture(3, narrow);
            bindTexture(4, wide);
            GL20.glUniform1i(GL20.glGetUniformLocation(compositeProgram, "Scene"), 0);
            GL20.glUniform1i(GL20.glGetUniformLocation(compositeProgram, "Effect"), 1);
            GL20.glUniform1i(GL20.glGetUniformLocation(compositeProgram, "Glow"), 2);
            GL20.glUniform1i(GL20.glGetUniformLocation(compositeProgram, "Narrow"), 3);
            GL20.glUniform1i(GL20.glGetUniformLocation(compositeProgram, "Wide"), 4);
            GL20.glUniform2f(GL20.glGetUniformLocation(compositeProgram, "TexelSize"),
                    1.0F / Math.max(1, width), 1.0F / Math.max(1, height));
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);
        }

        private void beginFullscreen(int program) {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(false);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL20.glUseProgram(program);
            GL30.glBindVertexArray(vertexArray);
        }

        private static void bindTexture(int unit, int texture) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        }

        private static int createProgram(String fragmentSource) {
            int vertex = GlProgram.compileShader(GL20.GL_VERTEX_SHADER, FULLSCREEN_VERTEX);
            int fragment = GlProgram.compileShader(GL20.GL_FRAGMENT_SHADER, fragmentSource);
            int program = GL20.glCreateProgram();
            GL20.glAttachShader(program, vertex);
            GL20.glAttachShader(program, fragment);
            GL20.glLinkProgram(program);
            GL20.glDeleteShader(vertex);
            GL20.glDeleteShader(fragment);
            if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
                String log = GL20.glGetProgramInfoLog(program);
                GL20.glDeleteProgram(program);
                throw new IllegalStateException("Shock Impact post program link failed: " + log);
            }
            return program;
        }

        @Override
        public void close() {
            if (blurProgram != 0) GL20.glDeleteProgram(blurProgram);
            if (compositeProgram != 0) GL20.glDeleteProgram(compositeProgram);
            if (vertexArray != 0) GL30.glDeleteVertexArrays(vertexArray);
            blurProgram = compositeProgram = vertexArray = 0;
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

    /** Converts quads to explicit triangles, avoiding shader-pack diagonal interpolation seams. */
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

        @Override public VertexConsumer uv(float u, float v) { this.u = u; this.v = v; return this; }
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
            defaultRed = red;
            defaultGreen = green;
            defaultBlue = blue;
            defaultAlpha = alpha;
        }

        @Override public void unsetDefaultColor() { defaultColor = false; }
    }

    private record RawVertex(float x, float y, float z,
                             int red, int green, int blue, int alpha,
                             float u, float v) {
    }

    private record GlState(int drawFramebuffer, int readFramebuffer, int program,
                           int vertexArray, int arrayBuffer, int activeTexture, int[] textures,
                           int[] viewport, boolean blend, boolean depth, boolean cull,
                           boolean depthMask, int depthFunc,
                           int blendSrcRgb, int blendDstRgb, int blendSrcAlpha, int blendDstAlpha,
                           int blendEquationRgb, int blendEquationAlpha, boolean[] colorMask) {
        private static GlState capture() {
            int active = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            int[] textures = new int[5];
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
