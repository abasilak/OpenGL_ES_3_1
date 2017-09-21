#include "version.h"
#extension GL_OES_shader_image_atomic  : enable

precision highp float;

#include "inout.h"
#include "uniforms.h"
#include "illumination.h"
#include "data_structs.h"

layout(binding = 0, r32ui)      uniform highp    coherent  uimage2D     uniform_image_counter;
layout(binding = 1, r32ui)      uniform highp    coherent  uimage2D     uniform_image_head;
layout(binding = 2, offset = 0)	uniform atomic_uint next_address;
layout(binding = 3, std430)		        highp    coherent  buffer       LinkedLists   { NodeTypeLL nodes[]; };

void main()
{
    vec4 color_final = compute_color();

    // A-Buffer Construction
    {
        uint page_id = atomicCounterIncrement(next_address) + 1U;

        nodes[page_id].color = color_final;
        nodes[page_id].depth = gl_FragCoord.z;
        nodes[page_id].next  = imageAtomicExchange(uniform_image_head, ivec2(gl_FragCoord.xy), page_id);

        imageAtomicAdd(uniform_image_counter, ivec2(gl_FragCoord.xy), 1U);

        discard;
    }
}