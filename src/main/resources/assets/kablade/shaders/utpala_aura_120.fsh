#version 120

uniform float uTime;
uniform float uStrength;
varying vec4 vColor;
varying vec2 vUv;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), u.x),
               mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x), u.y);
}

void main() {
    float kind = floor(vUv.x / 2.0);
    float u = fract(vUv.x / 2.0) * 2.0;
    float across = abs(vUv.y * 2.0 - 1.0);
    float body = 1.0 - smoothstep(0.18, 1.0, across);
    float core = 1.0 - smoothstep(0.0, 0.25, across);
    float flow = noise(vec2(u * (5.0 + kind), vUv.y * 3.0 - uTime * 0.07));
    float flicker = 0.86 + 0.14 * sin(uTime * 0.55 + u * 7.0 + kind * 1.7);
    float alpha = vColor.a * uStrength * flicker * (0.06 + body * 0.54 + core * 0.58 + flow * 0.08);
    if (alpha < 0.004) discard;

    vec3 deep = vec3(0.025, 0.25, 0.92);
    vec3 cyan = vec3(0.12, 0.78, 1.0);
    vec3 ice = vec3(0.90, 1.0, 1.0);
    vec3 color = mix(deep, cyan, body * 0.78 + flow * 0.12);
    color = mix(color, ice, core * 0.74);
    color = mix(color, vColor.rgb, 0.27);
    if (kind > 3.5) color *= vec3(0.22, 0.34, 0.56);
    gl_FragColor = vec4(color * (1.08 + core * 1.35 + flow * 0.24), clamp(alpha, 0.0, 1.0));
}
