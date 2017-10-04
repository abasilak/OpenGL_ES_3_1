#include "Include/version.h"
#extension GL_OES_shader_image_atomic  : enable

precision highp float;

#include "Include/inout_frag.h"
#include "Include/uniforms.h"
#include "Include/illumination.h"

layout(binding = 0, r32ui) uniform highp    coherent  uimage2D     uniform_image_counter;
layout(binding = 1, r32f ) uniform highp    writeonly image2DArray uniform_image_peel_depth;
layout(binding = 2, rgba8) uniform highp    writeonly image2DArray uniform_image_peel_color;

void main()
{
    vec4    color_final = compute_color();

    // A-Buffer Construction
    {
        int index = int(imageAtomicAdd	(uniform_image_counter   , ivec2(gl_FragCoord.xy), 1U));
                        imageStore		(uniform_image_peel_depth, ivec3(gl_FragCoord.xy, index), vec4(gl_FragCoord.z, 0.0f, 0.0f, 0.0f));
                        imageStore		(uniform_image_peel_color, ivec3(gl_FragCoord.xy, index), color_final                           );
    }
}