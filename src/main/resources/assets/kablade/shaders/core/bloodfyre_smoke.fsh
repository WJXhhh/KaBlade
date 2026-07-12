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
    float value = 0.0; float weight = 0.56;
    for (int i = 0; i < 5; i++) { value += noise(p) * weight; p = p * 2.01 + vec2(5.2, 8.3); weight *= 0.47; }
    return value;
}

void main() {
    float kind = floor(texCoord0.x * 0.5);
    float u = fract(texCoord0.x * 0.5) * 2.0;
    float v = clamp(texCoord0.y, 0.0, 1.0);
    float time = GameTime * 240.0;
    vec2 p = vec2(u, v) * 2.0 - 1.0;
    float radius = length(p);
    float low = fbm(p * 2.15 + vec2(time * 0.028, -time * 0.042));
    float curls = fbm(p * 4.6 + vec2(-time * 0.052, time * 0.022));
    float holes = smoothstep(0.72, 0.91, fbm(p * 8.4 + time * 0.018) + low * 0.10);
    float cloud = smoothstep(0.20, 0.68, low * 0.70 + curls * 0.46);
    cloud *= 1.0 - smoothstep(0.42, 1.04, radius + (low - 0.5) * 0.30);
    float alpha = vertexColor.a * ColorModulator.a * cloud * (1.0 - holes * 0.94);
    vec3 color = mix(vec3(0.008, 0.003, 0.005), vec3(0.11, 0.018, 0.024), low);
    float ember = smoothstep(0.64, 0.82, curls) * (1.0 - smoothstep(0.82, 0.96, curls));
    color += vec3(0.42, 0.014, 0.006) * ember * vertexColor.a;
    if (kind > 0.5) {
        alpha *= smoothstep(0.0, 0.12, v) * (1.0 - smoothstep(0.82, 1.0, v));
        color *= 0.78;
    }
    if (alpha < 0.006) discard;
    fragColor = vec4(color, clamp(alpha, 0.0, 0.88));
}
