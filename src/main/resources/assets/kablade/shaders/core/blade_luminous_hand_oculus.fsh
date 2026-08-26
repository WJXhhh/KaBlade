#version 150

uniform sampler2D Sampler0;
uniform float ColorScale;

in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec4 texel = texture(Sampler0, texCoord0);
    if (texel.a <= 0.001) {
        discard;
    }
    fragColor = vec4(texel.rgb * ColorScale, texel.a);
}
