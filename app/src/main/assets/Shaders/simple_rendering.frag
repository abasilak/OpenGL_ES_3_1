#version 310 es

precision mediump float;

uniform vec4 uniform_diffuse_color;

out vec4 out_frag_color;

void main()
{
    out_frag_color = uniform_diffuse_color;
}