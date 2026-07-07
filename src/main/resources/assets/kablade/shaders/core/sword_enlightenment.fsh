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

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash2(i), hash2(i + vec2(1.0, 0.0)), u.x),
               mix(hash2(i + vec2(0.0, 1.0)), hash2(i + vec2(1.0, 1.0)), u.x), u.y);
}

float taper(float u) {
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
        float body = 1.0 - smoothstep(0.16, 1.00, across);
        float core = 1.0 - smoothstep(0.00, 0.26, across);
        float hot = 1.0 - smoothstep(0.00, 0.075, across);
        float flow = pow(max(0.0, sin(u * 28.0 - time * 0.78 + v * 4.0)), 5.0);
        float torn = noise(vec2(u * 9.5 - time * 0.16, v * 4.0 + time * 0.045));
        float feather = 1.0 - smoothstep(0.70, 1.0, across + torn * 0.10);
        float opacity = alpha * taper(u) * feather * (body * 0.42 + core * 0.64 + hot * 0.54 + flow * 0.18);
        if (opacity < 0.004) {
            discard;
        }

        vec3 deep = vec3(0.22, 0.03, 0.66);
        vec3 violet = mix(vec3(0.58, 0.22, 1.0), tint, 0.30);
        vec3 white = vec3(1.0, 0.88, 1.0);
        vec3 color = mix(deep, violet, body * 0.82 + flow * 0.14);
        color = mix(color, white, core * 0.58 + hot * 0.48 + flow * 0.22);
        fragColor = vec4(color * (1.04 + core * 1.10 + hot * 1.70 + flow * 0.60),
                clamp(opacity, 0.0, 1.0));
        return;
    }

    if (kind < 1.5) {
        float body = 1.0 - smoothstep(0.12, 1.0, across);
        float core = 1.0 - smoothstep(0.00, 0.18, across);
        float needle = 1.0 - smoothstep(0.00, 0.050, across);
        float filament = pow(max(0.0, sin(u * 36.0 - time * 1.05 + v * 6.0)), 7.0);
        float broken = smoothstep(0.44, 0.88, noise(vec2(u * 14.0 - time * 0.26, v * 6.0)));
        float opacity = alpha * taper(u) * (body * 0.30 + core * 0.72 + needle * 1.10 + filament * 0.22)
                * (0.82 + broken * 0.18);
        if (opacity < 0.004) {
            discard;
        }

        vec3 color = mix(vec3(0.34, 0.04, 0.92), vec3(0.88, 0.58, 1.0), body);
        color = mix(color, vec3(1.0, 0.92, 1.0), core * 0.76 + needle * 0.72 + filament * 0.32);
        fragColor = vec4(color * (1.30 + core * 1.32 + needle * 2.10), clamp(opacity, 0.0, 1.0));
        return;
    }

    if (kind < 2.5) {
        float band = 1.0 - smoothstep(0.10, 1.0, across);
        float core = 1.0 - smoothstep(0.00, 0.22, across);
        float rune = pow(max(0.0, sin(u * 92.0 + time * 0.32)), 10.0) * (1.0 - across);
        float broken = smoothstep(0.22, 0.86, noise(vec2(u * 20.0 + time * 0.12, v * 5.0)));
        float opacity = alpha * (band * 0.38 + core * 0.52 + rune * 0.30) * (0.72 + broken * 0.28);
        if (opacity < 0.004) {
            discard;
        }

        vec3 color = mix(vec3(0.46, 0.12, 1.0), vec3(1.0, 0.84, 1.0), core * 0.70 + rune * 0.70);
        fragColor = vec4(color * (1.00 + core * 1.15 + rune * 1.40), clamp(opacity, 0.0, 1.0));
        return;
    }

    if (kind < 3.5) {
        vec2 p = vec2(u * 2.0 - 1.0, v * 2.0 - 1.0);
        float r = length(p);
        float disc = 1.0 - smoothstep(0.20, 1.0, r);
        float ring = smoothstep(0.28, 0.44, r) * (1.0 - smoothstep(0.44, 0.72, r));
        float cross = 1.0 - smoothstep(0.00, 0.060, min(abs(p.x), abs(p.y)));
        float diag = 1.0 - smoothstep(0.00, 0.055, min(abs(p.x + p.y), abs(p.x - p.y)));
        float rays = max(cross, diag) * (1.0 - smoothstep(0.20, 1.08, r));
        float sparkle = pow(max(0.0, sin((p.x - p.y) * 18.0 + time * 1.6)), 8.0) * disc;
        float opacity = alpha * (disc * 0.22 + ring * 0.68 + rays * 0.92 + sparkle * 0.34);
        if (opacity < 0.004) {
            discard;
        }

        vec3 color = mix(vec3(0.60, 0.16, 1.0), vec3(1.0, 0.90, 1.0), ring * 0.58 + rays * 0.78 + sparkle);
        fragColor = vec4(color * (1.16 + rays * 1.55 + sparkle * 1.10), clamp(opacity, 0.0, 1.0));
        return;
    }

    if (kind < 4.5) {
        vec2 p = vec2(u * 2.0 - 1.0, v * 2.0 - 1.0);
        float diamond = abs(p.x) * 0.66 + abs(p.y);
        float body = 1.0 - smoothstep(0.74, 1.08, diamond);
        float edge = smoothstep(0.42, 0.72, diamond) * (1.0 - smoothstep(0.72, 1.08, diamond));
        float core = 1.0 - smoothstep(0.00, 0.34, diamond);
        float glint = pow(max(0.0, sin((p.x - p.y) * 20.0 + time * 1.8)), 10.0);
        float opacity = alpha * body * (edge * 0.82 + core * 0.36 + glint * 0.42);
        if (opacity < 0.004) {
            discard;
        }

        vec3 color = mix(vec3(0.44, 0.10, 1.0), vec3(0.92, 0.62, 1.0), edge + core * 0.30);
        color = mix(color, vec3(1.0, 0.92, 1.0), core * 0.50 + glint * 0.78);
        fragColor = vec4(color * (1.08 + edge * 0.84 + core * 1.22 + glint * 1.50),
                clamp(opacity, 0.0, 1.0));
        return;
    }

    discard;
}
