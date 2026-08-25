#version 120

uniform float uTime;
varying vec4 vColor;
varying vec3 vLocal;
varying vec3 vViewNormal;

void main() {
    float facing = abs(normalize(vViewNormal).z);
    float fresnel = pow(1.0 - clamp(facing, 0.0, 1.0), 1.55);
    float plasma = 0.92 + 0.08 * sin(uTime * 19.0
        + atan(vLocal.z, vLocal.x) * 7.0 + length(vLocal) * 4.0);
    vec3 deepRed = vec3(1.0, 0.09, 0.01);
    vec3 hotOrange = vec3(1.0, 0.52, 0.07);
    vec3 coreGold = vec3(1.0, 0.94, 0.42);
    vec3 color = mix(deepRed, hotOrange, fresnel);
    color = mix(color, coreGold, pow(fresnel, 3.0) * 0.72);
    color *= vColor.rgb * plasma * (1.10 + fresnel * 0.55);
    float alpha = mix(0.22, 0.92, fresnel) * vColor.a;
    if (alpha < 0.006) discard;
    gl_FragColor = vec4(color, clamp(alpha, 0.0, 1.0));
}
