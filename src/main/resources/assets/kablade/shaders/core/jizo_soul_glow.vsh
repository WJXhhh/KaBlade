#version 150

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out float vertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;
out vec3 viewNormal;
out vec3 viewDirection;

void main() {
    // A thin normal-expanded shell lets the Fresnel light extend beyond the
    // translucent body's silhouette without changing the actual model pass.
    vec3 expandedPosition = Position + normalize(Normal) * 0.055;
    vec4 viewPosition = ModelViewMat * vec4(expandedPosition, 1.0);
    gl_Position = ProjMat * viewPosition;

    vertexDistance = length(viewPosition.xyz);
    vertexColor = Color;
    texCoord0 = UV0;
    viewNormal = normalize(mat3(ModelViewMat) * Normal);
    viewDirection = normalize(-viewPosition.xyz);
}
