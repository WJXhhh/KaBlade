#version 120

varying vec4 stageColor;
varying vec3 stagePosition;

void main() {
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
    stageColor = gl_Color;
    stagePosition = gl_Vertex.xyz;
}
