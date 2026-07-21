package com.wjx.kablade.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wjx.kablade.Main;
import com.wjx.kablade.client.shader.OculusFramebufferAccess;
import com.wjx.kablade.client.shader.ShaderCompat;
import com.wjx.kablade.client.shader.SkillShaderTarget;
import com.wjx.kablade.entity.ThunderboltCallEntity;
import net.minecraft.client.Minecraft;
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
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Private Oculus/Iris render path for Thunderbolt Call.
 *
 * <p>Shader packs replace Minecraft's entity {@code ShaderInstance}s and may reinterpret
 * translucent quads. This path queues visible casts, converts every logical quad to explicit
 * triangles, and submits the five original materials with private GL programs directly into
 * Oculus' live translucent HDR target. The target's own depth attachment remains active.</p>
 */
@Mod.EventBusSubscriber(modid = Main.MODID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ThunderboltCallOculusPipeline {
    private static final Map<Integer, QueuedCast> QUEUED = new LinkedHashMap<>();
    private static final ProgramSet PROGRAMS = new ProgramSet();
    private static final MeshDrawer MESH = new MeshDrawer();

    private static boolean resourcesDirty;
    private static boolean disabledForSession;
    private static boolean loggedMissingTarget;
    private static boolean loggedFailure;
    private static boolean loggedActive;

    private ThunderboltCallOculusPipeline() {
    }

    /** Returns true when the normal entity material pass must be suppressed. */
    public static boolean enqueue(ThunderboltCallEntity entity, float partialTick) {
        if (disabledForSession || !ShaderCompat.shouldUseOculusPostPath()) {
            return false;
        }
        Optional<SkillShaderTarget> target = OculusFramebufferAccess.findTranslucentTarget();
        if (target.isEmpty() || !target.get().isComplete()) {
            if (!loggedMissingTarget) {
                loggedMissingTarget = true;
                Main.LOGGER.warn("Thunderbolt Call could not resolve the Oculus translucent "
                        + "target; using the normal shader-pack fallback.");
            }
            return false;
        }
        QUEUED.put(entity.getId(), new QueuedCast(entity, partialTick));
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
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS
                || QUEUED.isEmpty()) {
            return;
        }

        List<QueuedCast> casts = new ArrayList<>(QUEUED.values());
        QUEUED.clear();
        if (disabledForSession || !ShaderCompat.shouldUseOculusPostPath()) {
            return;
        }

        Optional<SkillShaderTarget> resolved = OculusFramebufferAccess.findTranslucentTarget();
        if (resolved.isEmpty() || !resolved.get().isComplete()) {
            return;
        }
        SkillShaderTarget target = resolved.get();

        GlState state = GlState.capture();
        try {
            if (resourcesDirty) {
                closeResources();
                resourcesDirty = false;
            }
            PROGRAMS.ensureLoaded();
            bindTarget(target);
            DrawContext context = new DrawContext(
                    new Matrix4f(RenderSystem.getModelViewMatrix()),
                    new Matrix4f(event.getProjectionMatrix()),
                    shaderGameTime(event.getPartialTick()));
            renderQueued(event, casts, context);

            if (!loggedActive) {
                loggedActive = true;
                Main.LOGGER.info("Thunderbolt Call Oculus pipeline active: fbo={}, color={}, "
                                + "depth={}, size={}x{}, geometry=private-explicit-triangles",
                        target.framebufferId(), target.colorTextureId(), target.depthTextureId(),
                        target.width(), target.height());
            }
        } catch (RuntimeException | IOException exception) {
            disabledForSession = true;
            if (!loggedFailure) {
                loggedFailure = true;
                Main.LOGGER.warn("Disabling the Thunderbolt Call Oculus pipeline for this "
                        + "session; subsequent frames will use fallback geometry.", exception);
            }
        } finally {
            state.restore();
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
            throw new IllegalStateException("Thunderbolt Call Oculus framebuffer incomplete: 0x"
                    + Integer.toHexString(status));
        }
        GL11.glViewport(0, 0, target.width(), target.height());
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glDepthMask(false);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glColorMask(true, true, true, true);
    }

    private static void renderQueued(RenderLevelStageEvent event, List<QueuedCast> casts,
                                     DrawContext context) {
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        for (QueuedCast cast : casts) {
            ThunderboltCallEntity entity = cast.entity();
            if (!entity.isAlive()) {
                continue;
            }
            Vec3 position = entity.getPosition(cast.partialTick());
            poseStack.pushPose();
            try {
                poseStack.translate(position.x - camera.x, position.y - camera.y,
                        position.z - camera.z);
                RawGeometry geometry = new RawGeometry();
                ThunderboltCallRenderer.renderGeometry(entity, cast.partialTick(),
                        poseStack.last().pose(), camera,
                        geometry.composite, geometry.energy, geometry.cross,
                        geometry.lightning, geometry.particle);
                geometry.finish();
                context.draw(Material.COMPOSITE, geometry.composite);
                context.draw(Material.ENERGY, geometry.energy);
                context.draw(Material.CROSS, geometry.cross);
                context.draw(Material.LIGHTNING, geometry.lightning);
                context.draw(Material.PARTICLE, geometry.particle);
            } finally {
                poseStack.popPose();
            }
        }
    }

    private static float shaderGameTime(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return 0.0F;
        }
        return (float) ((minecraft.level.getGameTime() + partialTick) % 24000.0D) / 24000.0F;
    }

    private static void closeResources() {
        PROGRAMS.close();
        MESH.close();
    }

    private record QueuedCast(ThunderboltCallEntity entity, float partialTick) {
    }

    private enum Material {
        ENERGY("thunderbolt_call_energy", "textures/effect/raizan_noise.png", true),
        LIGHTNING("thunderbolt_call_lightning", "textures/effect/raizan_noise.png", true),
        CROSS("thunderbolt_call_cross", "textures/effect/raizan_slash_gradient.png", true),
        PARTICLE("thunderbolt_call_particle", "textures/effect/raizan_particle_mask.png", true),
        COMPOSITE("thunderbolt_call_composite", "textures/effect/raizan_slash_gradient.png", false);

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
        private final QuadTriangleConsumer cross = new QuadTriangleConsumer();
        private final QuadTriangleConsumer particle = new QuadTriangleConsumer();
        private final QuadTriangleConsumer composite = new QuadTriangleConsumer();

        private void finish() {
            energy.finish();
            lightning.finish();
            cross.finish();
            particle.finish();
            composite.finish();
        }
    }

    private record DrawContext(Matrix4f modelView, Matrix4f projection, float gameTime) {
        private void draw(Material material, QuadTriangleConsumer geometry) {
            if (geometry.vertices.isEmpty()) {
                return;
            }
            int texture = Minecraft.getInstance().getTextureManager()
                    .getTexture(material.texture).getId();
            PROGRAMS.get(material).apply(modelView, projection, gameTime, texture);
            GL11.glEnable(GL11.GL_BLEND);
            if (material.additive) {
                GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE,
                        GL11.GL_ZERO, GL11.GL_ONE);
            } else {
                GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                        GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            }
            GL20.glBlendEquationSeparate(GL14.GL_FUNC_ADD, GL14.GL_FUNC_ADD);
            MESH.draw(geometry.vertices);
        }
    }

    private static final class ProgramSet implements AutoCloseable {
        private final Map<Material, GlProgram> programs = new EnumMap<>(Material.class);

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
                throw new IllegalStateException("Thunderbolt Call Oculus program not loaded: "
                        + material);
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
                throw new IllegalStateException("Thunderbolt Call Oculus program link failed ("
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
                throw new IllegalStateException("Thunderbolt Call Oculus shader compile failed ("
                        + name + "): " + log);
            }
            return shader;
        }

        private void apply(Matrix4f modelView, Matrix4f projection,
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

    /** Converts every renderer quad to two explicit triangles before Oculus sees it. */
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
                throw new IllegalStateException("Thunderbolt Call emitted an incomplete quad: "
                        + quadSize);
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
                    GL11.glGetInteger(GL11.GL_DRAW_BUFFER),
                    GL11.glGetInteger(GL11.GL_READ_BUFFER),
                    GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM),
                    GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING),
                    GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING), active,
                    textures, viewport, GL11.glIsEnabled(GL11.GL_BLEND),
                    GL11.glIsEnabled(GL11.GL_DEPTH_TEST), GL11.glIsEnabled(GL11.GL_CULL_FACE),
                    GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK), GL11.glGetInteger(GL11.GL_DEPTH_FUNC),
                    GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),
                    GL11.glGetInteger(GL14.GL_BLEND_DST_RGB),
                    GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),
                    GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA),
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
            GL14.glBlendFuncSeparate(blendSrcRgb, blendDstRgb,
                    blendSrcAlpha, blendDstAlpha);
            GL20.glBlendEquationSeparate(blendEquationRgb, blendEquationAlpha);
            GL11.glDepthMask(depthMask);
            GL11.glDepthFunc(depthFunc);
            GL11.glColorMask(colorMask[0], colorMask[1], colorMask[2], colorMask[3]);
            setEnabled(GL11.GL_BLEND, blend);
            setEnabled(GL11.GL_DEPTH_TEST, depth);
            setEnabled(GL11.GL_CULL_FACE, cull);
        }

        private static void setEnabled(int capability, boolean enabled) {
            if (enabled) {
                GL11.glEnable(capability);
            } else {
                GL11.glDisable(capability);
            }
        }
    }
}
