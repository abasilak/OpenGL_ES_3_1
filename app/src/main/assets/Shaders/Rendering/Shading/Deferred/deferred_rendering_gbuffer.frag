#include "Include/version.h"

precision highp float;

// IN
in VS_OUT
{
	vec3 position_wcs_v;	// the position
	vec4 position_lcs_v;	// the position in light space
	vec3 normal_wcs_v;		// the normal
	vec2 texcoord_v;		// the texture coordinates
} fs_in;

layout(binding = 0) uniform	sampler2D uniform_textures_diffuse;
layout(binding = 1) uniform	sampler2D uniform_textures_normal;
layout(binding = 2) uniform	sampler2D uniform_textures_specular;
layout(binding = 3) uniform	sampler2D uniform_textures_emission;

layout (binding = 1, std140) uniform Material
{
	ivec4		uniform_material_has_tex;		// diffuse, normal, specular, emission
    vec4		uniform_material_diffuse_color; // .a = opacity
	vec4		uniform_material_specular_color;// .a = gloss
	vec3		uniform_material_emission_color;
};

layout(location = 0) out  vec3 out_frag_position_wcs;
layout(location = 1) out  vec4 out_frag_position_lcs;
layout(location = 2) out  vec3 out_frag_normal;
layout(location = 3) out uvec4 out_frag_colors;

layout (early_fragment_tests) in;

void main()
{
    // [0][POSITION][WCS]
    out_frag_position_wcs = fs_in.position_wcs_v;

    // [1][POSITION][LCS]
    out_frag_position_lcs = fs_in.position_lcs_v;

    // [2][NORMAL]
    out_frag_normal	      = normalize(fs_in.normal_wcs_v);

    // [3][DIFFUSE]
	vec4 diffuse_tex = vec4(1.0f);
	if (uniform_material_has_tex.x > 0)
	{
		diffuse_tex  = texture(uniform_textures_diffuse, fs_in.texcoord_v.xy);
		if (diffuse_tex.a < 1.0f)
			discard;
	}
	uint diffuse_color = packUnorm4x8(vec4(uniform_material_diffuse_color.rgb  * diffuse_tex.rgb , uniform_material_diffuse_color.a));

    // [4][SPECULAR]
	vec4 specular_tex = vec4(1.0f);
	if (uniform_material_has_tex.z > 0)
		specular_tex  = texture(uniform_textures_specular, fs_in.texcoord_v.xy);
    uint specular_color = packUnorm4x8(vec4(uniform_material_specular_color.rgb * specular_tex.rgb, uniform_material_specular_color.a));

    // [5][EMISSION]
	vec3 emission_tex = vec3(1.0f);
	if (uniform_material_has_tex.w > 0)
		emission_tex  = texture(uniform_textures_emission, fs_in.texcoord_v.xy).rgb;
    uint emission_color = packUnorm4x8(vec4(uniform_material_emission_color.rgb * emission_tex.rgb, 1.0f));

    out_frag_colors = uvec4(diffuse_color, specular_color, emission_color, 1u);
}