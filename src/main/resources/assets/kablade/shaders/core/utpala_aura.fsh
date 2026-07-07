#version 150

uniform vec4 ColorModulator;
uniform float GameTime;

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

float hash(float n) {
    return fract(sin(n) * 43758.5453123);
}

float hash2(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

float valueNoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash2(i), hash2(i + vec2(1.0, 0.0)), u.x),
               mix(hash2(i + vec2(0.0, 1.0)), hash2(i + vec2(1.0, 1.0)), u.x), u.y);
}

float softTaper(float u) {
    return smoothstep(0.00, 0.10, u) * (1.0 - smoothstep(0.90, 1.0, u));
}

void main() {
    float kind = floor(texCoord0.x / 2.0);
    float u = fract(texCoord0.x / 2.0) * 2.0;
    float v = clamp(texCoord0.y, 0.0, 1.0);
    float across = abs(v * 2.0 - 1.0);
    float time = GameTime * 240.0;

    vec3 tint = vertexColor.rgb * ColorModulator.rgb;
    float alpha = vertexColor.a * ColorModulator.a;

    if (kind < 0.5) {
        float body = 1.0 - smoothstep(0.18, 1.00, across);
        float core = 1.0 - smoothstep(0.00, 0.28, across);
        float hot = 1.0 - smoothstep(0.00, 0.08, across);
        float flow = valueNoise(vec2(u * 4.5 - time * 0.075, v * 2.2 + time * 0.035));
        float softLine = pow(max(0.0, sin(u * 8.0 - time * 0.55 + v * 2.0)), 6.0) * 0.12;
        float broken = smoothstep(0.40, 0.88, valueNoise(vec2(u * 8.0 - time * 0.11, v * 3.2)));
        float feather = 1.0 - smoothstep(0.74, 1.0, across + broken * 0.08);
        float flicker = 0.86 + 0.14 * sin(time * 0.72 + u * 5.0 + v * 2.0);
        float opacity = alpha * flicker * feather * (body * 0.48 + core * 0.72 + hot * 0.50 + softLine * 0.18 + flow * 0.08);
        if (opacity < 0.004) {
            discard;
        }

        vec3 deep = vec3(0.035, 0.32, 1.0);
        vec3 cyan = vec3(0.20, 0.82, 1.0);
        vec3 white = vec3(0.88, 1.0, 1.0);
        vec3 color = mix(deep, cyan, body * 0.72 + flow * 0.10);
        color = mix(color, white, core * 0.58 + hot * 0.42);
        color = mix(color, tint, 0.28);
        fragColor = vec4(color * (1.04 + core * 1.10 + hot * 1.45 + softLine * 0.45),
                clamp(opacity, 0.0, 1.0));
        return;
    }

    if (kind < 1.5) {
        float taper = softTaper(u);
        float body = 1.0 - smoothstep(0.20, 1.0, across);
        float core = 1.0 - smoothstep(0.00, 0.24, across);
        float bladeHot = 1.0 - smoothstep(0.00, 0.055, across);
        float torn = valueNoise(vec2(u * 10.0 - time * 0.24, v * 4.0));
        float shred = smoothstep(0.58, 0.92, torn) * (1.0 - smoothstep(0.70, 1.0, across));
        float filament = pow(max(0.0, sin(u * 13.0 - time * 0.85 + v * 3.0)), 6.0) * 0.16;
        float opacity = alpha * taper * (body * 0.42 + core * 0.95 + bladeHot * 1.16 + shred * 0.18 + filament * 0.14);
        if (opacity < 0.004) {
            discard;
        }

        vec3 blue = vec3(0.05, 0.34, 1.0);
        vec3 edge = vec3(0.15, 0.74, 1.0);
        vec3 white = vec3(0.92, 1.0, 1.0);
        vec3 color = mix(blue, edge, body * 0.74 + shred * 0.18);
        color = mix(color, white, core * 0.74 + bladeHot * 0.68 + filament * 0.30);
        color = mix(color, tint, 0.20);
        fragColor = vec4(color * (1.30 + core * 1.30 + bladeHot * 2.05 + filament * 0.45),
                clamp(opacity, 0.0, 1.0));
        return;
    }

    if (kind < 2.5) {
        vec2 p = vec2(u * 2.0 - 1.0, v * 2.0 - 1.0);
        float facet = abs(p.x) * 0.68 + abs(p.y);
        float body = 1.0 - smoothstep(0.80, 1.08, facet);
        float edge = smoothstep(0.42, 0.82, facet) * (1.0 - smoothstep(0.82, 1.08, facet));
        float core = 1.0 - smoothstep(0.00, 0.36, facet);
        float glint = pow(max(0.0, sin((p.x - p.y) * 20.0 + time * 1.9)), 10.0);
        float blink = 0.70 + 0.30 * sin(time * 2.4 + floor(u * 7.0) * 1.7);
        float opacity = alpha * body * blink * (edge * 0.78 + core * 0.38 + glint * 0.42);
        if (opacity < 0.004) {
            discard;
        }

        vec3 blue = vec3(0.04, 0.48, 1.0);
        vec3 ice = vec3(0.50, 0.94, 1.0);
        vec3 white = vec3(0.94, 1.0, 1.0);
        vec3 color = mix(blue, ice, edge + core * 0.36);
        color = mix(color, white, core * 0.55 + glint * 0.70);
        color = mix(color, tint, 0.22);
        fragColor = vec4(color * (1.10 + edge * 0.78 + core * 1.20 + glint * 1.55),
                clamp(opacity, 0.0, 1.0));
        return;
    }

    if (kind < 3.5) {
        float taper = softTaper(u);
        float crack = pow(max(0.0, sin(u * 14.0 + v * 5.0 - time * 0.72)), 7.0);
        float frost = valueNoise(vec2(u * 18.0 + time * 0.10, v * 10.0));
        float body = 1.0 - smoothstep(0.24, 1.0, across);
        float core = 1.0 - smoothstep(0.00, 0.20, across);
        float opacity = alpha * (body * 0.40 + core * 0.45 + crack * 0.30 + frost * 0.12) * max(taper, 0.48);
        if (opacity < 0.004) {
            discard;
        }

        vec3 color = mix(vec3(0.03, 0.38, 1.0), vec3(0.52, 0.95, 1.0), body + crack * 0.28);
        color = mix(color, vec3(0.92, 1.0, 1.0), core * 0.55 + crack * 0.28);
        fragColor = vec4(color * (0.95 + core * 0.90 + crack * 1.10), clamp(opacity, 0.0, 1.0));
        return;
    }

    if (kind < 4.5) {
        vec2 p = vec2(u * 2.0 - 1.0, v * 2.0 - 1.0);
        float oval = 1.0 - smoothstep(0.55, 1.08, length(p * vec2(0.62, 1.18)));
        float mist = valueNoise(vec2(u * 5.0 - time * 0.045, v * 3.0 + time * 0.025));
        float pulse = 0.72 + 0.28 * sin(time * 0.32 + u * 5.0);
        float opacity = alpha * oval * pulse * (0.62 + mist * 0.30);
        if (opacity < 0.003) {
            discard;
        }

        vec3 midnight = vec3(0.005, 0.025, 0.085);
        vec3 coldBlue = vec3(0.025, 0.13, 0.30);
        vec3 color = mix(midnight, coldBlue, mist * 0.45 + oval * 0.25);
        color = mix(color, tint * 0.30, 0.12);
        fragColor = vec4(color, clamp(opacity, 0.0, 0.42));
        return;
    }

    discard;
}
