package com.wjx.kablade.client.shader;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.lang.reflect.Field;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Standalone checks: no Minecraft launch, world, screenshot or visual judgement. */
public final class SkillRenderCompatibilityChecks {
    public static void main(String[] args) throws Exception {
        projectionChecks();
        SuperResolutionBridgeChecks.run();
        if (args.length > 1 && args[1].equals("--gl")) glChecks(Path.of(args[0]));
        System.out.println("Skill render compatibility checks passed.");
    }

    private static void projectionChecks() {
        for (int[] size : new int[][]{{1920, 1080}, {1919, 1079}, {853, 479}, {32, 33}}) {
            for (float scale : new float[]{1, 0.6666667F, 0.5F}) {
                int width = Math.max(1, (int) (size[0] * scale));
                int height = Math.max(1, (int) (size[1] * scale));
                for (float jitter : new float[]{0, -0.375F, 0.4375F}) {
                    SkillRenderLayout layout = new SkillRenderLayout(size[0], size[1],
                            0, 0, width, height, jitter, -jitter);
                    // Include both world FOV and a hand-like projection with compressed depth.
                    for (float fov : new float[]{70, 100}) {
                        Matrix4f projection = new Matrix4f().perspective((float) Math.toRadians(fov),
                                (float) size[0] / size[1], 0.05F, 1024);
                        if (fov == 100) projection.m22(projection.m22() * 0.125F);
                        Matrix4f original = new Matrix4f(projection);
                        for (float x : new float[]{-0.7F, 0, 0.7F}) {
                            Vector4f scene = projection.transform(new Vector4f(x, 0.3F, -3, 1));
                            Vector4f effect = layout.privateProjection(projection)
                                    .transform(new Vector4f(x, 0.3F, -3, 1));
                            // Reference: the shaderpack adds jitter, then packs clip.xy using
                            // (clip.xy + clip.ww) * render/screen - clip.ww, at full viewport.
                            float packedX = (scene.x + 2 * jitter / width * scene.w + scene.w)
                                    * width / size[0] - scene.w;
                            float packedY = (scene.y - 2 * jitter / height * scene.w + scene.w)
                                    * height / size[1] - scene.w;
                            near((packedX / scene.w + 1) * size[0] / 2,
                                    (effect.x / effect.w + 1) * width / 2, 0.0003F);
                            near((packedY / scene.w + 1) * size[1] / 2,
                                    (effect.y / effect.w + 1) * height / 2, 0.0003F);
                            near(scene.z / scene.w, effect.z / effect.w, 0);
                        }
                        if (!projection.equals(original)) throw new AssertionError("Projection mutated");
                    }
                }
            }
            Matrix4f identity = new Matrix4f();
            if (!SkillRenderLayout.full(size[0], size[1]).privateProjection(identity).equals(identity)) {
                throw new AssertionError("Native/off path changed projection");
            }
        }
    }

