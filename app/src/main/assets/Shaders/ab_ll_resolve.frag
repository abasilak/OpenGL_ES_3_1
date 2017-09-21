#include "version.h"

precision highp float;

#include "heatmap.h"
#include "sort.h"
#include "data_structs.h"

layout(binding = 0, r32ui) uniform highp   readonly uimage2D      uniform_image_counter;
layout(binding = 1, r32ui) uniform highp   readonly uimage2D      uniform_image_head;
layout(binding = 3, std430)        highp   readonly buffer LinkedLists { NodeTypeLL nodes[]; };

uint  getPixelFragCounter    ()                  {return imageLoad (uniform_image_counter   , ivec2(gl_FragCoord.xy)).r;}
uint  getPixelFragHead       ()                  {return imageLoad (uniform_image_head      , ivec2(gl_FragCoord.xy)).r;}

layout(location = 0) out vec4 out_frag_color;

void main()
{
    int layer        = 0;
    int counterTotal = int(getPixelFragCounter());

    if(counterTotal > 0)
    {
        counterTotal = min(counterTotal,LOCAL_SIZE);

        uint index = getPixelFragHead();

#ifdef HEATMAP_ENABLED
        out_frag_color  = vec4(getValueBetweenTwoFixedColors(float(counterTotal)/float(LOCAL_SIZE+1), GREEN, RED), 1.0f);
        out_frag_color  = vec4(getValueBetweenTwoFixedColors(float(index)/float(10000), GREEN, RED), 1.0f);
        return;
#endif

// 1. LOCAL STORAGE
    	for(int i=0; i<counterTotal && index != 0u; i++)
    	{
			fragments[i] = vec2(float(index), nodes[index].depth);
			index	     = nodes[index].next;
        }
// 2. SORT
        sort(counterTotal);

// LAYER
        int id = int(fragments[layer].r);
        out_frag_color = nodes[id].color;
        //out_frag_color = vec4(fragments[layer].g);

// 3. RESOLVE
//        vec4 finalColor = vec4(0.0f);
//        for(int i=0; i<counterTotal; i++)
//            finalColor += getPixelFragColorValue(int(fragments[i].r))*(1.0f-finalColor.a);
//        out_frag_color = finalColor;
    }
    else
        discard;
}