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
    vec2 p = texCoord0 * 2.0 - 1.0;
    float radius = length(p);
    float diamond = 1.0 - smoothstep(0.46, 1.0, abs(p.x) + abs(p.y));
    float rayX = 1.0 - smoothstep(0.035, 0.22, abs(p.x));
    float rayY = 1.0 - smoothstep(0.035, 0.22, abs(p.y));
    float star = max(rayX * (1.0 - smoothstep(0.18, 1.0, abs(p.y))),
                     rayY * (1.0 - smoothstep(0.18, 1.0, abs(p.x))));
    float core = 1.0 - smoothstep(0.0, 0.22, radius);
    float grain = hash12(floor(texCoord0 * 23.0)
            + vec2(floor(GameTime * 13.0), floor(GameTime * 17.0)));
    float particleMask = texture(Sampler0,
            fract(texCoord0 + vec2(GameTime * 0.09, -GameTime * 0.07))).r;
    float flicker = 0.72 + 0.28 * sin(GameTime * 239.0
            + texCoord0.x * 19.0 + grain * 2.7);
    float dissolve = smoothstep(0.12, 0.67,
            grain * 0.42 + particleMask * 0.40 + diamond * 0.34);
    float alpha = vertexColor.a * max(max(diamond * dissolve, star * 0.76), core)
            * flicker * ColorModulator.a;
    if (alpha < 0.003) discard;

    vec3 color = mix(vertexColor.rgb, vec3(0.85, 0.54, 1.0), grain * 0.18);
    color = mix(color, vec3(1.0), core * 0.91);
    color *= 0.90 + core * 1.48 + star * 0.38;
    fragColor = vec4(color * ColorModulator.rgb, clamp(alpha, 0.0, 1.0));
}
