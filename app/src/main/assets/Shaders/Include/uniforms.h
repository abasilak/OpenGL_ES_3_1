                    uniform vec3      uniform_camera_position_wcs;
layout(binding = 0) uniform	sampler2D uniform_textures_diffuse;
layout(binding = 1) uniform	sampler2D uniform_textures_normal;
layout(binding = 2) uniform	sampler2D uniform_textures_specular;
layout(binding = 3) uniform	sampler2D uniform_textures_emission;
layout(binding = 4) uniform	sampler2D uniform_textures_shadow_map;

layout (binding = 1, std140) uniform Material
{
	ivec4		uniform_material_has_tex;		// diffuse, normal, specular, emission
    vec4		uniform_material_diffuse_color; // .a = opacity
	vec4		uniform_material_specular_color;// .a = gloss
	vec3		uniform_material_emission_color;
};

layout (binding = 2, std140) uniform Light
{
	vec4		uniform_light_position_wcs;	    //.w == spot or directional
	vec4		uniform_light_direction_wcs;    //.w == cast shadows.

	vec3		uniform_light_ambient_color;
	vec3		uniform_light_diffuse_color;
	vec3		uniform_light_specular_color;

	vec4		uniform_light_attenuation_cutoff;
};