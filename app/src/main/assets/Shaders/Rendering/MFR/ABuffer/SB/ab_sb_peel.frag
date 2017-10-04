#include "Include/version.h"
#extension GL_OES_shader_image_atomic  : enable

precision highp uint;
precision highp float;

#include "Include/inout_frag.h"
#include "Include/uniforms.h"
#include "Include/illumination.h"
#include "Include/data_structs.h"

layout(binding = 1, r32ui)      uniform highp    coherent  uimage2D     uniform_image_head;
layout(binding = 4, std430)		        highp    coherent  buffer       SBUFFER     { NodeTypeSB nodes[]; };

void main()
{
    uint page_id         = imageAtomicAdd (uniform_image_head, ivec2(gl_FragCoord.xy), 1U);

    vec4 color_final     = compute_color();
    nodes[page_id].color = packUnorm4x8(color_final);
    nodes[page_id].depth = gl_FragCoord.z;
}