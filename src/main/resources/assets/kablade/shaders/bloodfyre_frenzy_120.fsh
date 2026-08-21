#version 120

uniform float uTime;
varying vec4 vColor;
varying vec2 vUv;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 cell = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(cell), hash(cell + vec2(1.0, 0.0)), f.x),
               mix(hash(cell + vec2(0.0, 1.0)), hash(cell + vec2(1.0, 1.0)), f.x), f.y);
}

float fbm(vec2 p) {
    float value = 0.0;
    float weight = 0.55;
    for (int i = 0; i < 4; i++) {
        value += noise(p) * weight;
        p = p * 2.03 + vec2(4.1, 7.7);
        weight *= 0.48;
    }
    return value;
}

void main() {
    float kind = floor(vUv.x * 0.5);
    float u = fract(vUv.x * 0.5) * 2.0;
    float v = clamp(vUv.y, 0.0, 1.0);
    float across = abs(v * 2.0 - 1.0);
    float warped = fbm(vec2(u * 10.0 - uTime * 0.10, v * 4.3 + uTime * 0.045));
    float fine = fbm(vec2(u * 29.0 + uTime * 0.035, v * 12.0 - uTime * 0.025));
    float ragged = 1.0 - smoothstep(0.72, 1.04, across + (warped - 0.5) * 0.30);
    float veins = 1.0 - smoothstep(0.035, 0.17,
            abs(sin(u * 57.0 - v * 16.0 + warped * 12.0 - uTime * 0.18)));
    float pores = smoothstep(0.73, 0.91, fine + warped * 0.14);
    float alpha = vColor.a * ragged * (1.0 - pores * 0.82);
    vec3 color;

    if (kind < 0.5) {
        color = mix(vec3(0.010, 0.0004, 0.0015), vec3(0.19, 0.002, 0.007), warped);
        alpha *= (0.42 + warped * 0.24) * (0.82 - pores * 0.28);
    } else if (kind < 1.5) {
        float grooves = (1.0 - smoothstep(0.05, 0.20,
                abs(sin(u * 19.0 + v * 9.0 + warped * 7.0)))) * smoothstep(0.18, 0.86, across);
        color = mix(vec3(0.16, 0.001, 0.005), vec3(0.78, 0.008, 0.018), 0.30 + warped * 0.62);
        color = mix(color, vec3(0.004, 0.001, 0.002), grooves * 0.94);
        color = mix(color, vec3(1.0, 0.12, 0.008), veins * 0.68);
        alpha *= 0.72 + warped * 0.20;
    } else if (kind < 2.5) {
        float outerEdge = smoothstep(0.10, 0.92, v + (warped - 0.5) * 0.20);
        float hot = veins * smoothstep(0.52, 0.84, fine);
        color = mix(vec3(0.92, 0.018, 0.006), vec3(1.0, 0.34, 0.025), 0.28 + outerEdge * 0.42);
        color = mix(color, vec3(1.0, 0.76, 0.28), hot * 0.32);
        color *= 1.18 + hot * 0.42 + outerEdge * 0.18;
    } else if (kind < 3.5) {
        float filament = 1.0 - smoothstep(0.08, 0.58, across);
        color = mix(vec3(0.86, 0.016, 0.006), vec3(1.0, 0.38, 0.035), filament);
        alpha *= filament * smoothstep(0.24, 0.62, warped);
        color *= 1.20 + filament * 0.70;
    } else {
        float body = 1.0 - smoothstep(0.46, 1.0, across);
        float core = 1.0 - smoothstep(0.0, 0.24, across);
        float hot = veins * smoothstep(0.34, 0.80, fine);
        color = mix(vec3(0.012, 0.0005, 0.002), vec3(0.48, 0.004, 0.012), body);
        color = mix(color, vec3(0.94, 0.020, 0.010), hot * 0.76);
        color = mix(color, vec3(0.002, 0.0004, 0.0005), core * 0.84);
        alpha *= body * (0.76 + hot * 0.32);
    }

    if (alpha < 0.004) discard;
    gl_FragColor = vec4(color * mix(vec3(1.0), max(vColor.rgb, vec3(0.12)), 0.12),
            clamp(alpha, 0.0, 1.0));
}