    private static void glChecks(Path root) throws Exception {
        if (!GLFW.glfwInit()) throw new AssertionError("Could not initialize hidden GL check");
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 2);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        long window = GLFW.glfwCreateWindow(65, 37, "KBlade automated shader check", 0, 0);
        if (window == 0) throw new AssertionError("Could not create hidden GL context");
        try {
            GLFW.glfwMakeContextCurrent(window);
            GL.createCapabilities();
            int programs = 0;
            for (String name : new String[]{"com.wjx.kablade.client.shader.SkillPostShaders",
                    "com.wjx.kablade.client.renderer.ShockImpactOculusPipeline$PostPrograms"}) {
                Class<?> type = Class.forName(name);
                Field vertexField = type.getDeclaredField("FULLSCREEN_VERTEX");
                vertexField.setAccessible(true);
                String vertex = (String) vertexField.get(null);
                for (Field field : type.getDeclaredFields()) {
                    if (!field.getName().endsWith("_FRAGMENT")) continue;
                    field.setAccessible(true);
                    int program = link(vertex, SkillRenderLayout.postFragment((String) field.get(null)));
                    if (field.getName().equals("BLUR_FRAGMENT")) samplingCheck(program);
                    GL20.glDeleteProgram(program);
                    programs++;
                }
            }
            // Compile/link the complete core resource pairs used by the private programs.
            Path shaders = root.resolve("src/main/resources/assets/kablade/shaders/core");
            try (var paths = Files.list(shaders)) {
                for (Path fragment : paths.filter(p -> p.toString().endsWith(".fsh")).toList()) {
                    String fragmentSource = Files.readString(fragment);
                    // jizo_soul_glow is vanilla-only and needs Minecraft's import preprocessor.
                    if (fragmentSource.contains("#moj_import")) continue;
                    Path vertex = fragment.resolveSibling(fragment.getFileName().toString().replace(".fsh", ".vsh"));
                    if (!Files.exists(vertex)) {
                        Path json = fragment.resolveSibling(fragment.getFileName().toString().replace(".fsh", ".json"));
                        String name = com.google.gson.JsonParser.parseString(Files.readString(json))
                                .getAsJsonObject().get("vertex").getAsString().replace("kablade:", "");
                        vertex = shaders.resolve(name + ".vsh");
                    }
                    if (Files.exists(vertex)) {
                        GL20.glDeleteProgram(link(Files.readString(vertex), fragmentSource));
                        programs++;
                    }
                }
            }
            if (GL11.glGetError() != GL11.GL_NO_ERROR) throw new AssertionError("OpenGL error");
            System.out.println("Compiled/linked " + programs + " GLSL pairs; packed-region blur readback passed.");
        } finally {
            GLFW.glfwDestroyWindow(window);
            GLFW.glfwTerminate();
        }
    }

    private static void samplingCheck(int program) {
        SkillRenderLayout layout = new SkillRenderLayout(65, 37, 0, 0, 43, 24, 0, 0);
        FloatBuffer input = BufferUtils.createFloatBuffer(65 * 37 * 4);
        for (int y = 0; y < 37; y++) for (int x = 0; x < 65; x++) {
            // Bright stale pixels outside the region must never leak into edge blur taps.
            boolean valid = x < 43 && y < 24;
            input.put(valid ? 0.25F : 100).put(valid ? 0.5F : 100).put(valid ? 2 : 100).put(1);
        }
        input.flip();
        int source = texture(input);
        int output = texture(null);
        int framebuffer = GL30.glGenFramebuffers();
        int vao = GL30.glGenVertexArrays();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, output, 0);
        GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
        if (GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE) {
            throw new AssertionError("Test framebuffer incomplete");
        }
        GL20.glUseProgram(program);
        layout.postUniforms(program);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, source);
        GL20.glUniform1i(GL20.glGetUniformLocation(program, "Source"), 0);
        GL30.glBindVertexArray(vao);
        layout.viewport();
        for (boolean horizontal : new boolean[]{true, false}) {
            GL20.glUniform2f(GL20.glGetUniformLocation(program, "TexelStep"),
                    horizontal ? 1.0F / layout.width() : 0, horizontal ? 0 : 1.0F / layout.height());
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);
            FloatBuffer result = BufferUtils.createFloatBuffer(43 * 24 * 4);
            GL11.glReadPixels(0, 0, 43, 24, GL11.GL_RGBA, GL11.GL_FLOAT, result);
            for (int i = 0; i < 43 * 24; i++) {
                near(result.get(i * 4), 0.25F, 0.002F);
                near(result.get(i * 4 + 1), 0.5F, 0.002F);
                near(result.get(i * 4 + 2), 2, 0.005F);
            }
        }
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        GL30.glDeleteFramebuffers(framebuffer);
        GL30.glDeleteVertexArrays(vao);
        GL11.glDeleteTextures(source);
        GL11.glDeleteTextures(output);
    }

    private static int texture(FloatBuffer pixels) {
        int id = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA16F, 65, 37, 0, GL11.GL_RGBA, GL11.GL_FLOAT, pixels);
        return id;
    }

    private static int link(String vertex, String fragment) {
        int v = compile(GL20.GL_VERTEX_SHADER, vertex);
        int f = compile(GL20.GL_FRAGMENT_SHADER, fragment);
        int program = GL20.glCreateProgram();
        GL20.glAttachShader(program, v);
        GL20.glAttachShader(program, f);
        GL20.glLinkProgram(program);
        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == 0) {
            throw new AssertionError(GL20.glGetProgramInfoLog(program));
        }
        GL20.glDeleteShader(v);
        GL20.glDeleteShader(f);
        return program;
    }

    private static int compile(int type, String source) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == 0) {
            throw new AssertionError(GL20.glGetShaderInfoLog(shader));
        }
        return shader;
    }

    private static void near(float actual, float expected, float tolerance) {
        if (!Float.isFinite(actual) || Math.abs(actual - expected) > tolerance) {
            throw new AssertionError("Expected " + expected + ", got " + actual);
        }
    }
}
