#version 310 es

uniform mat4 uniform_mvp;

layout(location = 0) in vec4 position;

void main()
{
    gl_Position = uniform_mvp * position;
}