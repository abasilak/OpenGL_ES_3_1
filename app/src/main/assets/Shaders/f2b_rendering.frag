#include "version.h"

precision mediump float;

#define POINT_LIGHT_LINEAR_ATT
//#define BLINN_PHONG_SHADING

// IN
in VS_OUT
{
	vec3 position_wcs_v;	// the position
	vec4 position_lcs_v;	// the position in light space
	vec3 normal_wcs_v;		// the normal
	vec2 texcoord_v;		// the texture coordinates
} fs_in;

// UNIFORM
struct Camera
{
	vec3		position_wcs;
};

struct Textures
{
	sampler2D	diffuse;
	sampler2D	normal;
	sampler2D	specular;
	sampler2D	emission;
	sampler2D	shadow_map;
	sampler2D	depth;
};

uniform ivec2           uniform_resolution;

uniform Camera			uniform_camera;
uniform Textures		uniform_textures;

layout (std140) uniform Material
{
	ivec4		uniform_material_has_tex;		// diffuse, normal, specular, emission
    vec4		uniform_material_diffuse_color; // .a = opacity
	vec4		uniform_material_specular_color;// .a = gloss
	vec3		uniform_material_emission_color;
};

layout (std140) uniform Light
{
	vec4		uniform_light_position_wcs;	    //.w == spot or directional
	vec4		uniform_light_direction_wcs;    //.w == cast shadows.

	vec3		uniform_light_ambient_color;
	vec3		uniform_light_diffuse_color;
	vec3		uniform_light_specular_color;

	vec4		uniform_light_attenuation_cutoff;
};

// OUT
layout(location = 0) out vec4 out_frag_color;

#include "shadow_map.h"

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
	vec3	vertex_to_camera_wcs = normalize(uniform_camera.position_wcs - fs_in.position_wcs_v);

#ifdef BLINN_PHONG_SHADING
    float	specular_shininess = uniform_material_specular_color.a * 2;
	vec3	reflect_wcs = normalize(light_direction_wcs + vertex_to_camera_wcs);
#else
    float	specular_shininess = uniform_material_specular_color.a;
	vec3	reflect_wcs	= reflect(-light_direction_wcs, normal_wcs);
#endif
    return	pow(max(0.0f, dot(vertex_to_camera_wcs, reflect_wcs)), specular_shininess);
}

float		uniform_z_near = 1.;
float		uniform_z_far = 100.;

float LinearizeDepth(float depth)
{
    float z = depth * 2.0 - 1.0; // Back to NDC
    return (2.0 * uniform_z_near * uniform_z_far) / (uniform_z_far + uniform_z_near - z * (uniform_z_far - uniform_z_near));
}

void main()
{
    vec2 coords = gl_FragCoord.xy/vec2(uniform_resolution);
    float depth = texture(uniform_textures.depth, coords).r;
	if(gl_FragCoord.z <= depth)
	{
		out_frag_color  = vec4(vec3(LinearizeDepth(gl_FragCoord.z)/uniform_z_far), 1.0f);
		//discard;
    }

	// [TEXTURES]
	vec4 diffuse_tex = vec4(1.0f);
	if (uniform_material_has_tex.x > 0)
	{
		diffuse_tex  = texture(uniform_textures.diffuse, fs_in.texcoord_v.xy);
		//if (diffuse_tex.a < 1.0f || uniform_material_diffuse_color.a < 1.0f)
		//	discard;
	}

	vec4 specular_tex = vec4(1.0f);
	if (uniform_material_has_tex.z > 0)
		specular_tex  = texture(uniform_textures.specular, fs_in.texcoord_v.xy);

	vec4 emission_tex = vec4(1.0f);
	if (uniform_material_has_tex.w > 0)
		emission_tex  = texture(uniform_textures.emission, fs_in.texcoord_v.xy);

	//[SPOT][OR][DIRECTIONAL][LIGHT]
	bool	is_spot_light		= (uniform_light_position_wcs.w == 1.0f) ? true : false;
	vec3	light_direction_wcs = (is_spot_light) ? uniform_light_direction_wcs.xyz : -uniform_light_position_wcs.xyz;

	vec3	vertex_to_light_wcs = uniform_light_position_wcs.xyz - fs_in.position_wcs_v;
	float	dist_to_light		= length	(vertex_to_light_wcs);
			vertex_to_light_wcs	= normalize	(vertex_to_light_wcs);
	vec3	normal_wcs			= normalize	(fs_in.normal_wcs_v);

	// [SPOTLIGHT]
	float	attenuation_factor	= (is_spot_light) ? lightGetAttenuation(dist_to_light) : 1.0f;
	float	spot_angle_factor	= (is_spot_light) ? lightGetSpot(-vertex_to_light_wcs, light_direction_wcs) : 1.0f;

	// [DIFFUSE]
	float	diffuse_angle_factor= (!is_spot_light || spot_angle_factor > 0.0f) ?
			lightGetDiffuse(normal_wcs, vertex_to_light_wcs)		 : 0.0f;

	// [SPECULAR]
	float	specular_shininess	= (diffuse_angle_factor > 0.0f) ?
			lightGetSpecular(normal_wcs, light_direction_wcs)		 : 0.0f;

	// [SHADOW MAPPING]
#ifdef SHADOW_MAPPING
	float   cast_shadows		= uniform_light_direction_wcs.a;
	float	shadow_factor		= (cast_shadows == 1.0f) ? ((diffuse_angle_factor > 0.0f) ? lightGetShadow(diffuse_angle_factor) : 0.0f) : 1.0f;
#else
	float	shadow_factor		= 1.0f;
#endif

	// [FINAL FACTORS]
	float	diffuse_factor		= diffuse_angle_factor	* spot_angle_factor * shadow_factor;
	float	specular_factor		= specular_shininess	* shadow_factor;

	// [FINAL COLORS]
	vec3	diffuse_color		= vec3(uniform_material_diffuse_color)  * diffuse_tex.rgb;
	vec3	specular_color		= vec3(uniform_material_specular_color) * specular_tex.rgb;
	vec3	emission_color		= vec3(uniform_material_emission_color) * emission_tex.rgb;

	// [FINAL COLORS]
	vec3	ambient_color_final  = uniform_light_ambient_color  * diffuse_color;
	vec3	diffuse_color_final  = uniform_light_diffuse_color  * diffuse_color  * diffuse_factor;
	vec3	specular_color_final = uniform_light_specular_color * specular_color * specular_factor;
	vec3	emission_color_final = emission_color;

	vec3	lighting_color_final = vec3(ambient_color_final + diffuse_color_final)*attenuation_factor + emission_color_final;

	out_frag_color               = vec4(lighting_color_final, 1.0f);
}