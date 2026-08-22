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
    float fresnel = pow(clamp(1.0 - NdotV, 0.0, 1.0), 1.8);

    vec3 deepRed = vec3(1.0, 0.12, 0.02);
    vec3 hotOrange = vec3(1.0, 0.60, 0.10);
    vec3 coreGold = vec3(1.0, 0.95, 0.45);

    vec3 baseColor = vertexColor.rgb;
    vec3 plasmaColor = mix(deepRed, hotOrange, clamp(fresnel * 1.5, 0.0, 1.0));
    plasmaColor = mix(plasmaColor, coreGold, clamp(fresnel * 2.2 - 0.7, 0.0, 1.0));

    vec3 finalRgb = mix(baseColor, plasmaColor, 0.85);

    float pulse = 0.90 + 0.10 * sin(GameTime * 25.0 + texCoord0.x * 20.0);
    finalRgb *= pulse;

    float finalAlpha = clamp(alpha * (0.65 + fresnel * 0.75), 0.0, 1.0);
    fragColor = vec4(finalRgb, finalAlpha);
}
