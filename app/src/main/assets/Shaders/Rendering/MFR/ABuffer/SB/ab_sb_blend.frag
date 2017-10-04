#include "Include/version.h"
#extension GL_OES_shader_image_atomic  : enable

precision highp uint;

layout(binding = 0, r32ui)      uniform highp    coherent   uimage2D        uniform_image_counter;

void main(void)
{
    imageAtomicAdd (uniform_image_counter, ivec2(gl_FragCoord.xy), 1U);
}