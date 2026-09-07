#version 150

uniform float GameTime;

in vec4 vertexColor;
in vec2 texCoord0;
in vec3 viewPos;

out vec4 fragColor;

void main() {
    float alpha = vertexColor.a;
    if (alpha <= 0.003) {
        discard;
    }

    vec3 viewDir = -normalize(viewPos);
    vec3 normal = normalize(cross(dFdx(viewPos), dFdy(viewPos)));
    float NdotV = abs(dot(normal, viewDir));
    float rim = pow(clamp(1.0 - NdotV, 0.0, 1.0), 1.6);

    // Death Gaze 崩坏紫色死光调色板：严格压制绿色通道以防光影 HDR 泛白
    // 深邃暗紫 -> 灼热品红 (0xE54C9E) -> 高能电光亮粉紫
    vec3 deepViolet = vec3(0.52, 0.02, 0.90);
    vec3 honkaiMagenta = vec3(0.90, 0.16, 0.62); // 接近 0xE54C9E
    vec3 hotCorePink = vec3(1.0, 0.32, 0.92);

    vec3 base = vertexColor.rgb;

    // 边缘掠射角赋予强烈的崩坏暗紫霓虹光晕，正向面赋予高能品红与亮粉核心
    vec3 energyColor = mix(honkaiMagenta, deepViolet, rim);

    // 若顶点本身亮度极高（如核心激光柱），则融入电光亮粉核心，绝不用 G>0.4 的惨白
    float coreFactor = smoothstep(0.70, 0.98, max(base.r, base.b));
    energyColor = mix(energyColor, hotCorePink, coreFactor * (1.0 - rim * 0.65));

    // 与顶点自带颜色融合
    vec3 finalRgb = mix(base, energyColor, 0.75);

    // 沿光线方向的高频等离子能量微振荡
    float pulse = 0.92 + 0.08 * sin(GameTime * 32.0 + viewPos.z * 1.8);
    finalRgb *= pulse;

    // 边缘软化：圆柱管壁切线处做柔和羽化，避免空心圆柱在迎面看时像实心塑料圈
    float edgeFeather = smoothstep(0.0, 0.18, NdotV);
    float finalAlpha = clamp(alpha * (0.45 + 0.55 * edgeFeather), 0.0, 1.0);
    fragColor = vec4(finalRgb, finalAlpha);
}
