#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float GameTime;

in vec4 vertexColor;
in vec2 texCoord0;
out vec4 fragColor;

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * vec3(0.1031, 0.1030, 0.0973));
    p3 += dot(p3, p3.yzx + 31.32);
    return fract((p3.x + p3.y) * p3.z);
}

void main() {
    float across = abs(texCoord0.y * 2.0 - 1.0);
    float sheath = pow(max(0.0, 1.0 - across), 1.72);
    float core = 1.0 - smoothstep(0.035, 0.205, across);
    float cell = hash12(floor(vec2(texCoord0.x * 31.0 - GameTime * 17.0,
                                   texCoord0.y * 7.0)));
    float crawl = texture(Sampler0, fract(vec2(texCoord0.x * 0.54 - GameTime * 1.35,
                                               texCoord0.y * 1.7 + cell * 0.19))).r;
    float packetPhase = fract(texCoord0.x * 4.0 - GameTime * 11.0 + cell * 0.20);
    float packet = 1.0 - smoothstep(0.07, 0.31, abs(packetPhase - 0.5));
    float strobe = step(0.24, hash12(vec2(floor(GameTime * 94.0), cell * 17.0)));
    float flow = 0.80 + crawl * 0.17 + packet * 0.24;
    float alpha = vertexColor.a * (sheath * 0.92 + core * 0.96) * flow
            * mix(0.78, 1.0, strobe) * ColorModulator.a;
    if (alpha < 0.002) discard;

    vec3 plasma = mix(vertexColor.rgb, vec3(0.75, 0.34, 1.0), sheath * 0.18);
    vec3 color = mix(plasma, vec3(1.0, 0.985, 1.0), core * 0.94);
    color *= 0.98 + core * 1.78 + packet * 0.24;
    fragColor = vec4(color * ColorModulator.rgb, clamp(alpha, 0.0, 1.0));
}
