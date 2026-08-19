#version 120

uniform float uTime;
uniform float uStrength;
uniform int uPass;

varying vec4 vColor;
varying vec3 vPosition;

void main() {
    if (uPass == 0) {
        // Pass 0: 奇点事件视界 (Core) - 纯黑吸光核心 (深度写入)
        gl_FragColor = vec4(0.003, 0.001, 0.006, 1.0);
    } else if (uPass == 2) {
        // Pass 2: 能量泛光与高能光环 (Glow) - 保持 1.20 调校色彩并适度增强辉光
        float intensity = mix(1.0, 1.22, clamp(uStrength, 0.0, 1.0));
        vec3 col = vColor.rgb * intensity;
        
        float lum = dot(col, vec3(0.299, 0.587, 0.114));
        if (lum > 0.85) {
            col += vec3(0.08, 0.04, 0.06);
        }
        gl_FragColor = vec4(col, vColor.a);
    } else {
        // Pass 1 (Dark) 及安全回退: 严格使用 1.20 精确顶点色与 Alpha
        gl_FragColor = vColor;
    }
}
