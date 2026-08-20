#version 120

varying vec4 vColor;
varying vec2 vUv;

void main() {
    vColor = gl_Color;
    vUv = gl_MultiTexCoord0.xy;
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
}
