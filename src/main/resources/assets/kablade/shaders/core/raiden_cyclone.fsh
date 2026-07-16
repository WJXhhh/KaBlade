#version 150

uniform vec4 ColorModulator;
uniform float GameTime;

in vec4 vertexColor;
in vec2 texCoord0;
out vec4 fragColor;

void main() {
    float across = abs(texCoord0.y * 2.0 - 1.0);
    float softEdge = 1.0 - smoothstep(0.48, 1.0, across);
    float core = 1.0 - smoothstep(0.0, 0.18, across);
    float taper = smoothstep(0.0, 0.10, texCoord0.x) * (1.0 - smoothstep(0.88, 1.0, texCoord0.x));
    float noise = 0.72 + 0.28 * sin(texCoord0.x * 47.0 - GameTime * 180.0 + across * 9.0);
    float opacity = vertexColor.a * softEdge * taper * noise * ColorModulator.a;
    if (opacity < 0.004) discard;
    float darkLayer = 1.0 - smoothstep(0.13, 0.28,
        max(vertexColor.r, max(vertexColor.g, vertexColor.b)));
    vec3 luminous = mix(vertexColor.rgb, vec3(0.92, 1.0, 1.0), core * 0.86)
        * (0.82 + core * 1.08);
    vec3 darkShell = vertexColor.rgb * (0.58 + noise * 0.22);
    vec3 energy = mix(luminous, darkShell, darkLayer);
    opacity *= mix(1.0, 0.86, darkLayer);
    fragColor = vec4(energy * ColorModulator.rgb, opacity);
}
