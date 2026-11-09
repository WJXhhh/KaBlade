#version 150
uniform vec4 ColorModulator;
uniform float GameTime;
in vec4 vertexColor;
in vec2 texCoord0;
out vec4 fragColor;
void main() {
    float edge = abs(texCoord0.y * 2.0 - 1.0);
    float glow = 1.0 - smoothstep(0.15, 1.0, edge);
    float flicker = 0.8 + 0.2 * sin(texCoord0.x * 95.0 - GameTime * 220.0);
    float alpha = vertexColor.a * glow * flicker * ColorModulator.a;
    if (alpha < 0.004) discard;
    vec3 core = mix(vertexColor.rgb, vec3(1.0, 1.0, 0.78), pow(glow, 4.0) * 0.65);
    fragColor = vec4(core * ColorModulator.rgb, alpha);
}
