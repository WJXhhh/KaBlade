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
    float fresnel = pow(clamp(1.0 - NdotV, 0.0, 1.0), 1.6);

    vec3 baseColor = vertexColor.rgb;
    // Check if the vertex is an earthen rock slab (grey/slate base) or golden energy
    float maxDiff = max(max(abs(baseColor.r - baseColor.g), abs(baseColor.g - baseColor.b)), abs(baseColor.r - baseColor.b));
    bool isRock = maxDiff < 0.12 && baseColor.r < 0.65;

    if (isRock) {
        // Earthen rock slab shading: subtle edge highlight and dark basalt tone
        vec3 rockColor = baseColor * (0.80 + 0.20 * NdotV) + vec3(0.18, 0.12, 0.04) * fresnel * 0.4;
        fragColor = vec4(rockColor, alpha);
    } else {
        // Valkyrie Golden Energy Blade / Flash Shading
        vec3 warmAmber = vec3(1.0, 0.55, 0.08);
        vec3 radiantGold = vec3(1.0, 0.85, 0.25);
        vec3 coreWhiteGold = vec3(1.0, 0.98, 0.88);

        vec3 energyColor = mix(warmAmber, radiantGold, clamp(fresnel * 1.4, 0.0, 1.0));
        energyColor = mix(energyColor, coreWhiteGold, clamp(fresnel * 2.0 - 0.6, 0.0, 1.0));

        vec3 finalRgb = mix(baseColor, energyColor, 0.75);
        float pulse = 0.92 + 0.08 * sin(GameTime * 28.0 + texCoord0.x * 16.0);
        finalRgb *= pulse;

        float finalAlpha = clamp(alpha * (0.70 + fresnel * 0.60), 0.0, 1.0);
        fragColor = vec4(finalRgb, finalAlpha);
    }
}
