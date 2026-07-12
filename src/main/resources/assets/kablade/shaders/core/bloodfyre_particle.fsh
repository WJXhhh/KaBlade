#version 150

uniform vec4 ColorModulator;
uniform float GameTime;
in vec4 vertexColor;
in vec2 texCoord0;
out vec4 fragColor;

float hash(vec2 p) { return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453); }

void main() {
    float kind = floor(texCoord0.x * 0.5);
    float u = fract(texCoord0.x * 0.5) * 2.0;
    float v = clamp(texCoord0.y, 0.0, 1.0);
    float time = GameTime * 240.0;
    float alpha = vertexColor.a * ColorModulator.a;
    vec3 tint = max(vertexColor.rgb, vec3(0.01));
    vec3 color;

    if (kind < 0.5) {
        float vein = 1.0 - smoothstep(0.035, 0.16,
                abs(sin(u * 18.0 + v * 7.0 + hash(vec2(u * 7.0, v * 9.0)) * 2.4)));
        float edge = smoothstep(0.0, 0.08, v) * (1.0 - smoothstep(0.88, 1.0, v));
        color = mix(tint * vec3(0.38, 0.14, 0.20), tint, 0.58);
        color = mix(color, vec3(1.0, 0.08, 0.14), vein * 0.46);
        alpha *= edge * (0.74 + vein * 0.22);
    } else if (kind < 1.5) {
        vec2 p = vec2(u, v) * 2.0 - 1.0;
        float diamond = abs(p.x) + abs(p.y);
        float body = 1.0 - smoothstep(0.50, 1.0, diamond);
        float core = 1.0 - smoothstep(0.0, 0.28, diamond);
        color = mix(tint, vec3(1.0, 0.38, 0.055), core);
        alpha *= body;
        color *= 1.10 + core * 1.40;
    } else {
        vec2 p = vec2(u, v) * 2.0 - 1.0;
        float radius = length(p);
        float spark = 1.0 - smoothstep(0.05, 1.0, radius);
        float flicker = 0.74 + 0.26 * sin(time * 0.72 + hash(vec2(u, v)) * 9.0);
        color = mix(vec3(1.0, 0.18, 0.015), vec3(1.0, 0.94, 0.52), spark);
        alpha *= spark * flicker;
        color *= 1.5 + spark * 1.7;
    }

    if (alpha < 0.004) discard;
    fragColor = vec4(color, clamp(alpha, 0.0, 1.0));
}
