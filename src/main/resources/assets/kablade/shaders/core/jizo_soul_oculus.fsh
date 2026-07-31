#version 150

uniform sampler2D Sampler0;
uniform float GameTime;
uniform float Opacity;
uniform int PassMode;

in vec2 texCoord0;
in vec3 viewNormal;
in vec3 viewDirection;

out vec4 fragColor;

void main() {
    vec4 texel = texture(Sampler0, texCoord0);
    if (texel.a < 0.10) {
        discard;
    }

    if (PassMode == 2) {
        fragColor = vec4(1.0);
        return;
    }

    float facing = abs(dot(normalize(viewNormal), normalize(viewDirection)));
    float rim = pow(clamp(1.0 - facing, 0.0, 1.0), 1.55);
    float pulse = 0.93 + 0.07 * sin(GameTime * 240.0 * 0.42);

    if (PassMode == 1) {
        float energy = (0.16 + rim * 1.34) * pulse * Opacity;
        vec3 red = mix(vec3(0.72, 0.004, 0.006),
                       vec3(2.35, 0.075, 0.025), rim);
        fragColor = vec4(red * energy, clamp(energy, 0.0, 1.0));
        return;
    }

    float luma = dot(texel.rgb, vec3(0.2126, 0.7152, 0.0722));
    vec3 readable = mix(texel.rgb, texel.rgb * 1.14 + vec3(luma * 0.05, 0.0, 0.0), 0.72);
    readable += vec3(0.10, 0.002, 0.002) * (0.22 + rim * 0.34);
    fragColor = vec4(readable, texel.a * Opacity);
}
