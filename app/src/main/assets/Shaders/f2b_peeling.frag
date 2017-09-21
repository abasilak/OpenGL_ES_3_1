#include "version.h"

precision highp float;

#include "inout.h"
#include "uniforms.h"
#include "illumination.h"

                     uniform    ivec2     uniform_resolution;
layout (binding = 5) uniform    sampler2D uniform_textures_depth;

layout (early_fragment_tests) in;
layout(location = 1) out float out_frag_depth;

void main()
{
    vec2 coords = gl_FragCoord.xy/vec2(uniform_resolution);
    float depth = texture(uniform_textures_depth, coords).r;
	if(gl_FragCoord.z <= depth)
		discard;

	out_frag_color = compute_color();
	out_frag_depth = gl_FragCoord.z;
}