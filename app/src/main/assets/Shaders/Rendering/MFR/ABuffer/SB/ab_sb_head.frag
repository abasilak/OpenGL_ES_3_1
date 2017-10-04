#include "Include/version.h"

precision highp uint;
precision highp float;

layout(binding = 0, r32ui) uniform highp readonly  uimage2D     uniform_image_counter;
layout(binding = 1, r32ui) uniform highp coherent  uimage2D     uniform_image_head;
layout(binding = 3, std430)	       highp coherent  buffer       final_address  { uint final; };

void main()
{
    uint counter     = imageLoad (uniform_image_counter, ivec2(gl_FragCoord.xy)).r;
    if(counter > 0u)
    {
        uint page_id = atomicAdd (final, counter);
                       imageStore(uniform_image_head   , ivec2(gl_FragCoord.xy), uvec4(page_id, 0U, 0U, 0U) );
    }
}