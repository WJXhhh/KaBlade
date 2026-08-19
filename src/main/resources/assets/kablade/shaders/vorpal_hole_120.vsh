#version 120

varying vec4 vColor;
varying vec3 vPosition;

void main() {
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
    vColor = gl_Color;
    vPosition = gl_Vertex.xyz;
}
