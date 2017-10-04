#include "Include/version.h"

precision highp int;
precision highp uint;
precision highp float;

#include "Include/heatmap.h"
#include "Include/sort.h"
#include "Include/data_structs.h"

layout(binding = 0, r32ui) uniform highp   readonly uimage2D      uniform_image_head;
layout(binding = 4, std430)        highp   readonly buffer LinkedLists { NodeTypeLL nodes[]; };

uint  getPixelFragHead() {return imageLoad (uniform_image_head, ivec2(gl_FragCoord.xy)).r;}

layout(location = 0) out vec4 out_frag_color;

void main()
{
    int  layer = 0;
    uint index = getPixelFragHead();
    if(index > 0u)
    {
// 1. LOCAL STORAGE
        int counter = 0;
    	while(index != 0u)
    	{
			fragments[counter] = vec2(float(index), nodes[index].depth);
			index	           = nodes[index].next;
			counter++;
        }
        counter = min(counter,LOCAL_SIZE);

// 2. SORT
        sort(counter);

// LAYER
      //  int id = int(fragments[layer].r);
      //  out_frag_color = unpackUnorm4x8(nodes[id].color);
      //  out_frag_color = vec4(fragments[layer].g);

// 3. RESOLVE
        vec4 finalColor = vec4(0.0f);
        for(int i=0; i<counter; i++)
            finalColor += unpackUnorm4x8(nodes[int(fragments[i].r)].color)*(1.0f-finalColor.a);
        out_frag_color = finalColor;
    }
    else
        discard;
}