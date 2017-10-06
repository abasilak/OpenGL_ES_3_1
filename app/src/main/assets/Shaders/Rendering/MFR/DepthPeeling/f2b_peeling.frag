#include "Include/version.h"

precision highp float;

#include "Include/inout_frag.h"
#include "Include/uniforms.h"
#include "Include/illumination.h"

                     uniform    ivec2     uniform_resolution;
layout (binding = 5) uniform    sampler2D uniform_textures_depth;

void main()
{
    vec2 coords = gl_FragCoord.xy/vec2(uniform_resolution);
    float depth = texture(uniform_textures_depth, coords).r;
	if(gl_FragCoord.z <= depth)
		discard;

	vec4 diffuse_tex = vec4(1.0f);
	if (uniform_material_has_tex.x > 0)
	{
		diffuse_tex  = texture(uniform_textures_diffuse, fs_in.texcoord_v.xy);
		if (diffuse_tex.a < 1.0f)
			discard;
	}

	vec4 specular_tex = vec4(1.0f);
	if (uniform_material_has_tex.z > 0)
		specular_tex  = texture(uniform_textures_specular, fs_in.texcoord_v.xy);

	vec4 emission_tex = vec4(1.0f);
	if (uniform_material_has_tex.w > 0)
		emission_tex  = texture(uniform_textures_emission, fs_in.texcoord_v.xy);

	// [FINAL COLORS]
	vec4	diffuse_color	= vec4(uniform_material_diffuse_color.rgb  * diffuse_tex.rgb , uniform_material_diffuse_color.a );
	vec4	specular_color	= vec4(uniform_material_specular_color.rgb * specular_tex.rgb, uniform_material_specular_color.a);
	vec3	emission_color	=      uniform_material_emission_color.rgb * emission_tex.rgb;

	out_frag_color = compute_color(
                            fs_in.position_wcs_v,
                            fs_in.position_lcs_v,
                            fs_in.normal_wcs_v,
                     	    diffuse_color,
                     	    specular_color,
                     	    emission_color
                     	);
}