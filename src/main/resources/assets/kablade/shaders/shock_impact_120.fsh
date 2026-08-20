#version 120

uniform float uTime;
uniform float uStrength;
varying vec4 vColor;
varying vec3 vLocal;

void main() {
    if (vColor.a < 0.012) {
        discard;
    }
    float pulse = 0.92 + 0.08 * sin(uTime * 0.72 + vLocal.x * 2.4 + vLocal.y * 1.7);
    float feather = smoothstep(0.012, 0.46, vColor.a);
    float core = smoothstep(0.18, 0.82, vColor.a);
    vec3 cyan = mix(vColor.rgb, vec3(0.72, 0.98, 1.0), core * 0.34);
    cyan *= pulse * (1.0 + core * 0.32 * uStrength);
    gl_FragColor = vec4(cyan, clamp(vColor.a * feather * (1.0 + core * 0.18), 0.0, 1.0));
}
