#include "Include/version.h"

precision highp int;
precision highp uint;
precision highp float;

#include "Include/heatmap.h"
#include "Include/sort.h"
#include "Include/data_structs.h"

layout(binding = 0, r32ui) uniform highp   readonly uimage2D      uniform_image_counter;
layout(binding = 1, r32ui) uniform highp   readonly uimage2D      uniform_image_head;
layout(binding = 4, std430)        highp   readonly buffer SBUFFER { NodeTypeSB nodes[]; };

uint  getPixelCounter () {return imageLoad (uniform_image_counter, ivec2(gl_FragCoord.xy)).r;}
uint  getPixelFragHead() {return imageLoad (uniform_image_head   , ivec2(gl_FragCoord.xy)).r - 1U;}

layout(location = 0) out vec4 out_frag_color;

void main()
{
    int  layer = 0;
    int counterTotal = int(getPixelCounter());
    counterTotal     = min(counterTotal,LOCAL_SIZE);

    if(counterTotal > 0)
    {
        uint index = getPixelFragHead();

// 1. LOCAL STORAGE
#ifdef HEATMAP_ENABLED
        out_frag_color  = vec4(getValueBetweenTwoFixedColors(float(counterTotal)/float(LOCAL_SIZE+1), GREEN, RED), 1.0f);
        return;
#endif

    	for(int C=0; C<counterTotal; C++)
    	{
			fragments[C] = vec2(float(index), nodes[index].depth);
			index--;
        }


// 2. SORT
        sort(counterTotal);

// LAYER
      //  int id = int(fragments[layer].r);
      //  out_frag_color = unpackUnorm4x8(nodes[id].color);
      //  out_frag_color = vec4(fragments[layer].g);

// 3. RESOLVE
        vec4 finalColor = vec4(0.0f);
        for(int i=0; i<counterTotal; i++)
            finalColor += unpackUnorm4x8(nodes[int(fragments[i].r)].color)*(1.0f-finalColor.a);
        out_frag_color = finalColor;
    }
    else
        discard;
}