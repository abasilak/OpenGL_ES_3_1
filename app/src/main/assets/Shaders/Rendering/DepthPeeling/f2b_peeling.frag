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

	out_frag_color = compute_color();
}