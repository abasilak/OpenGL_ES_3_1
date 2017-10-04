// IN
layout (binding = 0, std140) uniform Matrices
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

// OUT
out VS_OUT
{
	vec3 position_wcs_v;	// the position
	vec4 position_lcs_v;	// the position in light space
	vec3 normal_wcs_v;		// the normal
	vec2 texcoord_v;		// the texture coordinates
} vs_out;
