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
    for (int i = 0; i < 4; i++) { value += noise(p) * weight; p = p * 2.02 + vec2(7.1, 3.9); weight *= 0.48; }
    return value;
}

void main() {
    float kind = floor(texCoord0.x * 0.5);
    float u = fract(texCoord0.x * 0.5) * 2.0;
    float v = clamp(texCoord0.y, 0.0, 1.0);
    float time = GameTime * 240.0;
    vec2 p = vec2(v * 2.0 - 1.0, u);
    float n = fbm(vec2(p.x * 4.2 + time * 0.08, p.y * 7.0 - time * 0.42));
    p.x += (n - 0.5) * (0.38 + p.y * 0.18);
    float width = mix(0.82, 0.045, pow(p.y, 0.74));
    float flame = 1.0 - smoothstep(width * 0.58, width, abs(p.x));
    float endMask = smoothstep(0.0, 0.055, u) * (1.0 - smoothstep(0.86, 1.0, u));
    float fibers = 1.0 - smoothstep(0.035, 0.15, abs(sin(u * 31.0 - v * 13.0 + n * 8.0)));
    float holes = smoothstep(0.76, 0.92, fbm(vec2(u * 19.0, v * 15.0 + time * 0.04)) + n * 0.12);
    float alpha = vertexColor.a * ColorModulator.a * flame * endMask * (1.0 - holes * 0.92);
    vec3 tint = max(vertexColor.rgb, vec3(0.02));
    vec3 color;

    if (kind < 0.5) {
        color = mix(vec3(0.025, 0.002, 0.004), vec3(0.46, 0.006, 0.01), n);
        alpha *= 0.72 + fibers * 0.24;
    } else if (kind < 1.5) {
        color = mix(vec3(0.72, 0.006, 0.012), vec3(1.0, 0.22, 0.018), n);
        color = mix(color, vec3(1.0, 0.75, 0.24), fibers * 0.45);
        color *= 1.30 + fibers * 0.82;
    } else if (kind < 2.5) {
        float core = 1.0 - smoothstep(0.0, width * 0.30, abs(p.x));
        color = mix(vec3(1.0, 0.22, 0.018), vec3(1.0, 0.99, 0.88), core);
        alpha *= 0.68 + core * 0.52;
        color *= 2.15 + core * 3.10;
    } else {
        vec2 discUv = vec2(fract(texCoord0.x * 0.5) * 2.0, v) * 2.0 - 1.0;
        float radius = length(discUv);
        float rays = pow(max(0.0, sin(atan(discUv.y, discUv.x) * 9.0 + n * 4.0)), 7.0);
        float disc = 1.0 - smoothstep(0.06, 0.95, radius);
        alpha = vertexColor.a * ColorModulator.a * (disc * 0.52 + rays * 0.72);
        color = mix(vec3(1.0, 0.10, 0.015), vec3(1.0, 0.99, 0.86), 1.0 - smoothstep(0.0, 0.34, radius));
        color *= 2.10 + rays * 2.80;
    }

    if (alpha < 0.004) discard;
    fragColor = vec4(color * mix(vec3(1.0), tint, 0.16), clamp(alpha, 0.0, 1.0));
}
