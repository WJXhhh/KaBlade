#version 120

uniform sampler2D Sampler0;
uniform float GameTime;

varying float vertexDistance;
varying vec4 vertexColor;
varying vec2 texCoord0;
varying vec3 viewNormal;
varying vec3 viewDirection;

void main() {
    vec4 textureColor = texture2D(Sampler0, texCoord0);
    if (textureColor.a < 0.10) {
        discard;
    }

    float facing = abs(dot(normalize(viewNormal), normalize(viewDirection)));
    float rim = pow(clamp(1.0 - facing, 0.0, 1.0), 1.65);
    float pulse = 0.92 + 0.08 * sin(GameTime * 240.0 * 0.42);
    float opacity = textureColor.a * vertexColor.a
            * (0.035 + rim * 0.965) * pulse;
    if (opacity < 0.004) {
        discard;
    }

    vec3 deepRed = vec3(0.56, 0.002, 0.004);
    vec3 hotRed = vec3(1.0, 0.055, 0.018);
    vec3 color = mix(deepRed, hotRed, rim) * (1.05 + rim * 1.15);
    color *= mix(vec3(1.0), vertexColor.rgb, 0.18);

    float fog = clamp((gl_Fog.end - vertexDistance)
            / max(0.0001, gl_Fog.end - gl_Fog.start), 0.0, 1.0);
    vec4 result = vec4(color, clamp(opacity, 0.0, 0.82));
    result.rgb = mix(gl_Fog.color.rgb, result.rgb, fog);
    gl_FragColor = result;
}
