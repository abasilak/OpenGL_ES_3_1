#include "version.h"

layout (std140) uniform Matrices
{
    mat4 uniform_m;
    mat4 uniform_v;
	mat4 uniform_p;
	mat4 uniform_n;
	mat4 uniform_l;
};

layout(location = 0) in vec3 position;
layout(location = 1) in vec3 normal;
layout(location = 2) in vec2 texcoord0;

// attributes that are passed to the fragment shader
out VS_OUT
{
	vec3 position_wcs_v;	// the position
	vec4 position_lcs_v;	// the position in light space
	vec3 normal_wcs_v;		// the normal
	vec2 texcoord_v;		// the texture coordinates
} vs_out;

void main()
{
    vs_out.position_wcs_v   = vec3(uniform_m * vec4(position, 1.0f)).xyz;

    vs_out.position_lcs_v	= uniform_l * vec4(position, 1.0);

	vs_out.normal_wcs_v	    = vec3(uniform_n * vec4(normal, 0.0f)).xyz;

    vs_out.texcoord_v	    = texcoord0;

    gl_Position             = uniform_p * vec4((uniform_v * vec4(vs_out.position_wcs_v, 1.0)).xyz, 1.0f);
}