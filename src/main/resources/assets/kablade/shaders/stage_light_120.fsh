#version 120

uniform float uTime;
uniform float uStrength;

varying vec4 stageColor;
varying vec3 stagePosition;

void main() {
    float radius = length(stagePosition.xz);
    float angle = atan(stagePosition.z, stagePosition.x);
    float pulse = 0.84 + 0.16 * sin(radius * 5.8 - uTime * 0.16);
    float chase = 0.88 + 0.12 * sin(angle * 6.0 + uTime * 0.22);
    float heightGlow = 0.92 + 0.08 * sin(stagePosition.y * 8.0 - uTime * 0.12);
    float shimmer = pulse * chase * heightGlow;
    float intensity = mix(0.82, 1.18, clamp(uStrength, 0.0, 1.0));
    gl_FragColor = vec4(stageColor.rgb * shimmer * intensity,
                        stageColor.a * (0.86 + 0.14 * chase));
}
