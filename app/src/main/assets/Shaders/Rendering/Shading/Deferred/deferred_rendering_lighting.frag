#include "Include/version.h"

precision highp float;

                    uniform  ivec2     uniform_viewport_resolution;
                    uniform  vec3      uniform_camera_position_wcs;

layout(binding = 0) uniform	 sampler2D uniform_textures_position_wcs;
layout(binding = 1) uniform	 sampler2D uniform_textures_position_lcs;
layout(binding = 2) uniform	 sampler2D uniform_textures_normal;
layout(binding = 3) uniform	usampler2D uniform_textures_colors;
layout(binding = 4) uniform	 sampler2D uniform_textures_shadow_map;

layout (binding = 2, std140) uniform Light
{
	vec4		uniform_light_position_wcs;	    //.w == spot or directional
	vec4		uniform_light_direction_wcs;    //.w == cast shadows.

	vec3		uniform_light_ambient_color;
	vec3		uniform_light_diffuse_color;
	vec3		uniform_light_specular_color;

	vec4		uniform_light_attenuation_cutoff;
};

#include "Include/illumination.h"

layout(location = 0) out vec4 out_frag_color;

void main()
{
    vec2    coords              = gl_FragCoord.xy/vec2(uniform_viewport_resolution);

	// retrieve data from gbuffer
	uvec3	colors		        = texture(uniform_textures_colors, coords).rgb;
	vec4    diffuse_color       = unpackUnorm4x8(colors.r);
	//if(diffuse_color.a == 0.f)
	  //  discard;

	vec4	specular_color		= unpackUnorm4x8(colors.g);
	vec3	emission_color		= unpackUnorm4x8(colors.b).rgb;

    vec3	position_wcs_v		= texture(uniform_textures_position_wcs	 , coords).rgb;
	vec4	position_lcs_v		= texture(uniform_textures_position_lcs	 , coords);
    vec3	normal_wcs_v		= texture(uniform_textures_normal		 , coords).rgb;

	out_frag_color = compute_color(
        position_wcs_v, position_lcs_v, normal_wcs_v,
	    diffuse_color , specular_color, emission_color	);
}