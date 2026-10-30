package com.wjx.kablade.client.shader;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/** Immutable per-draw mapping. Texture allocation, scene pixels and jitter are independent. */
public record SkillRenderLayout(int textureWidth, int textureHeight,
                                int x, int y, int width, int height,
                                float jitterX, float jitterY) {
    public SkillRenderLayout {
        if (textureWidth <= 0 || textureHeight <= 0 || x < 0 || y < 0
                || width <= 0 || height <= 0 || x + width > textureWidth || y + height > textureHeight
                || !Float.isFinite(jitterX) || !Float.isFinite(jitterY)) {
            throw new IllegalArgumentException("Invalid skill render region");
        }
    }

    public static SkillRenderLayout full(int width, int height) {
        return new SkillRenderLayout(width, height, 0, 0, width, height, 0, 0);
    }

    public void viewport() {
        GL11.glViewport(x, y, width, height);
    }

    /** Copy corresponding scene pixels between the currently bound read/draw targets. */
    public void copyColor() {
        GL30.glBlitFramebuffer(x, y, x + width, y + height, x, y, x + width, y + height,
                GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
    }

    /** Only for private GL programs. Iris-owned programs already implement this transform. */
    public Matrix4f privateProjection(Matrix4f projection) {
        // Jitter is in input pixels, +Y upwards, after SR's shaderpack adaptation.
        // Left multiplication adds clip.w * jitter to clip.xy and preserves hand FOV/depth.
        return new Matrix4f().translation(2 * jitterX / width, 2 * jitterY / height, 0).mul(projection);
    }

    public void postUniforms(int program) {
        GL20.glUniform2f(GL20.glGetUniformLocation(program, "RegionScale"),
                (float) width / textureWidth, (float) height / textureHeight);
        GL20.glUniform2f(GL20.glGetUniformLocation(program, "RegionOffset"),
                (float) x / textureWidth, (float) y / textureHeight);
        GL20.glUniform2f(GL20.glGetUniformLocation(program, "RegionMin"),
                (x + 0.5F) / textureWidth, (y + 0.5F) / textureHeight);
        GL20.glUniform2f(GL20.glGetUniformLocation(program, "RegionMax"),
                (x + width - 0.5F) / textureWidth, (y + height - 0.5F) / textureHeight);
    }

    /** Fullscreen UVs/offsets are logical scene UVs; clamp every tap to valid texel centres. */
    public static String postFragment(String source) {
        int versionEnd = source.indexOf('\n') + 1;
        return source.substring(0, versionEnd) + """
                uniform vec2 RegionScale;
                uniform vec2 RegionOffset;
                uniform vec2 RegionMin;
                uniform vec2 RegionMax;
                vec4 sampleRegion(sampler2D image, vec2 uv) {
                    return texture(image, clamp(RegionOffset + uv * RegionScale, RegionMin, RegionMax));
                }
                """ + source.substring(versionEnd);
    }
}
