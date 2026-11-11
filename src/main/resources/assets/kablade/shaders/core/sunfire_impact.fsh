#version 150
uniform vec4 ColorModulator;
uniform float GameTime;
in vec4 vertexColor;
in vec2 texCoord0;
out vec4 fragColor;
void main() {
    float edge = abs(texCoord0.y * 2.0 - 1.0);
    float glow = 1.0 - smoothstep(0.2, 1.0, edge);
    float flame = 0.78 + 0.22 * sin(texCoord0.x * 61.0 - GameTime * 620.0 + edge * 11.0);
    float alpha = vertexColor.a * glow * flame * ColorModulator.a;
    if (alpha < 0.004) discard;
    float core = pow(glow, 6.0);
    vec3 color = mix(vec3(1.0, 0.19, 0.008), vec3(1.0, 0.91, 0.55), core);
    fragColor = vec4(color * (1.0 + core * 0.5) * ColorModulator.rgb, alpha);
}
