#include "version.h"

precision highp uint;
precision highp float;

layout(binding = 0, r32ui) uniform highp writeonly uimage2D     uniform_image_counter;

void main()
{
    imageStore (uniform_image_counter, ivec2(gl_FragCoord.xy), uvec4(0U));
}