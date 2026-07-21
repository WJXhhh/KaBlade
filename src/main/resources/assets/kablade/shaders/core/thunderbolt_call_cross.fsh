#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float GameTime;

in vec4 vertexColor;
in vec2 texCoord0;
out vec4 fragColor;

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * vec3(0.1031, 0.1030, 0.0973));
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float noise2(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash12(i), hash12(i + vec2(1.0, 0.0)), f.x),
               mix(hash12(i + vec2(0.0, 1.0)), hash12(i + vec2(1.0)), f.x), f.y);
}

void main() {
    float across = abs(texCoord0.y * 2.0 - 1.0);
    float body = pow(max(0.0, 1.0 - across), 1.34);
    float hotCore = 1.0 - smoothstep(0.018, 0.135, across);
    float serration = noise2(vec2(texCoord0.x * 39.0 - GameTime * 23.0,
                                  texCoord0.y * 10.0));
    float turbulence = noise2(vec2(texCoord0.x * 11.0 + GameTime * 8.0,
                                   texCoord0.y * 4.0));
    float gradient = texture(Sampler0, vec2(fract(texCoord0.x * 0.84 - GameTime * 0.56),
                                             clamp(texCoord0.y, 0.0, 1.0))).r;
    float written = smoothstep(0.002, 0.048, texCoord0.x)
            * (1.0 - smoothstep(0.925, 0.998, texCoord0.x));
    float tornEdge = smoothstep(0.24, 0.76, serration + body * 0.43);
    float movingNotch = smoothstep(0.10, 0.55,
            noise2(vec2(texCoord0.x * 67.0 - GameTime * 47.0, across * 13.0)));
    float alpha = vertexColor.a * (body * tornEdge * (0.70 + movingNotch * 0.32)
            + hotCore) * (0.64 + gradient * 0.60) * written * ColorModulator.a;
    if (alpha < 0.003) discard;

    vec3 deepViolet = vec3(0.36, 0.055, 0.68);
    vec3 plasmaViolet = vec3(0.78, 0.34, 1.0);
    vec3 color = mix(deepViolet, vertexColor.rgb, 0.58 + turbulence * 0.22);
    color = mix(color, plasmaViolet, body * 0.28);
    color = mix(color, vec3(1.0, 0.97, 1.0), hotCore * 0.94);
    color *= 0.96 + hotCore * 1.92 + movingNotch * 0.18;
    fragColor = vec4(max(color, vec3(0.0)) * ColorModulator.rgb,
                     clamp(alpha, 0.0, 1.0));
}
