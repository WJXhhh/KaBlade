#version 150

uniform vec4 ColorModulator;
uniform float GameTime;

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

float noise(vec2 p) {
    vec2 cell = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(cell), hash(cell + vec2(1.0, 0.0)), f.x),
               mix(hash(cell + vec2(0.0, 1.0)), hash(cell + vec2(1.0, 1.0)), f.x), f.y);
}

float fbm(vec2 p) {
    float value = 0.0;
    float weight = 0.55;
    for (int i = 0; i < 4; i++) {
        value += noise(p) * weight;
        p = p * 2.03 + vec2(4.1, 7.7);
        weight *= 0.48;
    }
    return value;
}

void main() {
    float kind = floor(texCoord0.x * 0.5);
    float u = fract(texCoord0.x * 0.5) * 2.0;
    float v = clamp(texCoord0.y, 0.0, 1.0);
    float time = GameTime * 240.0;
    float across = abs(v * 2.0 - 1.0);
    float warped = fbm(vec2(u * 10.0 - time * 0.10, v * 4.3 + time * 0.045));
    float fine = fbm(vec2(u * 29.0 + time * 0.035, v * 12.0 - time * 0.025));
    float ragged = 1.0 - smoothstep(0.72, 1.04, across + (warped - 0.5) * 0.30);
    float veins = 1.0 - smoothstep(0.035, 0.17,
            abs(sin(u * 57.0 - v * 16.0 + warped * 12.0 - time * 0.18)));
    float pores = smoothstep(0.73, 0.91, fine + warped * 0.14);
    float alpha = vertexColor.a * ColorModulator.a * ragged * (1.0 - pores * 0.82);
    vec3 color;

    if (kind < 0.5) {
        color = mix(vec3(0.018, 0.001, 0.004), vec3(0.34, 0.006, 0.014), warped);
        alpha *= 0.70 + warped * 0.30;
    } else if (kind < 1.5) {
        float grooves = (1.0 - smoothstep(0.05, 0.20,
                abs(sin(u * 19.0 + v * 9.0 + warped * 7.0)))) * smoothstep(0.18, 0.86, across);
        color = mix(vec3(0.28, 0.002, 0.009), vec3(0.96, 0.014, 0.026), 0.36 + warped * 0.60);
        color = mix(color, vec3(0.004, 0.001, 0.002), grooves * 0.94);
        color = mix(color, vec3(1.0, 0.25, 0.02), veins * 0.76);
        alpha *= 0.78 + veins * 0.30;
    } else if (kind < 2.5) {
        float outerEdge = smoothstep(0.10, 0.92, v + (warped - 0.5) * 0.20);
        float white = veins * smoothstep(0.42, 0.78, fine);
        float whiteBody = clamp(0.56 + outerEdge * 0.30 + white * 0.40, 0.0, 1.0);
        color = mix(vec3(1.0, 0.13, 0.012), vec3(1.0, 0.52, 0.075), outerEdge);
        color = mix(color, vec3(1.0, 0.97, 0.80), whiteBody);
        alpha *= 0.92 + white * 0.10;
        color *= 1.70 + white * 1.10 + outerEdge * 0.35;
    } else if (kind < 3.5) {
        float filament = 1.0 - smoothstep(0.08, 0.58, across);
        float breaks = smoothstep(0.24, 0.62, warped);
        color = mix(vec3(1.0, 0.18, 0.018), vec3(1.0, 0.98, 0.84), filament);
        alpha *= filament * breaks;
        color *= 2.15 + filament * 2.45;
    } else {
        // The erosion front is deliberately rendered with ordinary alpha blending:
        // an additive pass cannot preserve its near-black charred core.
        float rimAcross = abs(v * 2.0 - 1.0);
        float rimBody = 1.0 - smoothstep(0.46, 1.0, rimAcross);
        float charCore = 1.0 - smoothstep(0.0, 0.24, rimAcross);
        float hotCrack = veins * smoothstep(0.34, 0.80, fine);
        color = mix(vec3(0.012, 0.0005, 0.002), vec3(0.48, 0.004, 0.012), rimBody);
        color = mix(color, vec3(0.94, 0.020, 0.010), hotCrack * 0.76);
        color = mix(color, vec3(0.002, 0.0004, 0.0005), charCore * 0.84);
        alpha *= rimBody * (0.76 + hotCrack * 0.32);
    }

    if (alpha < 0.004) discard;
    fragColor = vec4(color * mix(vec3(1.0), max(vertexColor.rgb, vec3(0.12)), 0.12),
            clamp(alpha, 0.0, 1.0));
}
