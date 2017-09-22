#include "version.h"
#extension GL_OES_shader_image_atomic  : enable

precision highp uint;
precision highp float;

#include "inout.h"
#include "uniforms.h"
#include "illumination.h"
#include "data_structs.h"

layout(binding = 0, r32ui)      uniform highp    coherent  uimage2D     uniform_image_head;
layout(binding = 4, std430)		        highp    coherent  buffer       LinkedLists     { NodeTypeLL nodes[]; };
layout(binding = 3, offset = 0)	uniform atomic_uint next_address;

void main()
{
    uint page_id = atomicCounterIncrement(next_address) + 1u;

    if(page_id < uint(nodes.length()))
    {
        //vec4 color_final = compute_color();
        vec4 color_final = vec4(compute_color().rgb, 0.5f);

        uint prev_id = imageAtomicExchange(uniform_image_head, ivec2(gl_FragCoord.xy), page_id);

        nodes[page_id].color = packUnorm4x8(color_final);
        nodes[page_id].depth = gl_FragCoord.z;
        nodes[page_id].next  = prev_id;

        discard;
    }
}