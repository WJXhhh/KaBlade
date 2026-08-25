#version 120

varying vec4 vColor;
varying vec3 vLocal;
varying vec3 vViewNormal;

void main() {
    vColor = gl_Color;
    vLocal = gl_Vertex.xyz;
    vec3 localNormal = length(gl_Vertex.xyz) > 0.0001
        ? normalize(gl_Vertex.xyz) : vec3(0.0, 1.0, 0.0);
    vViewNormal = normalize(gl_NormalMatrix * localNormal);
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
}
