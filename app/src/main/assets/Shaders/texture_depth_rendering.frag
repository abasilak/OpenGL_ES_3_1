#include "version.h"

precision mediump float;

in VS_OUT
{
	vec2 texcoord_v;
} fs_in;

uniform float		uniform_z_near; 
uniform float		uniform_z_far; 
uniform sampler2D	uniform_texture_depth;

layout(location = 0) out vec4 out_color;
 
float LinearizeDepth(float depth) 
{
    float z = depth * 2.0 - 1.0; // Back to NDC 
    return (2.0 * uniform_z_near * uniform_z_far) / (uniform_z_far + uniform_z_near - z * (uniform_z_far - uniform_z_near));	
}

void main()
{ 
	float depth = texture(uniform_texture_depth, fs_in.texcoord_v).r;
    out_color   = vec4(vec3(LinearizeDepth(depth)/uniform_z_far), 1.0f);
}