package com.wjx.kablade.client.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wjx.kablade.Main;
import mods.flammpfeil.slashblade.client.renderer.model.obj.Face;
import mods.flammpfeil.slashblade.client.renderer.model.obj.GroupObject;
import mods.flammpfeil.slashblade.client.renderer.model.obj.TextureCoordinate;
import mods.flammpfeil.slashblade.client.renderer.model.obj.Vertex;
import mods.flammpfeil.slashblade.client.renderer.model.obj.WavefrontObject;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
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
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Draws visible-scene luminous blade masks through one Oculus HDR/Bloom pass.
 *
 * <p>Oculus 1.8 maps Minecraft's translucent-emissive shader to the shader pack's
 * entity-eyes program even while it is rendering a hand. A separate hand-only workaround
 * would make first and third person look different, so this path captures the exact matrix of
 * every visible perspective draw and renders all of them into Oculus' live HDR target and the
 * same narrow bloom mask after translucent blocks.</p>
 */
@Mod.EventBusSubscriber(modid = Main.MODID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BladeLuminousHandOculusPipeline {
    private static final List<QueuedDraw> QUEUED = new ArrayList<>();
    private static final Map<WavefrontObject, Map<String, LuminousMesh>> MESHES =
            new IdentityHashMap<>();
    private static final LuminousProgram PROGRAM = new LuminousProgram();

    private static boolean resourcesDirty;
    private static boolean disabledForSession;
    private static boolean loggedActive;
    private static boolean loggedUnavailable;
    private static boolean loggedFailure;

    private BladeLuminousHandOculusPipeline() {
    }

    /**
     * Captures a luminous draw while its world or hand projection is still current.
     */
    public static boolean enqueue(WavefrontObject model, String target,
                                  ResourceLocation texture, PoseStack poseStack) {
        if (model == null || target == null || texture == null || poseStack == null
                || disabledForSession || !ShaderCompat.shouldUseOculusPostPath()) {
            return false;
        }

        Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewMatrix())
                .mul(poseStack.last().pose());
        Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix());
        QUEUED.add(new QueuedDraw(model, target, texture, modelView, projection));
        return true;
    }

    /** Marks GL objects stale after a resource or shader-pack reload. */
    public static void invalidateResources() {
        QUEUED.clear();
        resourcesDirty = true;
        disabledForSession = false;
        loggedActive = false;
        loggedUnavailable = false;
        loggedFailure = false;
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS
                || QUEUED.isEmpty()) {
            return;
        }

        List<QueuedDraw> draws = new ArrayList<>(QUEUED);
        QUEUED.clear();
        if (disabledForSession || !ShaderCompat.shouldUseOculusPostPath()) {
            return;
        }

        GlState state = GlState.capture();
        try {
            if (resourcesDirty) {
                closeResources();
                resourcesDirty = false;
            }
            PROGRAM.ensureLoaded();
            // A blade edge needs a tight halo. The generic skill preset uses two blur passes
            // plus refraction, which makes thin weapon details look washed out in first person.
            boolean rendered = OculusSkillRenderer.runPostIfNeeded(
                    () -> render(draws, false),
                    () -> render(draws, true),
                    1, 0.38F, 0.0F, 1.0F);
            if (!rendered) {
                if (!loggedUnavailable) {
                    loggedUnavailable = true;
                    Main.LOGGER.warn("Oculus blade luminous post-pass could not "
                            + "acquire the shader-pack target; retaining the RenderType fallback.");
                }
                return;
            }

            if (!loggedActive) {
                loggedActive = true;
                Main.LOGGER.info("Unified Oculus blade luminous post-pass active.");
            }
        } catch (RuntimeException | IOException exception) {
            disabledForSession = true;
            if (!loggedFailure) {
                loggedFailure = true;
                Main.LOGGER.warn("Disabling the unified Oculus blade luminous post-pass "
                        + "for this session; retaining the RenderType fallback.", exception);
            }
        } finally {
            state.restore();
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            QUEUED.clear();
            resourcesDirty = true;
        }
    }

    private static void render(List<QueuedDraw> draws, boolean maskPass) {
        GL11.glColorMask(true, true, true, true);
        GL11.glEnable(GL11.GL_BLEND);
        GL20.glBlendEquationSeparate(GL14.GL_FUNC_ADD, GL14.GL_FUNC_ADD);
        if (maskPass) {
            GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE,
                    GL11.GL_ONE, GL11.GL_ONE);
        } else {
            // Keep the sharp emissive core at the texture's actual hue. Adding it over
            // the already-rendered base texture pushes warm colors above the HDR range,
            // where the shader pack's tone mapper turns them pale or nearly white.
            GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                    GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        }
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glDepthMask(false);
        GL11.glDisable(GL11.GL_CULL_FACE);

        for (QueuedDraw draw : draws) {
            LuminousMesh mesh = mesh(draw.model, draw.target);
            int texture = textureId(draw.texture);
            PROGRAM.apply(draw.modelView, draw.projection, texture);
            mesh.draw();
        }
    }

    private static LuminousMesh mesh(WavefrontObject model, String target) {
        Map<String, LuminousMesh> modelMeshes = MESHES.computeIfAbsent(
                model, ignored -> new LinkedHashMap<>());
        return modelMeshes.computeIfAbsent(target.toLowerCase(Locale.ROOT),
                ignored -> LuminousMesh.from(model, target));
    }

    private static int textureId(ResourceLocation texture) {
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        Minecraft.getInstance().getTextureManager().bindForSetup(texture);
        return GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
    }

    private static void closeResources() {
        PROGRAM.close();
        for (Map<String, LuminousMesh> modelMeshes : MESHES.values()) {
            for (LuminousMesh mesh : modelMeshes.values()) {
                mesh.close();
            }
        }
        MESHES.clear();
    }

    private static final class LuminousProgram implements AutoCloseable {
        private static final FloatBuffer MATRIX = BufferUtils.createFloatBuffer(16);
        private int id;
        private int modelViewLocation;
        private int projectionLocation;
        private int samplerLocation;

        private void ensureLoaded() throws IOException {
            if (id != 0) {
                return;
            }

            int vertex = compile(GL20.GL_VERTEX_SHADER,
                    readShader("blade_luminous_hand_oculus.vsh"));
            int fragment = compile(GL20.GL_FRAGMENT_SHADER,
                    readShader("blade_luminous_hand_oculus.fsh"));
            id = GL20.glCreateProgram();
            GL20.glAttachShader(id, vertex);
            GL20.glAttachShader(id, fragment);
            GL20.glBindAttribLocation(id, 0, "Position");
            GL20.glBindAttribLocation(id, 1, "UV0");
            GL20.glLinkProgram(id);
            GL20.glDeleteShader(vertex);
            GL20.glDeleteShader(fragment);
            if (GL20.glGetProgrami(id, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
                String log = GL20.glGetProgramInfoLog(id);
                GL20.glDeleteProgram(id);
                id = 0;
                throw new IllegalStateException(
                        "Blade luminous Oculus program link failed: " + log);
            }

            modelViewLocation = GL20.glGetUniformLocation(id, "ModelViewMat");
            projectionLocation = GL20.glGetUniformLocation(id, "ProjMat");
            samplerLocation = GL20.glGetUniformLocation(id, "Sampler0");
        }

        private void apply(Matrix4f modelView, Matrix4f projection, int texture) {
            GL20.glUseProgram(id);
            uploadMatrix(modelViewLocation, modelView);
            uploadMatrix(projectionLocation, projection);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
            GL20.glUniform1i(samplerLocation, 0);
        }

        private static int compile(int type, String source) {
            int shader = GL20.glCreateShader(type);
            GL20.glShaderSource(shader, source);
            GL20.glCompileShader(shader);
            if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
                String log = GL20.glGetShaderInfoLog(shader);
                GL20.glDeleteShader(shader);
                throw new IllegalStateException(
                        "Blade luminous Oculus shader compile failed: " + log);
            }
            return shader;
        }

        private static String readShader(String file) throws IOException {
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath(
                    Main.MODID, "shaders/core/" + file);
            try (var stream = Minecraft.getInstance().getResourceManager().open(location)) {
                return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            }
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

    private static final class LuminousMesh implements AutoCloseable {
        private static final int STRIDE = 20;
        private final ByteBuffer vertices;
        private final int vertexCount;
        private int vertexArray;
        private int vertexBuffer;

        private LuminousMesh(ByteBuffer vertices, int vertexCount) {
            this.vertices = vertices;
            this.vertexCount = vertexCount;
        }

        private static LuminousMesh from(WavefrontObject model, String target) {
            GroupObject group = model.groupObjects.stream()
                    .filter(candidate -> candidate != null && candidate.name != null
                            && target.equalsIgnoreCase(candidate.name))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "Missing luminous source OBJ group: " + target));

            int vertexCount = group.faces.stream()
                    .filter(face -> face != null && face.vertices != null
                            && face.vertices.length >= 3)
                    .mapToInt(face -> (face.vertices.length - 2) * 3)
                    .sum();
            ByteBuffer data = BufferUtils.createByteBuffer(vertexCount * STRIDE);
            for (Face face : group.faces) {
                if (face == null || face.vertices == null || face.vertices.length < 3) {
                    continue;
                }
                for (int i = 1; i < face.vertices.length - 1; i++) {
                    putVertex(data, face, 0);
                    putVertex(data, face, i);
                    putVertex(data, face, i + 1);
                }
            }
            data.flip();
            return new LuminousMesh(data, vertexCount);
        }

        private static void putVertex(ByteBuffer data, Face face, int index) {
            Vertex position = face.vertices[index];
            TextureCoordinate uv = face.textureCoordinates != null
                    && index < face.textureCoordinates.length
                    ? face.textureCoordinates[index] : null;
            data.putFloat(position.x).putFloat(position.y).putFloat(position.z);
            data.putFloat(uv == null ? 0.0F : uv.u).putFloat(uv == null ? 0.0F : uv.v);
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
        }

        @Override
        public void close() {
            if (vertexBuffer != 0) {
                GL15.glDeleteBuffers(vertexBuffer);
            }
            if (vertexArray != 0) {
                GL30.glDeleteVertexArrays(vertexArray);
            }
            vertexBuffer = vertexArray = 0;
        }
    }

    private record QueuedDraw(WavefrontObject model, String target, ResourceLocation texture,
                              Matrix4f modelView, Matrix4f projection) {
    }

    private record GlState(int drawFramebuffer, int readFramebuffer,
                           int drawBuffer, int readBuffer, int program,
                           int vertexArray, int arrayBuffer, int activeTexture,
                           int texture, int[] viewport, boolean blend, boolean depth,
                           boolean cull, boolean depthMask, int depthFunc,
                           int blendSrcRgb, int blendDstRgb,
                           int blendSrcAlpha, int blendDstAlpha,
                           int blendEquationRgb, int blendEquationAlpha,
                           boolean[] colorMask) {

        private static GlState capture() {
            int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            int texture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            GL13.glActiveTexture(activeTexture);

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
                    activeTexture, texture, viewport,
                    GL11.glIsEnabled(GL11.GL_BLEND),
                    GL11.glIsEnabled(GL11.GL_DEPTH_TEST),
                    GL11.glIsEnabled(GL11.GL_CULL_FACE),
                    GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK),
                    GL11.glGetInteger(GL11.GL_DEPTH_FUNC),
                    GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),
                    GL11.glGetInteger(GL14.GL_BLEND_DST_RGB),
                    GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),
                    GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA),
                    GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB),
                    GL11.glGetInteger(GL20.GL_BLEND_EQUATION_ALPHA),
                    colorMask);
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
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
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
