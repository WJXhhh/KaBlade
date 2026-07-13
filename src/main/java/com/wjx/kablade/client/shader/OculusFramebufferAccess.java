package com.wjx.kablade.client.shader;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public final class OculusFramebufferAccess {
    private static final int MAX_SCAN_DEPTH = 4;

    private OculusFramebufferAccess() {
    }

    public static Optional<SkillShaderTarget> findTranslucentTarget() {
        Optional<Object> pipeline = findPipeline();
        if (pipeline.isEmpty()) {
            return Optional.empty();
        }

        Optional<SkillShaderTarget> sodiumTarget = findSodiumTranslucentTarget(pipeline.get());
        if (sodiumTarget.isPresent()) {
            return sodiumTarget;
        }

        Optional<Object> framebuffer = findFramebufferLike(
                pipeline.get(),
                Set.of("translucent", "terrain", "sodium", "framebuffer", "write"),
                Collections.newSetFromMap(new java.util.IdentityHashMap<>()),
                0);

        if (framebuffer.isEmpty()) {
            return Optional.empty();
        }

        return extractTarget(framebuffer.get(), findRenderTargets(pipeline.get()), true);
    }

    private static Optional<SkillShaderTarget> findSodiumTranslucentTarget(Object pipeline) {
        Optional<Object> terrainPipeline = invokeFirstNoArgOptional(pipeline, "getSodiumTerrainPipeline");
        if (terrainPipeline.isEmpty()) {
            return Optional.empty();
        }

        Optional<Object> framebuffer = invokeFirstNoArgOptional(terrainPipeline.get(), "getTranslucentFramebuffer");
        if (framebuffer.isEmpty()) {
            return Optional.empty();
        }

        Optional<Object> renderTargets = findRenderTargets(pipeline);
        Optional<SkillShaderTarget> attached = extractAttachedTarget(framebuffer.get(), renderTargets);
        return attached.isPresent() ? attached : extractTarget(framebuffer.get(), renderTargets, true);
    }

    /**
     * Oculus' GlFramebuffer exposes its id and color attachments but deliberately hides the
     * depth attachment. Querying the live FBO is both narrower and more reliable than walking
     * arbitrary private pipeline fields, especially across Iris/Oculus minor releases.
     */
    private static Optional<SkillShaderTarget> extractAttachedTarget(Object framebuffer,
                                                                      Optional<Object> renderTargets) {
        Optional<Integer> framebufferId = readInt(framebuffer,
                "getId", "getGlId", "getGlFramebuffer", "getFramebufferId", "frameBufferId", "id");
        Optional<Integer> colorTextureId = readIntWithIntArg(framebuffer, 0, "getColorAttachment")
                .or(() -> readInt(framebuffer,
                        "getColorTextureId", "getColorTexture", "colorTexture", "colorTextureId"));
        if (framebufferId.isEmpty() || colorTextureId.isEmpty()) {
            return Optional.empty();
        }

        int previousDrawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int previousReadFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int depthTextureId;
        try {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebufferId.get());
            int objectType = GL30.glGetFramebufferAttachmentParameteri(
                    GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT,
                    GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE);
            if (objectType != GL11.GL_TEXTURE) {
                return Optional.empty();
            }
            depthTextureId = GL30.glGetFramebufferAttachmentParameteri(
                    GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT,
                    GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME);
        } finally {
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousDrawFramebuffer);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previousReadFramebuffer);
        }
        if (depthTextureId <= 0) {
            return Optional.empty();
        }

        int width = renderTargets.flatMap(targets -> readInt(targets, "getCurrentWidth", "cachedWidth"))
                .orElseGet(() -> textureSize(colorTextureId.get(), GL11.GL_TEXTURE_WIDTH));
        int height = renderTargets.flatMap(targets -> readInt(targets, "getCurrentHeight", "cachedHeight"))
                .orElseGet(() -> textureSize(colorTextureId.get(), GL11.GL_TEXTURE_HEIGHT));
        if (width <= 0 || height <= 0) {
            width = Minecraft.getInstance().getWindow().getWidth();
            height = Minecraft.getInstance().getWindow().getHeight();
        }
        return Optional.of(new SkillShaderTarget(
                framebufferId.get(), colorTextureId.get(), depthTextureId, width, height, true));
    }

    private static int textureSize(int textureId, int parameter) {
        int previousActiveTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        int previousTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        try {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
            return GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, parameter);
        } finally {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousTexture);
            GL13.glActiveTexture(previousActiveTexture);
        }
    }

    private static Optional<Object> findRenderTargets(Object pipeline) {
        Optional<Object> viaMethod = invokeFirstNoArgOptional(pipeline, "getRenderTargets", "renderTargets");
        if (viaMethod.isPresent()) {
            return viaMethod;
        }

        try {
            Field field = pipeline.getClass().getDeclaredField("renderTargets");
            field.setAccessible(true);
            return Optional.ofNullable(field.get(pipeline));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return Optional.empty();
        }
    }

    private static Optional<Object> findPipeline() {
        return findPipelineViaModernIris()
                .or(OculusFramebufferAccess::findPipelineViaLegacyIris);
    }

    private static Optional<Object> findPipelineViaModernIris() {
        try {
            Class<?> iris = Class.forName("net.irisshaders.iris.Iris");
            Object manager = iris.getMethod("getPipelineManager").invoke(null);
            Object pipeline = invokeFirstNoArg(manager, "getPipelineNullable", "getPipeline");
            return unwrapOptional(pipeline);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return Optional.empty();
        }
    }

    private static Optional<Object> findPipelineViaLegacyIris() {
        try {
            Class<?> iris = Class.forName("net.coderbot.iris.Iris");
            Object manager = iris.getMethod("getPipelineManager").invoke(null);
            Object pipeline = invokeFirstNoArg(manager, "getPipelineNullable", "getPipeline");
            return unwrapOptional(pipeline);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return Optional.empty();
        }
    }

    private static Optional<Object> findFramebufferLike(Object root, Set<String> hints,
                                                       Set<Object> visited, int depth) {
        if (root == null || depth > MAX_SCAN_DEPTH || !visited.add(root)) {
            return Optional.empty();
        }

        Class<?> type = root.getClass();
        String typeName = type.getName().toLowerCase(Locale.ROOT);
        if ((typeName.contains("framebuffer") || typeName.contains("rendertarget"))
                && canExtractFramebufferId(root)
                && scoreName(typeName, hints) > 0) {
            return Optional.of(root);
        }

        Object best = null;
        int bestScore = 0;
        for (Field field : type.getDeclaredFields()) {
            Optional<Object> value = readField(root, field);
            if (value.isEmpty()) {
                continue;
            }
            int score = scoreName(field.getName(), hints) + scoreName(value.get().getClass().getName(), hints);
            if (score > bestScore && looksFramebufferLike(value.get())) {
                best = value.get();
                bestScore = score;
            }
            Optional<Object> nested = findFramebufferLike(value.get(), hints, visited, depth + 1);
            if (nested.isPresent()) {
                return nested;
            }
        }

        for (Method method : type.getDeclaredMethods()) {
            if (method.getParameterCount() != 0) {
                continue;
            }
            Optional<Object> value = invoke(method, root);
            if (value.isEmpty()) {
                continue;
            }
            int score = scoreName(method.getName(), hints) + scoreName(value.get().getClass().getName(), hints);
            if (score > bestScore && looksFramebufferLike(value.get())) {
                best = value.get();
                bestScore = score;
            }
        }

        return Optional.ofNullable(best);
    }

    private static boolean looksFramebufferLike(Object value) {
        String typeName = value.getClass().getName().toLowerCase(Locale.ROOT);
        return (typeName.contains("framebuffer") || typeName.contains("rendertarget"))
                && canExtractFramebufferId(value);
    }

    private static Optional<SkillShaderTarget> extractTarget(Object framebuffer, Optional<Object> renderTargets,
                                                            boolean shaderPackTarget) {
        Optional<Integer> framebufferId = readInt(framebuffer,
                "getId", "getGlId", "getGlFramebuffer", "getFramebufferId", "frameBufferId", "id");
        Optional<Integer> colorTextureId = readInt(framebuffer,
                "getColorTextureId", "getColorTexture", "colorTexture", "colorTextureId")
                .or(() -> readIntWithIntArg(framebuffer, 0, "getColorAttachment"));
        Optional<Integer> depthTextureId = readInt(framebuffer,
                "getDepthTextureId", "getDepthAttachment", "getDepthTexture", "depthTexture", "depthTextureId")
                .or(() -> renderTargets.flatMap(targets -> readInt(targets,
                        "getDepthTexture", "currentDepthTexture")));

        if (framebufferId.isEmpty() || colorTextureId.isEmpty() || depthTextureId.isEmpty()) {
            return Optional.empty();
        }

        int width = readInt(framebuffer, "width", "getWidth")
                .or(() -> renderTargets.flatMap(targets -> readInt(targets, "getCurrentWidth", "cachedWidth")))
                .orElse(Minecraft.getInstance().getWindow().getWidth());
        int height = readInt(framebuffer, "height", "getHeight")
                .or(() -> renderTargets.flatMap(targets -> readInt(targets, "getCurrentHeight", "cachedHeight")))
                .orElse(Minecraft.getInstance().getWindow().getHeight());
        return Optional.of(new SkillShaderTarget(
                framebufferId.get(),
                colorTextureId.get(),
                depthTextureId.get(),
                width,
                height,
                shaderPackTarget));
    }

    private static boolean canExtractFramebufferId(Object value) {
        return readInt(value, "getId", "getGlId", "getGlFramebuffer", "getFramebufferId", "frameBufferId", "id")
                .isPresent();
    }

    private static Optional<Integer> readInt(Object target, String... names) {
        Class<?> type = target.getClass();
        for (String name : names) {
            try {
                Method method = type.getDeclaredMethod(name);
                if (method.getParameterCount() == 0) {
                    method.setAccessible(true);
                    Object value = method.invoke(target);
                    if (value instanceof Number number) {
                        return Optional.of(number.intValue());
                    }
                }
            } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            }

            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                Object value = field.get(target);
                if (value instanceof Number number) {
                    return Optional.of(number.intValue());
                }
            } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            }
        }
        return Optional.empty();
    }

    private static Optional<Integer> readIntWithIntArg(Object target, int argument, String... names) {
        Class<?> type = target.getClass();
        for (String name : names) {
            try {
                Method method = type.getDeclaredMethod(name, int.class);
                method.setAccessible(true);
                Object value = method.invoke(target, argument);
                if (value instanceof Number number) {
                    return Optional.of(number.intValue());
                }
            } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            }
        }
        return Optional.empty();
    }

    private static Object invokeFirstNoArg(Object target, String... names) throws ReflectiveOperationException {
        Class<?> type = target.getClass();
        for (String name : names) {
            try {
                Method method = type.getMethod(name);
                if (method.getParameterCount() == 0) {
                    return method.invoke(target);
                }
            } catch (NoSuchMethodException ignored) {
            }
        }
        throw new NoSuchMethodException(type.getName());
    }

    private static Optional<Object> invokeFirstNoArgOptional(Object target, String... names) {
        Class<?> type = target.getClass();
        for (String name : names) {
            try {
                Method method = type.getMethod(name);
                if (method.getParameterCount() == 0) {
                    return Optional.ofNullable(method.invoke(target));
                }
            } catch (NoSuchMethodException ignored) {
            } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
                return Optional.empty();
            }

            try {
                Method method = type.getDeclaredMethod(name);
                if (method.getParameterCount() == 0) {
                    method.setAccessible(true);
                    return Optional.ofNullable(method.invoke(target));
                }
            } catch (NoSuchMethodException ignored) {
            } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private static Optional<Object> unwrapOptional(Object value) {
        if (value instanceof Optional<?> optional) {
            return optional.map(Object.class::cast);
        }
        return Optional.ofNullable(value);
    }

    private static Optional<Object> readField(Object target, Field field) {
        if (field.getType().isPrimitive() || field.getType().isArray() || field.getType().isEnum()) {
            return Optional.empty();
        }
        try {
            field.setAccessible(true);
            return Optional.ofNullable(field.get(target));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return Optional.empty();
        }
    }

    private static Optional<Object> invoke(Method method, Object target) {
        Class<?> returnType = method.getReturnType();
        if (returnType == Void.TYPE || returnType.isPrimitive() || returnType.isArray() || returnType.isEnum()) {
            return Optional.empty();
        }
        try {
            method.setAccessible(true);
            return Optional.ofNullable(method.invoke(target));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return Optional.empty();
        }
    }

    private static int scoreName(String rawName, Set<String> hints) {
        String name = rawName.toLowerCase(Locale.ROOT);
        int score = 0;
        for (String hint : hints) {
            if (name.contains(hint)) {
                score++;
            }
        }
        return score;
    }
}
