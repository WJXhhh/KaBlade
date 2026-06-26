#version 150

uniform vec4 ColorModulator;
uniform float GameTime;

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

float hash(float n) {
    return fract(sin(n) * 43758.5453123);
}

void main() {
    vec3 baseColor = vertexColor.rgb * ColorModulator.rgb;
    float alpha = vertexColor.a * ColorModulator.a;

    if (texCoord0.x <= 1.05) {
        vec2 p = texCoord0 * 2.0 - 1.0;
        float radius = length(p);
        if (radius > 1.0) {
            discard;
        }

        float angle = atan(p.y, p.x);
        float t = GameTime * 120.0;
        float flow = sin(t * 0.13) * 0.42 + sin(t * 0.047 + 1.8) * 0.28;
        float core = 1.0 - smoothstep(0.0, 0.27, radius);
        float eventHorizon = smoothstep(0.20, 0.35, radius) * (1.0 - smoothstep(0.43, 0.60, radius));
        float disk = 1.0 - smoothstep(0.76, 1.0, radius);
        float outer = smoothstep(0.52, 0.86, radius) * (1.0 - smoothstep(0.86, 1.0, radius));
        float smoke = smoothstep(0.34, 0.72, radius) * (1.0 - smoothstep(0.84, 1.0, radius));

        float spiralA = 0.5 + 0.5 * cos(angle * 4.0 - radius * 18.0 + t * 1.62 + flow);
        float spiralB = 0.5 + 0.5 * cos(angle * 3.0 - radius * 13.0 + t * 1.18 + 1.8 - flow * 0.66);
        float arms = max(pow(spiralA, 7.0), pow(spiralB, 8.5));
        arms *= smoothstep(0.20, 0.50, radius) * (1.0 - smoothstep(0.78, 0.98, radius));
        float darkArms = pow(0.5 + 0.5 * cos(angle * 5.0 - radius * 20.0 + t * 0.98 + 2.4 + flow * 0.42), 5.0)
                       * smoothstep(0.16, 0.42, radius)
                       * (1.0 - smoothstep(0.70, 0.96, radius));

        float emberNoise = hash(floor(angle * 26.0 + radius * 41.0));
        float redFlecks = step(0.70, emberNoise) * outer
                        * (0.45 + 0.55 * sin(t * 2.2 + angle * 7.0));
        float rimGlow = outer * (0.28 + arms * 0.72 + redFlecks * 0.62);

        float opacity = alpha * clamp(disk * 0.72 + smoke * 0.18 + arms * 0.22 + eventHorizon * 0.58 + rimGlow * 0.36,
                                      0.0, 1.0);
        if (opacity < 0.004) {
            discard;
        }

        vec3 black = vec3(0.0, 0.0, 0.0);
        vec3 deepRed = vec3(0.13, 0.0, 0.0);
        vec3 red = mix(baseColor, vec3(1.0, 0.025, 0.012), 0.70);
        vec3 hot = vec3(1.0, 0.16, 0.035);

        vec3 color = mix(black, deepRed, disk * 0.22 + smoke * 0.20);
        color = mix(color, red, arms * 0.58 + rimGlow * 0.34);
        color = mix(color, hot, eventHorizon * 0.35 + redFlecks * 0.46);
        color = mix(color, black, core * 0.97 + darkArms * 0.42);
        color *= 0.62 + arms * 1.12 + rimGlow * 0.70 + eventHorizon * 0.46 + smoke * 0.16;

        fragColor = vec4(color, clamp(opacity, 0.0, 1.0));
        return;
    }

    if (texCoord0.x < 3.0) {
        float u = clamp(texCoord0.x - 1.0, 0.0, 1.0);
        float across = abs(texCoord0.y * 2.0 - 1.0);
        float taper = smoothstep(0.0, 0.16, u) * (1.0 - smoothstep(0.76, 1.0, u));
        float core = 1.0 - smoothstep(0.0, 0.14, across);
        float edge = 1.0 - smoothstep(0.12, 1.0, across);
        float strobe = pow(0.5 + 0.5 * sin(u * 20.0 - GameTime * 140.0), 8.0);
        float opacity = alpha * taper * (edge * 0.34 + core * 0.74 + strobe * 0.28);
        if (opacity < 0.004) {
            discard;
        }
        vec3 color = mix(baseColor, vec3(1.0, 0.74, 0.66), core * 0.42 + strobe * 0.24);
        fragColor = vec4(color * (0.82 + core * 0.92 + strobe * 0.48), clamp(opacity, 0.0, 1.0));
        return;
    }

    if (texCoord0.x < 4.05) {
        float u = clamp(texCoord0.x - 3.0, 0.0, 1.0);
        float across = abs(texCoord0.y * 2.0 - 1.0);
        float head = smoothstep(0.0, 0.10, u);
        float tail = 1.0 - smoothstep(0.52, 1.0, u);
        float width = 1.0 - smoothstep(0.0, 1.0, across);
        float torn = pow(width, 1.8);
        float gaps = smoothstep(0.18, 0.74, hash(floor(u * 28.0) + floor(across * 9.0) * 11.0));
        float edgeSpark = pow(max(0.0, sin(u * 24.0 - GameTime * 180.0)), 6.0) * (1.0 - across);
        float opacity = alpha * head * tail * (torn * 0.62 + edgeSpark * 0.45) * gaps;
        if (opacity < 0.004) {
            discard;
        }
        vec3 color = mix(vec3(0.0, 0.0, 0.0), baseColor, 0.62 + edgeSpark * 0.28);
        color = mix(color, vec3(1.0, 0.10, 0.04), edgeSpark * 0.42);
        fragColor = vec4(color * (0.75 + edgeSpark * 1.15), clamp(opacity, 0.0, 1.0));
        return;
    }

    if (texCoord0.x < 6.05) {
        float u = fract(texCoord0.x - 5.0);
        float across = abs(texCoord0.y * 2.0 - 1.0);
        float middle = 1.0 - smoothstep(0.0, 0.28, across);
        float softEdge = 1.0 - smoothstep(0.10, 1.0, across);
        float segment = floor(u * 78.0);
        float rough = hash(segment * 4.71 + floor(across * 7.0) * 19.0);
        float gaps = smoothstep(0.12, 0.58, rough);
        float ripple = pow(max(0.0, sin(u * 68.0 - GameTime * 230.0)), 5.0) * (1.0 - across * 0.72);
        float opacity = alpha * gaps * (softEdge * 0.70 + middle * 1.20 + ripple * 0.72);
        if (opacity < 0.004) {
            discard;
        }

        vec3 hot = vec3(1.0, 0.18, 0.045);
        vec3 ember = mix(baseColor, hot, middle * 0.54 + ripple * 0.64);
        vec3 color = mix(vec3(0.02, 0.0, 0.0), ember, 0.74 + middle * 0.36);
        fragColor = vec4(color * (1.05 + middle * 1.05 + ripple * 1.55), clamp(opacity, 0.0, 1.0));
        return;
    }

    if (texCoord0.x < 8.05) {
        vec2 p = vec2((texCoord0.x - 7.0) * 2.0 - 1.0, texCoord0.y * 2.0 - 1.0);
        float radius = length(p);
        if (radius > 1.0) {
            discard;
        }

        float angle = atan(p.y, p.x);
        float core = 1.0 - smoothstep(0.0, 0.26, radius);
        float disk = 1.0 - smoothstep(0.16, 0.98, radius);
        float tear = pow(0.5 + 0.5 * cos(angle * 18.0 + radius * 9.0 - GameTime * 210.0), 4.0);
        float broken = smoothstep(0.24, 0.76, hash(floor(angle * 24.0) + floor(radius * 13.0) * 17.0));
        float edge = smoothstep(0.42, 0.88, radius) * (1.0 - smoothstep(0.82, 1.0, radius));
        float opacity = alpha * disk * (0.28 + core * 1.12 + tear * broken * 0.58 + edge * 0.42);
        if (opacity < 0.004) {
            discard;
        }

        vec3 hot = vec3(1.0, 0.62, 0.42);
        vec3 red = mix(baseColor, vec3(1.0, 0.04, 0.015), 0.76);
        vec3 color = mix(vec3(0.0, 0.0, 0.0), red, disk * 0.68 + tear * 0.28);
        color = mix(color, hot, core * 0.62 + tear * broken * 0.24);
        fragColor = vec4(color * (1.18 + core * 1.35 + tear * 0.80), clamp(opacity, 0.0, 1.0));
        return;
    }

    if (texCoord0.x < 10.05) {
        float u = clamp(texCoord0.x - 9.0, 0.0, 1.0);
        float across = abs(texCoord0.y * 2.0 - 1.0);
        float body = 1.0 - smoothstep(0.0, 1.0, across);
        float core = 1.0 - smoothstep(0.0, 0.42, across);
        float raggedEdge = pow(1.0 - smoothstep(0.38, 1.0, across), 0.70);
        float head = smoothstep(0.55, 1.0, u);
        float tail = smoothstep(0.0, 0.18, u);
        float chunks = hash(floor(u * 36.0) * 5.13 + floor(across * 8.0) * 17.0);
        float ripped = smoothstep(0.10, 0.64, chunks);
        float vein = pow(max(0.0, sin(u * 45.0 + across * 8.0 - GameTime * 170.0)), 5.0);
        float opacity = alpha * tail * ripped
                      * (body * 0.38 + raggedEdge * 0.60 + head * 0.48 + vein * 0.28);
        if (opacity < 0.004) {
            discard;
        }

        vec3 hot = vec3(1.0, 0.16, 0.035);
        vec3 black = vec3(0.0, 0.0, 0.0);
        vec3 red = mix(baseColor, hot, 0.62 + head * 0.24 + vein * 0.20);
        vec3 color = mix(black, red, raggedEdge * 0.70 + head * 0.28 + vein * 0.16);
        color = mix(color, black, core * (0.52 - head * 0.22));
        fragColor = vec4(color * (0.92 + head * 0.86 + vein * 0.70), clamp(opacity, 0.0, 1.0));
        return;
    }

    discard;
}
