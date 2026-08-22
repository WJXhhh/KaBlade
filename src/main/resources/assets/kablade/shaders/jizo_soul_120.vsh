#version 120

varying float vertexDistance;
varying vec4 vertexColor;
varying vec2 texCoord0;
varying vec3 viewNormal;
varying vec3 viewDirection;

void main() {
    // 法线外扩的薄壳对应 1.20 辉光 pass，不改变本体的实际深度外壳。
    vec3 normal = normalize(gl_Normal);
    vec3 expandedPosition = gl_Vertex.xyz + normal * 0.055;
    vec4 viewPosition = gl_ModelViewMatrix * vec4(expandedPosition, 1.0);
    gl_Position = gl_ProjectionMatrix * viewPosition;

    vertexDistance = length(viewPosition.xyz);
    vertexColor = gl_Color;
    texCoord0 = gl_MultiTexCoord0.xy;
    viewNormal = normalize(gl_NormalMatrix * normal);
    viewDirection = normalize(-viewPosition.xyz);
    gl_FogFragCoord = vertexDistance;
}
