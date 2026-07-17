#version 120

uniform vec4 ColorModulator;
uniform float GameTime;

varying vec4 vertexColor;
varying vec2 texCoord0;

void main() {
    if (texCoord0.x < -0.5) {
        vec2 point = vec2((texCoord0.x + 1.5) * 2.0, texCoord0.y * 2.0 - 1.0);
        float distanceToCenter = length(point);
        float halo = 1.0 - smoothstep(0.08, 1.0, distanceToCenter);
        float core = 1.0 - smoothstep(0.0, 0.27, distanceToCenter);
        float horizontalRay = (1.0 - smoothstep(0.05, 0.23, abs(point.y)))
                            * (1.0 - smoothstep(0.25, 1.0, abs(point.x)));
        float verticalRay = (1.0 - smoothstep(0.05, 0.23, abs(point.x)))
                          * (1.0 - smoothstep(0.25, 1.0, abs(point.y)));
        float star = max(halo, max(horizontalRay, verticalRay));
        float opacity = vertexColor.a * clamp(star * 0.78 + core, 0.0, 1.0)
                      * ColorModulator.a;
        if (opacity < 0.004) {
            discard;
        }
        vec3 sparkColor = mix(vertexColor.rgb, vec3(1.0, 0.985, 0.78), core * 0.92);
        gl_FragColor = vec4(sparkColor * ColorModulator.rgb * (0.92 + core * 1.45), opacity);
        return;
    }

    float across = abs(texCoord0.y * 2.0 - 1.0);
    float softBand = 1.0 - smoothstep(0.18, 1.0, across);
    float whiteCore = 1.0 - smoothstep(0.0, 0.16, across);
    float wave = 0.5 + 0.5 * cos(texCoord0.x * 18.8495559 - GameTime * 125.0);
    float travelingLight = pow(wave, 22.0) * (1.0 - smoothstep(0.12, 0.68, across));

    vec3 warm = mix(vertexColor.rgb, vec3(1.0, 0.975, 0.84),
                    clamp(whiteCore * 0.82 + travelingLight, 0.0, 1.0));
    float opacity = vertexColor.a * (softBand * 0.44 + whiteCore * 0.78 + travelingLight * 1.35);
    opacity = clamp(opacity, 0.0, 1.0) * ColorModulator.a;
    if (opacity < 0.004) {
        discard;
    }

    float energy = 0.78 + whiteCore * 0.72 + travelingLight * 1.25;
    gl_FragColor = vec4(warm * ColorModulator.rgb * energy, opacity);
}
