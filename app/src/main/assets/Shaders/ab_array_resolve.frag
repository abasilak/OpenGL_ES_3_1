#include "version.h"

#define LOCAL_SIZE 5

precision highp float;

layout(binding = 0, r32ui) uniform mediump readonly uimage2D      uniform_image_counter;
layout(binding = 1, r32f ) uniform mediump readonly  image2DArray uniform_image_peel_depth;
layout(binding = 2, rgba8) uniform mediump readonly  image2DArray uniform_image_peel_color;

uint  getPixelFragCounter    ()                  {return imageLoad (uniform_image_counter   , ivec2(gl_FragCoord.xy)).r;}
float getPixelFragDepthValue (const int coord_z) {return imageLoad (uniform_image_peel_depth, ivec3(gl_FragCoord.xy, coord_z)).r;}
vec4  getPixelFragColorValue (const int coord_z) {return imageLoad (uniform_image_peel_color, ivec3(gl_FragCoord.xy, coord_z));}

layout(location = 0) out vec4 out_frag_color;

vec2  fragments [LOCAL_SIZE];

void sort_insert(const int num)
{
    for (int j = 1; j < num; ++j)
    {
        vec2 key = fragments[j];
        int i = j - 1;

        while (i >= 0 && fragments[i].g > key.g)
        {
            fragments[i+1] = fragments[i];
            --i;
        }
        fragments[i+1] = key;
    }
}

void main()
{
    int layer        = 0;
    int counterTotal = int(getPixelFragCounter());

    if(counterTotal > 0)
    {
        counterTotal = min(counterTotal,LOCAL_SIZE);
		for(int i=0; i<counterTotal; i++)
		{
			fragments[i] = vec2(i, getPixelFragDepthValue(i));
		}

        sort_insert(counterTotal);

        int id = int(fragments[layer].r);
        out_frag_color = getPixelFragColorValue(id);
    }
    else
        discard;
}