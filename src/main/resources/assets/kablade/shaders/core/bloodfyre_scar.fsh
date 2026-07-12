#version 150

uniform vec4 ColorModulator;
uniform float GameTime;
in vec4 vertexColor;
in vec2 texCoord0;
out vec4 fragColor;

float hash(vec2 p) { return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453); }
float noise(vec2 p) {
    vec2 i = floor(p); vec2 f = fract(p); f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1, 0)), f.x),
               mix(hash(i + vec2(0, 1)), hash(i + vec2(1, 1)), f.x), f.y);
}
float fbm(vec2 p) {
    float value = 0.0; float weight = 0.55;
    for (int i = 0; i < 4; i++) { value += noise(p) * weight; p = p * 2.03 + vec2(4.1, 7.7); weight *= 0.48; }
    return value;
}

void main() {
    float kind = floor(texCoord0.x * 0.5);
    float u = fract(texCoord0.x * 0.5) * 2.0;
    float v = clamp(texCoord0.y, 0.0, 1.0);
    float time = GameTime * 240.0;
    float across = abs(v * 2.0 - 1.0);
    float warp = fbm(vec2(u * 13.0 - time * 0.012, v * 7.0));
    float fine = fbm(vec2(u * 31.0, v * 17.0 + time * 0.008));
    float torn = 1.0 - smoothstep(0.68, 1.02, across + (warp - 0.5) * 0.42);
    float cracks = 1.0 - smoothstep(0.025, 0.14,
            abs(sin(u * 47.0 - v * 18.0 + warp * 9.0)));
    float gaps = smoothstep(0.78, 0.93, fine + warp * 0.12);
    float alpha = vertexColor.a * ColorModulator.a * torn * (1.0 - gaps * 0.94);
    vec3 color;

    if (kind < 0.5) {
        float centerCut = 1.0 - smoothstep(0.0, 0.18, across);
        color = mix(vec3(0.035, 0.002, 0.006), vec3(0.13, 0.005, 0.012), warp);
        color = mix(color, vec3(0.002, 0.001, 0.001), centerCut * 0.94);
        alpha *= 0.70 + warp * 0.28;
    } else if (kind < 1.5) {
        float groove = 1.0 - smoothstep(0.0, 0.10 + warp * 0.08, across);
        color = mix(vec3(0.22, 0.003, 0.008), vec3(0.78, 0.018, 0.024), warp);
        color = mix(color, vec3(0.005, 0.001, 0.002), groove * 0.96);
        color = mix(color, vec3(1.0, 0.15, 0.018), cracks * 0.48);
    } else {
        float core = 1.0 - smoothstep(0.0, 0.28, across);
        color = mix(vec3(1.0, 0.10, 0.012), vec3(1.0, 0.94, 0.62), core * 0.92 + cracks * 0.52);
        alpha *= (0.58 + core * 0.56) * smoothstep(0.24, 0.62, warp + fine * 0.18);
        color *= 1.55 + core * 2.15;
    }

    if (alpha < 0.004) discard;
    fragColor = vec4(color, clamp(alpha, 0.0, 1.0));
}
