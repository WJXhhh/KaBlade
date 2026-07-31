#version 150

in vec3 Position;
in vec2 UV0;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform int PassMode;

out vec2 texCoord0;
out vec3 viewNormal;
out vec3 viewDirection;

void main() {
    vec3 normal = normalize(Normal);
    float shellWidth = PassMode == 1 ? 0.065 : 0.0;
    vec4 viewPosition = ModelViewMat * vec4(Position + normal * shellWidth, 1.0);
    gl_Position = ProjMat * viewPosition;
    texCoord0 = UV0;
    viewNormal = normalize(mat3(transpose(inverse(ModelViewMat))) * normal);
    viewDirection = normalize(-viewPosition.xyz);
}
