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
    float time = GameTime * 120.0;

    if (texCoord0.x < 2.15) {
        float u = clamp(texCoord0.x - 1.0, 0.0, 1.0);
        float across = abs(texCoord0.y * 2.0 - 1.0);
        float tail = smoothstep(0.00, 0.12, u);
        float tip = 1.0 - smoothstep(0.78, 1.0, u);
        float broad = 1.0 - smoothstep(0.12, 1.0, across);
        float body = 1.0 - smoothstep(0.00, 0.78, across);
        float flow = pow(max(0.0, sin(u * 24.0 - time * 1.85)), 5.0) * (1.0 - across * 0.72);
        float opacity = alpha * tail * tip * (broad * 0.30 + body * 0.50 + flow * 0.22);
        if (opacity < 0.004) {
            discard;
        }

        vec3 edge = vec3(0.02, 0.58, 1.0);
        vec3 cyan = mix(tint, vec3(0.12, 0.86, 1.0), 0.58);
        vec3 white = vec3(0.82, 1.0, 1.0);
        vec3 color = mix(edge, cyan, body * 0.72 + flow * 0.24);
        color = mix(color, white, flow * 0.16);
        fragColor = vec4(color * (0.80 + body * 0.82 + flow * 0.62), clamp(opacity, 0.0, 1.0));
        return;
    }

    if (texCoord0.x < 3.35) {
        float u = clamp(texCoord0.x - 2.2, 0.0, 1.0);
        float across = abs(texCoord0.y * 2.0 - 1.0);
        float tail = smoothstep(0.00, 0.10, u);
        float tip = 1.0 - smoothstep(0.78, 1.0, u);
        float body = 1.0 - smoothstep(0.00, 0.70, across);
        float inner = 1.0 - smoothstep(0.00, 0.36, across);
        float pulse = pow(max(0.0, sin(u * 30.0 - time * 2.25)), 6.0);
        float opacity = alpha * tail * tip * (body * 0.58 + inner * 0.42 + pulse * 0.24);
        if (opacity < 0.004) {
            discard;
        }

        vec3 cyan = vec3(0.20, 0.92, 1.0);
        vec3 white = vec3(0.92, 1.0, 1.0);
        vec3 color = mix(cyan, white, inner * 0.46 + pulse * 0.28);
        fragColor = vec4(color * (1.10 + inner * 0.72 + pulse * 0.90), clamp(opacity, 0.0, 1.0));
        return;
    }

    if (texCoord0.x < 5.75) {
        float u = clamp(texCoord0.x - 3.4, 0.0, 1.0);
        float across = abs(texCoord0.y * 2.0 - 1.0);
        float tail = smoothstep(0.00, 0.08, u);
        float tip = 1.0 - smoothstep(0.68, 1.0, u);
        float razor = 1.0 - smoothstep(0.00, 0.34, across);
        float fringe = 1.0 - smoothstep(0.05, 0.92, across);
        float shimmer = 0.78 + 0.22 * sin(time * 2.8 + u * 20.0);
        float opacity = alpha * tail * tip * shimmer * (fringe * 0.26 + razor * 0.92);
        if (opacity < 0.004) {
            discard;
        }

        vec3 color = mix(vec3(0.16, 0.84, 1.0), vec3(0.96, 1.0, 1.0), razor * 0.82);
        fragColor = vec4(color * (1.18 + razor * 1.30), clamp(opacity, 0.0, 1.0));
        return;
    }

    if (texCoord0.x < 7.15) {
        float u = clamp(texCoord0.x - 6.0, 0.0, 1.0);
        float across = abs(texCoord0.y * 2.0 - 1.0);
        float taper = smoothstep(0.00, 0.16, u) * (1.0 - smoothstep(0.58, 1.0, u));
        float streak = 1.0 - smoothstep(0.00, 1.0, across);
        float core = 1.0 - smoothstep(0.00, 0.30, across);
        float broken = smoothstep(0.12, 0.54, hash(floor(u * 34.0) + floor(across * 7.0) * 13.0));
        float opacity = alpha * taper * broken * (streak * 0.42 + core * 0.62);
        if (opacity < 0.004) {
            discard;
        }

        vec3 color = mix(vec3(0.04, 0.62, 1.0), vec3(0.78, 1.0, 1.0), core * 0.54);
        fragColor = vec4(color * (0.88 + core * 0.96), clamp(opacity, 0.0, 1.0));
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
        float blink = 0.72 + 0.28 * sin(time * 2.9 + floor(texCoord0.y * 7.0));
        float opacity = alpha * blink * (border * 0.88 + fill * 0.12);
        if (opacity < 0.004) {
            discard;
        }

        vec3 color = mix(vec3(0.08, 0.66, 1.0), vec3(0.86, 1.0, 1.0), border * 0.58);
        fragColor = vec4(color * (0.86 + border * 1.35), clamp(opacity, 0.0, 1.0));
        return;
    }

    discard;
}
