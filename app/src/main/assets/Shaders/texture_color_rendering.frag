#version 310 es

precision mediump float;

in VS_OUT
{
	highp vec2 texcoord_v;
} fs_in;

uniform sampler2D	uniform_texture_color;

layout(location = 0) out vec4 out_color;

void main()
{
    out_color  = texture(uniform_texture_color, fs_in.texcoord_v);
}