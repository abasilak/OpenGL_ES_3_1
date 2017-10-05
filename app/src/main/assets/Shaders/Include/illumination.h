#include "Include/shadow_mapping.h"
#include "Include/phong_shading.h"

/*
subroutine(color_t)
vec4 color_white()
{
    return  vec4(1.0f);
}
*/

//subroutine(color_t)
vec4 compute_color(const vec3 position_wcs_v, const vec4 position_lcs_v, const vec3 normal_wcs_v, const vec4 diffuse_color , const vec4 specular_color, const vec3 emission_color)
{
	//[SPOT][OR][DIRECTIONAL][LIGHT]
	bool	is_spot_light		= (uniform_light_position_wcs.w == 1.0f) ? true : false;
	vec3	light_direction_wcs = (is_spot_light) ? uniform_light_direction_wcs.xyz : -uniform_light_position_wcs.xyz;

	vec3	vertex_to_light_wcs = uniform_light_position_wcs.xyz - position_wcs_v;
	float	dist_to_light		= length	(vertex_to_light_wcs);
			vertex_to_light_wcs	= normalize	(vertex_to_light_wcs);
	vec3	normal_wcs			= normalize	(normal_wcs_v);

    // BACK-FACE SHADING
	if(!gl_FrontFacing) normal_wcs = -normal_wcs;

	// [SPOTLIGHT]
	float	attenuation_factor	= (is_spot_light) ? lightGetAttenuation(dist_to_light)                      : 1.0f;
	float	spot_angle_factor	= (is_spot_light) ? lightGetSpot(-vertex_to_light_wcs, light_direction_wcs) : 1.0f;

	// [DIFFUSE]
	float	diffuse_angle_factor= (!is_spot_light || spot_angle_factor > 0.0f) ?
			lightGetDiffuse(normal_wcs, vertex_to_light_wcs)		 : 0.0f;

	// [SPECULAR]
	float	specular_shininess	= (diffuse_angle_factor > 0.0f) ?
			lightGetSpecular(position_wcs_v, normal_wcs, normalize(light_direction_wcs), specular_color.a) : 0.0f;

	// [SHADOW MAPPING]
#ifdef SHADOW_MAPPING
	float   cast_shadows		= uniform_light_direction_wcs.a;
	float	shadow_factor		= (cast_shadows == 1.0f) ? ((diffuse_angle_factor > 0.0f) ? lightGetShadow(diffuse_angle_factor, position_lcs_v) : 0.0f) : 1.0f;
#else
	float	shadow_factor		= 1.0f;
#endif

	// [FINAL FACTORS]
	float	diffuse_factor		= diffuse_angle_factor	* spot_angle_factor * shadow_factor;
	float	specular_factor		= specular_shininess	* shadow_factor;

	// [FINAL COLORS]
	vec3	ambient_color_final  = uniform_light_ambient_color  * diffuse_color.rgb;
	vec3	diffuse_color_final  = uniform_light_diffuse_color  * diffuse_color.rgb  * diffuse_factor;
	vec3	specular_color_final = uniform_light_specular_color * specular_color.rgb * specular_factor;
	vec3	emission_color_final = emission_color;

	vec3	lighting_color_final = vec3(ambient_color_final + diffuse_color_final + specular_color_final)*attenuation_factor + emission_color_final;

    return  vec4(lighting_color_final, diffuse_color.a);
}