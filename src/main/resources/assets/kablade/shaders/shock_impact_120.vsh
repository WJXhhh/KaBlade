#version 120

varying vec4 vColor;
varying vec3 vLocal;

void main() {
    vColor = gl_Color;
    vLocal = gl_Vertex.xyz;
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
}
