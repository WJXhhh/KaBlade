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
    vec2 centered = texCoord0 * 2.0 - 1.0;
    float across = abs(centered.y);
    float stripHalo = 1.0 - smoothstep(0.22, 1.0, across);
    float stripCore = 1.0 - smoothstep(0.0, 0.115, across);
    float radial = 1.0 - smoothstep(0.06, 1.0, length(centered));

    float broad = noise2(vec2(texCoord0.x * 7.0 - GameTime * 28.0,
                              texCoord0.y * 3.6 + GameTime * 3.0));
    float fine = noise2(vec2(texCoord0.x * 29.0 - GameTime * 61.0,
                             texCoord0.y * 11.0 - GameTime * 7.0));
    float mask = texture(Sampler0, fract(vec2(texCoord0.x * 0.78 - GameTime * 2.1,
                                              texCoord0.y * 0.92 + GameTime * 0.37))).r;
    float packets = smoothstep(0.58, 0.91, broad * 0.62 + mask * 0.38);
    float ionization = 0.68 + broad * 0.24 + fine * 0.12 + packets * 0.36;
    float pulse = 0.86 + 0.14 * sin(GameTime * 470.0 + texCoord0.x * 15.0);
    float body = max(stripHalo, radial * 0.62);
    float core = max(stripCore, radial * radial * 0.45);
    float alpha = vertexColor.a * (body * 0.58 + core) * ionization * pulse
            * ColorModulator.a;
    if (alpha < 0.003) discard;

    vec3 violet = mix(vertexColor.rgb, vec3(0.76, 0.42, 1.0), broad * 0.24);
    vec3 color = mix(violet, vec3(1.0, 0.985, 1.0), core * 0.91);
    color *= 0.88 + core * 1.65 + packets * 0.30;
    fragColor = vec4(max(color, vec3(0.0)) * ColorModulator.rgb,
                     clamp(alpha, 0.0, 1.0));
}
