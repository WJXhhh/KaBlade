package com.wjx.kablade.client.shader;

public record SkillShaderTarget(
        int framebufferId,
        int colorTextureId,
        int depthTextureId,
        int width,
        int height,
        boolean shaderPackTarget,
        SkillRenderLayout layout) {

    public SkillShaderTarget(int framebufferId, int colorTextureId, int depthTextureId,
                             int width, int height, boolean shaderPackTarget) {
        this(framebufferId, colorTextureId, depthTextureId, width, height, shaderPackTarget,
                SkillRenderLayout.full(width, height));
    }

    public SkillShaderTarget withLayout(SkillRenderLayout next) {
        return new SkillShaderTarget(framebufferId, colorTextureId, depthTextureId,
                width, height, shaderPackTarget, next);
    }

    public boolean isComplete() {
        return framebufferId > 0 && colorTextureId > 0 && depthTextureId > 0 && width > 0 && height > 0;
    }
}
