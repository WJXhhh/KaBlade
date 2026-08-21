#version 150

uniform vec4 ColorModulator;
uniform float Time;
uniform float Intensity;

in vec4 vertexColor;
in vec2 texCoord0;
in vec3 viewNormal;

out vec4 fragColor;

void main() {
    float facing = abs(viewNormal.z);
    float fresnel = pow(1.0 - clamp(facing, 0.0, 1.0), 1.6);

    vec3 deepRed = vec3(1.0, 0.12, 0.02);
    vec3 hotOrange = vec3(1.0, 0.60, 0.10);
    vec3 coreGold = vec3(1.0, 0.95, 0.45);

    // Dynamic color gradient across fresnel angle
    vec3 plasmaColor = mix(deepRed, hotOrange, fresnel);
    plasmaColor = mix(plasmaColor, coreGold, pow(fresnel, 3.0) * 0.7);

    // Combined with vertexColor for per-geometry tinting
    vec3 finalRgb = plasmaColor * vertexColor.rgb * ColorModulator.rgb * (1.35 * max(Intensity, 1.0));

    // Solid, rich alpha curve: Center opacity ~ 0.55, Rim opacity ~ 1.0
    float domeAlpha = mix(0.55, 1.0, fresnel);
    float alpha = domeAlpha * vertexColor.a * ColorModulator.a;

    if (alpha < 0.005) {
        discard;
    }

    fragColor = vec4(finalRgb, clamp(alpha, 0.0, 1.0));
}
