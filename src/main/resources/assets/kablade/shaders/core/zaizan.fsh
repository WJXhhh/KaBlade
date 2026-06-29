#version 150

uniform vec4 ColorModulator;
uniform float GameTime;

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

float hash(float n) {
    return fract(sin(n) * 43758.5453123);
}

void main() {
    vec3 tint = vertexColor.rgb * ColorModulator.rgb;
    float alpha = vertexColor.a * ColorModulator.a;
    float time = GameTime * 210.0;

    if (texCoord0.x < 1.25) {
        float u = fract(texCoord0.x);
        float across = abs(texCoord0.y * 2.0 - 1.0);
        float rim = 1.0 - smoothstep(0.16, 0.96, across);
        float core = 1.0 - smoothstep(0.00, 0.30, across);
        float flow = pow(max(0.0, sin(u * 38.0 - time * 2.45)), 5.0);
        float ticks = smoothstep(0.62, 1.0, hash(floor(u * 42.0) + floor(time * 0.045)));
        float blink = 0.76 + 0.24 * sin(time * 2.2 + u * 18.0);
        float opacity = alpha * blink * (rim * 0.62 + core * 0.76 + flow * 0.34 + ticks * 0.28);
        if (opacity < 0.004) {
            discard;
        }

        vec3 red = mix(tint, vec3(1.0, 0.06, 0.02), 0.72);
        vec3 orange = vec3(1.0, 0.24, 0.06);
        vec3 white = vec3(1.0, 0.88, 0.70);
        vec3 color = mix(red, orange, core * 0.55 + flow * 0.30);
        color = mix(color, white, core * 0.34 + flow * 0.22);
        fragColor = vec4(color * (1.15 + core * 1.38 + flow * 1.05 + ticks * 0.68),
                clamp(opacity, 0.0, 1.0));
        return;
    }

    if (texCoord0.x >= 2.0 && texCoord0.x < 3.25) {
        float u = clamp(texCoord0.x - 2.0, 0.0, 1.0);
        float across = abs(texCoord0.y * 2.0 - 1.0);
        float taper = smoothstep(0.00, 0.10, u) * (1.0 - smoothstep(0.82, 1.0, u));
        float body = 1.0 - smoothstep(0.34, 1.00, across);
        float core = 1.0 - smoothstep(0.00, 0.28, across);
        float hot = 1.0 - smoothstep(0.00, 0.105, across);
        float crack = pow(max(0.0, sin(u * 56.0 + across * 8.0 - time * 3.8)), 7.0);
        float broken = smoothstep(0.08, 0.38, hash(floor(u * 70.0) + floor(across * 8.0) * 19.0));
        float opacity = alpha * taper * (body * 0.54 + core * 0.94 + hot * 1.30 + crack * broken * 0.40);
        if (opacity < 0.004) {
            discard;
        }

        vec3 edge = vec3(1.0, 0.04, 0.02);
        vec3 red = vec3(1.0, 0.10, 0.045);
        vec3 white = vec3(1.0, 0.96, 0.88);
        vec3 color = mix(edge, red, body * 0.65);
        color = mix(color, white, core * 0.72 + hot * 0.92 + crack * 0.22);
        fragColor = vec4(color * (1.42 + core * 1.40 + hot * 2.35 + crack * 1.02),
                clamp(opacity, 0.0, 1.0));
        return;
    }

    if (texCoord0.x >= 4.0 && texCoord0.x < 5.25) {
        float u = clamp(texCoord0.x - 4.0, 0.0, 1.0);
        float across = abs(texCoord0.y * 2.0 - 1.0);
        float edge = 1.0 - smoothstep(0.06, 0.96, across);
        float core = 1.0 - smoothstep(0.00, 0.28, across);
        float pulse = pow(max(0.0, sin(u * 44.0 - time * 1.85)), 6.0);
        float opacity = alpha * (edge * 0.42 + core * 0.56 + pulse * 0.28);
        if (opacity < 0.004) {
            discard;
        }

        vec3 color = mix(vec3(0.86, 0.02, 0.02), vec3(1.0, 0.24, 0.08), core + pulse * 0.45);
        fragColor = vec4(color * (0.96 + core * 0.88 + pulse * 1.05), clamp(opacity, 0.0, 1.0));
        return;
    }

    if (texCoord0.x >= 6.0 && texCoord0.x < 7.25) {
        vec2 p = vec2((texCoord0.x - 6.0) * 2.0 - 1.0, texCoord0.y * 2.0 - 1.0);
        float diamond = abs(p.x) * 0.72 + abs(p.y);
        if (diamond > 1.05) {
            discard;
        }

        float border = smoothstep(0.46, 0.68, diamond) * (1.0 - smoothstep(0.68, 1.0, diamond));
        float core = 1.0 - smoothstep(0.00, 0.34, diamond);
        float blink = 0.70 + 0.30 * sin(time * 4.6 + floor(texCoord0.y * 8.0));
        float opacity = alpha * blink * (border * 0.70 + core * 0.24);
        if (opacity < 0.004) {
            discard;
        }

        vec3 color = mix(vec3(1.0, 0.025, 0.035), vec3(1.0, 0.18, 0.20), border * 0.56 + core * 0.22);
        fragColor = vec4(color * (1.02 + border * 0.92 + core * 0.62), clamp(opacity, 0.0, 1.0));
        return;
    }

    if (texCoord0.x >= 8.0 && texCoord0.x < 9.25) {
        float u = clamp(texCoord0.x - 8.0, 0.0, 1.0);
        float v = clamp(texCoord0.y, 0.0, 1.0);
        float edgeTaper = smoothstep(0.00, 0.16, u) * (1.0 - smoothstep(0.82, 1.0, u));
        float center = 1.0 - smoothstep(0.00, 0.72, abs(v - 0.52) * 2.0);
        float leftGlow = 1.0 - smoothstep(0.00, 0.50, u);
        float rightGlow = smoothstep(0.20, 0.88, u);
        float wave = 0.5 + 0.5 * sin(u * 20.0 + v * 8.0 - time * 0.95);
        float torn = smoothstep(0.20, 0.88, wave)
                * smoothstep(0.05, 0.28, v)
                * (1.0 - smoothstep(0.76, 1.0, v));
        float opacity = alpha * edgeTaper * (center * 0.48 + leftGlow * 0.24 + rightGlow * 0.14 + torn * 0.20);
        if (opacity < 0.004) {
            discard;
        }

        vec3 red = vec3(1.0, 0.04, 0.035);
        vec3 rose = vec3(1.0, 0.28, 0.36);
        vec3 white = vec3(1.0, 0.78, 0.72);
        vec3 color = mix(red, rose, center * 0.72 + torn * 0.36);
        color = mix(color, white, center * leftGlow * 0.38);
        fragColor = vec4(color * (0.96 + center * 0.90 + torn * 0.58), clamp(opacity, 0.0, 1.0));
        return;
    }

    discard;
}
