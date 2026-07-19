#version 150

uniform vec4 ColorModulator;
uniform float GameTime;

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

float hash2(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash2(i), hash2(i + vec2(1.0, 0.0)), u.x),
               mix(hash2(i + vec2(0.0, 1.0)), hash2(i + vec2(1.0, 1.0)), u.x), u.y);
}

float taper(float u) {
    return smoothstep(0.0, 0.09, u) * (1.0 - smoothstep(0.91, 1.0, u));
}

void main() {
    float kind = floor(texCoord0.x / 2.0);
    float u = fract(texCoord0.x / 2.0) * 2.0;
    float v = clamp(texCoord0.y, 0.0, 1.0);
    float across = abs(v * 2.0 - 1.0);
    float time = GameTime * 240.0;

    vec3 tint = vertexColor.rgb * ColorModulator.rgb;
    float alpha = vertexColor.a * ColorModulator.a;
    vec3 indigo = vec3(0.12, 0.13, 0.25);
    vec3 lavender = vec3(0.68, 0.70, 1.0);
    vec3 ivory = vec3(1.0, 0.94, 0.76);
    vec3 pearl = vec3(1.0, 0.99, 0.93);

    if (kind < 3.5) {
        float body = 1.0 - smoothstep(0.12, 1.0, across);
        float core = 1.0 - smoothstep(0.0, 0.22, across);
        float needle = 1.0 - smoothstep(0.0, 0.055, across);
        float flow = pow(max(0.0, sin(u * 34.0 - time * 0.82 + v * 5.0)), 7.0);
        float grain = noise(vec2(u * 13.0 - time * 0.18, v * 5.0 + time * 0.04));
        float longitudinal = kind < 1.5 ? taper(u) : 1.0;
        float opacity = alpha * longitudinal
                * (body * 0.34 + core * 0.72 + needle * 0.92 + flow * 0.18)
                * (0.82 + grain * 0.18);
        if (opacity < 0.004) {
            discard;
        }

        vec3 orderedTint = mix(lavender, ivory, clamp(tint.r - tint.b + 0.55, 0.0, 1.0));
        vec3 color = mix(indigo, mix(tint, orderedTint, 0.42), body * 0.92);
        color = mix(color, ivory, core * 0.62 + flow * 0.18);
        color = mix(color, pearl, needle * 0.74);
        fragColor = vec4(color * (1.12 + core * 1.18 + needle * 1.54 + flow * 0.42),
                clamp(opacity, 0.0, 1.0));
        return;
    }

    if (kind < 5.0) {
        vec2 p = vec2(u * 2.0 - 1.0, v * 2.0 - 1.0);
        float diamond = abs(p.x) * 0.66 + abs(p.y);
        float body = 1.0 - smoothstep(0.72, 1.08, diamond);
        float edge = smoothstep(0.38, 0.68, diamond)
                * (1.0 - smoothstep(0.68, 1.08, diamond));
        float core = 1.0 - smoothstep(0.0, 0.30, diamond);
        float glint = pow(max(0.0, sin((p.x - p.y) * 18.0 + time * 1.35)), 9.0);
        float opacity = alpha * body * (edge * 0.78 + core * 0.42 + glint * 0.32);
        if (opacity < 0.004) {
            discard;
        }

        vec3 color = mix(tint, ivory, edge * 0.52 + core * 0.34);
        color = mix(color, pearl, core * 0.56 + glint * 0.72);
        fragColor = vec4(color * (1.10 + edge * 0.82 + core * 1.18 + glint),
                clamp(opacity, 0.0, 1.0));
        return;
    }

    discard;
}
