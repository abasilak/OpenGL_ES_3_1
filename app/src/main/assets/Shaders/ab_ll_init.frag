#include "version.h"

precision highp uint;
precision highp float;

layout(binding = 0, r32ui) uniform highp writeonly uimage2D     uniform_image_counter;
layout(binding = 1, r32ui) uniform highp writeonly uimage2D     uniform_image_head;

void main()
{
    imageStore (uniform_image_counter, ivec2(gl_FragCoord.xy), uvec4(0u));
    imageStore (uniform_image_head   , ivec2(gl_FragCoord.xy), uvec4(0u));
}