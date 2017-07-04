#version 310 es

precision mediump float;

in VS_OUT
{
	vec3 position_wcs_v;	// the position
	//vec4 position_lcs_v;	// the position in light space
	vec3 normal_wcs_v;		// the normal
	vec2 texcoord_v;		// the texture coordinates
} fs_in;

uniform vec4 uniform_diffuse_color;

layout(location = 0) out vec4 out_frag_color;

void main()
{
    out_frag_color = vec4(fs_in.normal_wcs_v*0.5f +0.5f, 1.0f);
}