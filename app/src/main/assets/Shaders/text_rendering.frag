#version 310 es

precision mediump float;

uniform sampler2D   font_texture;

in VS_OUT
{
    vec4 color_v;
	vec2 texcoord_v;
} fs_in;

layout(location = 0) out vec4 out_frag_color;

void main(void)
{
	vec4 color     =  texture( font_texture, fs_in.texcoord_v  ) * fs_in.color_v;
	out_frag_color =  vec4(color.rgb * fs_in.color_v.a, color.a);
}