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
    float time = GameTime * 180.0;

    if (texCoord0.x < 2.15) {
        float u = clamp(texCoord0.x - 1.0, 0.0, 1.0);
        float across = abs(texCoord0.y * 2.0 - 1.0);
        float tail = smoothstep(0.00, 0.12, u);
        float tip = 1.0 - smoothstep(0.78, 1.0, u);
        float broad = 1.0 - smoothstep(0.20, 0.82, across);
        float body = 1.0 - smoothstep(0.00, 0.58, across);
        float flow = pow(max(0.0, sin(u * 34.0 - time * 3.10)), 5.0) * (1.0 - across * 0.66);
        float aura = pow(max(0.0, 1.0 - across), 1.7);
        float opacity = alpha * tail * tip * (broad * 0.16 + body * 0.38 + flow * 0.24 + aura * 0.035);
        if (opacity < 0.004) {
            discard;
        }

        vec3 edge = vec3(0.02, 0.58, 1.0);
        vec3 cyan = mix(tint, vec3(0.12, 0.86, 1.0), 0.58);
        vec3 white = vec3(0.82, 1.0, 1.0);
        vec3 color = mix(edge, cyan, body * 0.72 + flow * 0.24);
        color = mix(color, white, flow * 0.24 + aura * 0.08);
        fragColor = vec4(color * (0.82 + body * 0.96 + flow * 0.76), clamp(opacity, 0.0, 1.0));
        return;
    }

    if (texCoord0.x < 3.35) {
        float u = clamp(texCoord0.x - 2.2, 0.0, 1.0);
        float across = abs(texCoord0.y * 2.0 - 1.0);
        float tail = smoothstep(0.00, 0.10, u);
        float tip = 1.0 - smoothstep(0.78, 1.0, u);
        float body = 1.0 - smoothstep(0.00, 0.56, across);
        float inner = 1.0 - smoothstep(0.00, 0.24, across);
        float pulse = pow(max(0.0, sin(u * 42.0 - time * 3.70)), 6.0);
        float hot = pow(max(0.0, 1.0 - across), 3.0);
        float opacity = alpha * tail * tip * (body * 0.50 + inner * 0.64 + pulse * 0.30 + hot * 0.18);
        if (opacity < 0.004) {
            discard;
        }

        vec3 cyan = vec3(0.20, 0.92, 1.0);
        vec3 white = vec3(0.92, 1.0, 1.0);
        vec3 color = mix(cyan, white, inner * 0.58 + pulse * 0.36 + hot * 0.28);
        fragColor = vec4(color * (1.22 + inner * 1.28 + pulse * 1.02), clamp(opacity, 0.0, 1.0));
        return;
    }

    if (texCoord0.x < 5.75) {
        float u = clamp(texCoord0.x - 3.4, 0.0, 1.0);
        float across = abs(texCoord0.y * 2.0 - 1.0);
        float tail = smoothstep(0.00, 0.08, u);
        float tip = 1.0 - smoothstep(0.68, 1.0, u);
        float razor = 1.0 - smoothstep(0.00, 0.24, across);
        float fringe = 1.0 - smoothstep(0.05, 0.92, across);
        float shimmer = 0.78 + 0.22 * sin(time * 4.2 + u * 28.0);
        float opacity = alpha * tail * tip * shimmer * (fringe * 0.20 + razor * 1.18);
        if (opacity < 0.004) {
            discard;
        }

        vec3 color = mix(vec3(0.16, 0.84, 1.0), vec3(0.96, 1.0, 1.0), razor * 0.82);
        fragColor = vec4(color * (1.36 + razor * 1.65), clamp(opacity, 0.0, 1.0));
        return;
    }

    if (texCoord0.x < 7.15) {
        float u = clamp(texCoord0.x - 6.0, 0.0, 1.0);
        float across = abs(texCoord0.y * 2.0 - 1.0);
        float taper = smoothstep(0.00, 0.16, u) * (1.0 - smoothstep(0.58, 1.0, u));
        float streak = 1.0 - smoothstep(0.00, 1.0, across);
        float core = 1.0 - smoothstep(0.00, 0.30, across);
        float broken = smoothstep(0.08, 0.44, hash(floor(u * 44.0) + floor(across * 7.0) * 13.0));
        float opacity = alpha * taper * broken * (streak * 0.54 + core * 0.78);
        if (opacity < 0.004) {
            discard;
        }

        vec3 color = mix(vec3(0.04, 0.62, 1.0), vec3(0.78, 1.0, 1.0), core * 0.54);
        fragColor = vec4(color * (1.00 + core * 1.20), clamp(opacity, 0.0, 1.0));
        return;
    }

    if (texCoord0.x < 9.15) {
        vec2 p = vec2((texCoord0.x - 8.0) * 2.0 - 1.0, texCoord0.y * 2.0 - 1.0);
        float radius = length(p);
        if (radius > 1.0) {
            discard;
        }

        float border = smoothstep(0.46, 0.70, radius) * (1.0 - smoothstep(0.70, 0.98, radius));
        float fill = 1.0 - smoothstep(0.68, 0.98, radius);
        float blink = 0.72 + 0.28 * sin(time * 4.8 + floor(texCoord0.y * 7.0));
        float opacity = alpha * blink * (border * 0.88 + fill * 0.12);
        if (opacity < 0.004) {
            discard;
        }

        vec3 color = mix(vec3(0.08, 0.66, 1.0), vec3(0.86, 1.0, 1.0), border * 0.58);
        fragColor = vec4(color * (0.96 + border * 1.62), clamp(opacity, 0.0, 1.0));
        return;
    }

    if (texCoord0.x < 11.15) {
        vec2 p = vec2((texCoord0.x - 10.0) * 2.0 - 1.0, texCoord0.y * 2.0 - 1.0);
        float diamond = abs(p.x) + abs(p.y);
        if (diamond > 1.05) {
            discard;
        }

        float border = smoothstep(0.52, 0.70, diamond) * (1.0 - smoothstep(0.70, 0.98, diamond));
        float core = 1.0 - smoothstep(0.00, 0.38, diamond);
        float blink = 0.82 + 0.18 * sin(time * 5.6 + floor(texCoord0.y * 9.0));
        float opacity = alpha * blink * (border * 0.94 + core * 0.18);
        if (opacity < 0.004) {
            discard;
        }

        vec3 color = mix(vec3(0.10, 0.72, 1.0), vec3(0.94, 1.0, 1.0), border * 0.70 + core * 0.34);
        fragColor = vec4(color * (1.18 + border * 1.55 + core * 0.70), clamp(opacity, 0.0, 1.0));
        return;
    }

    discard;
}
