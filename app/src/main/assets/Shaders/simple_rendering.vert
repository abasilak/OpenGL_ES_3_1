#version 310 es

uniform mat4 uMVPMatrix;

layout(location = 0) in vec4 position;

void main()
{
    gl_Position = uMVPMatrix * position;
}