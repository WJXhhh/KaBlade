#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float GameTime;

in vec4 vertexColor;
in vec2 texCoord0;
out vec4 fragColor;

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * vec3(0.1031, 0.1030, 0.0973));
    p3 += dot(p3, p3.yzx + 32.17);
    return fract((p3.x + p3.y) * p3.z);
}

void main() {
    vec2 p = texCoord0 * 2.0 - 1.0;
    float radius = length(p);
    float ribbon = pow(max(0.0, 1.0 - abs(p.y)), 1.18);
    float radial = 1.0 - smoothstep(0.04, 1.0, radius);
    // Every composite primitive is backed by a quad. Clamp the material to a soft
    // circular envelope so rotating impact/aura billboards cannot expose that quad.
    float circularEnvelope = 1.0 - smoothstep(0.68, 1.0, radius);
    float mask = texture(Sampler0, vec2(fract(texCoord0.x * 0.73 + GameTime * 0.17),
                                         clamp(texCoord0.y, 0.0, 1.0))).r;
    float grain = hash12(floor(texCoord0 * vec2(37.0, 13.0))
            + vec2(floor(GameTime * 9.0), 0.0));
    float silhouette = max(ribbon * (0.54 + mask * 0.33), radial * 0.72)
            * circularEnvelope;
    float erode = smoothstep(0.16, 0.62,
            grain * 0.36 + mask * 0.36 + silhouette * 0.42);
    float pulse = 0.91 + 0.09 * sin(GameTime * 173.0 + radius * 17.0);
    float alpha = vertexColor.a * silhouette * erode * pulse * ColorModulator.a;
    if (alpha < 0.003) discard;

    float luma = dot(vertexColor.rgb, vec3(0.2126, 0.7152, 0.0722));
    vec3 shadowViolet = vec3(0.075, 0.008, 0.14);
    vec3 edgeViolet = vec3(0.28, 0.045, 0.46);
    vec3 color = mix(shadowViolet, edgeViolet, clamp(luma * 1.35 + mask * 0.22, 0.0, 1.0));
    color += vertexColor.rgb * 0.16;
    fragColor = vec4(color * ColorModulator.rgb, clamp(alpha, 0.0, 1.0));
}
