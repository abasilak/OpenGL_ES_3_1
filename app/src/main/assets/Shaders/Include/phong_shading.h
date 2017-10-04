#define POINT_LIGHT_LINEAR_ATT
//#define BLINN_PHONG_SHADING

#ifndef PI
#define PI 3.1415936
#endif // PI

float lightGetAttenuation(const float dist_to_light)
{
#ifdef POINT_LIGHT_LINEAR_ATT
	// linear attenuation
	float	attenuation_factor = 1.0f / (uniform_light_attenuation_cutoff.x + uniform_light_attenuation_cutoff.y * dist_to_light);
#else
	// quadratic attenuation
	float	attenuation_factor = 1.0f / (uniform_light_attenuation_cutoff.x + uniform_light_attenuation_cutoff.y * dist_to_light + uniform_light_attenuation_cutoff.z * dist_to_light * dist_to_light);
#endif

	return	attenuation_factor;
}

float lightGetSpot(const vec3 vertex_to_light_wcs, const vec3 light_direction_wcs)
{
	float	spotangle_angle		= max(0.0, dot(vertex_to_light_wcs	, uniform_light_direction_wcs.xyz));

	return (spotangle_angle > uniform_light_attenuation_cutoff.w) ?
		1.0 - (1.0 - spotangle_angle) * 1.0/(1.0 - uniform_light_attenuation_cutoff.w) :
		0.0f;
}

float lightGetDiffuse(const vec3 normal_wcs, const vec3 vertex_to_light_wcs)
{
	return max(0.0, dot(normal_wcs, vertex_to_light_wcs));
}

float lightGetSpecular(const vec3 normal_wcs, const vec3 light_direction_wcs)
{
	vec3	vertex_to_camera_wcs = normalize(uniform_camera_position_wcs - fs_in.position_wcs_v);

#ifdef BLINN_PHONG_SHADING
    float   shininess   = ( 8.0f + uniform_material_specular_color.a ) / ( 8.0f * PI );
	vec3	halfway_wcs = normalize(-light_direction_wcs + vertex_to_camera_wcs);
	return	pow(max(0.01f, dot(normal_wcs, halfway_wcs)), shininess);
#else
    float   shininess   = ( 2.0f + uniform_material_specular_color.a ) / ( 2.0f * PI );
	vec3	reflect_wcs	= reflect(light_direction_wcs, normal_wcs);
	return	pow(max(0.01f, dot(vertex_to_camera_wcs, reflect_wcs)), shininess);
#endif
    }