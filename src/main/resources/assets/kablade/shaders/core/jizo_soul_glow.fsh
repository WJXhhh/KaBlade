#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
uniform float GameTime;

in float vertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;
in vec3 viewNormal;
in vec3 viewDirection;

out vec4 fragColor;

void main() {
    vec4 textureColor = texture(Sampler0, texCoord0);
    if (textureColor.a < 0.10) {
        discard;
    }

    float facing = abs(dot(normalize(viewNormal), normalize(viewDirection)));
    float rim = pow(clamp(1.0 - facing, 0.0, 1.0), 1.65);
    float pulse = 0.92 + 0.08 * sin(GameTime * 240.0 * 0.42);
    float opacity = textureColor.a * vertexColor.a * ColorModulator.a
            * (0.035 + rim * 0.965) * pulse;
    if (opacity < 0.004) {
        discard;
    }

    vec3 deepRed = vec3(0.56, 0.002, 0.004);
    vec3 hotRed = vec3(1.0, 0.055, 0.018);
    vec3 color = mix(deepRed, hotRed, rim) * (1.05 + rim * 1.15);
    color *= mix(vec3(1.0), vertexColor.rgb, 0.18) * ColorModulator.rgb;
    fragColor = linear_fog(vec4(color, clamp(opacity, 0.0, 0.82)),
            vertexDistance, FogStart, FogEnd, FogColor);
}
