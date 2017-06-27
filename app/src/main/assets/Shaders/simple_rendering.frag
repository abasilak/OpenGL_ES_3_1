#version 310 es

precision mediump float;

uniform vec4 diffuse_color;

out vec4 frag_color;

void main()
{
    frag_color = diffuse_color;
}