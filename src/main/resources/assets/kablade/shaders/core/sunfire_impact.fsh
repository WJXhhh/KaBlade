#version 150
uniform vec4 ColorModulator;
uniform float GameTime;
in vec4 vertexColor;
in vec2 texCoord0;
out vec4 fragColor;
void main() {
    float edge = abs(texCoord0.y * 2.0 - 1.0);
    float glow = 1.0 - smoothstep(0.0, 1.0, edge);
    float flame = 0.8 + 0.2 * sin(texCoord0.x * 80.0 - GameTime * 220.0 + edge * 8.0);
    float alpha = vertexColor.a * glow * flame * ColorModulator.a;
    if (alpha < 0.004) discard;
    vec3 color = mix(vec3(1.0, 0.42, 0.02), vec3(1.0, 1.0, 0.68), pow(glow, 2.0));
    fragColor = vec4(color * vertexColor.rgb * ColorModulator.rgb, alpha);
}
