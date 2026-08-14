#version 150

in vec3 Position;
in vec2 UV0;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec2 texCoord0;

void main() {
    vec4 clip = ProjMat * ModelViewMat * vec4(Position, 1.0);
    // Pull the copied surface forward in normalized depth without changing the OBJ.
    clip.z -= clip.w * 0.00035;
    gl_Position = clip;
    texCoord0 = UV0;
}
