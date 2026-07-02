package com.wjx.kablade.client.shader;

public record SkillShaderTarget(
        int framebufferId,
        int colorTextureId,
        int depthTextureId,
        int width,
        int height,
        boolean shaderPackTarget) {

    public boolean isComplete() {
        return framebufferId > 0 && colorTextureId > 0 && depthTextureId > 0 && width > 0 && height > 0;
    }
}
