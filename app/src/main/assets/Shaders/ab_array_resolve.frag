#include "version.h"

precision highp float;

#include "heatmap.h"
#include "sort.h"

layout(binding = 0, r32ui) uniform highp   readonly uimage2D      uniform_image_counter;
layout(binding = 1, r32f ) uniform highp   readonly  image2DArray uniform_image_peel_depth;
layout(binding = 2, rgba8) uniform highp   readonly  image2DArray uniform_image_peel_color;

uint  getPixelFragCounter    ()                  {return imageLoad (uniform_image_counter   , ivec2(gl_FragCoord.xy)).r;}
float getPixelFragDepthValue (const int coord_z) {return imageLoad (uniform_image_peel_depth, ivec3(gl_FragCoord.xy, coord_z)).r;}
vec4  getPixelFragColorValue (const int coord_z) {return imageLoad (uniform_image_peel_color, ivec3(gl_FragCoord.xy, coord_z));}

layout(location = 0) out vec4 out_frag_color;

void main()
{
    int layer        = 0;
    int counterTotal = int(getPixelFragCounter());

    if(counterTotal > 0)
    {
        counterTotal = min(counterTotal,LOCAL_SIZE);

#ifdef HEATMAP_ENABLED
        out_frag_color  = vec4(getValueBetweenTwoFixedColors(float(counterTotal)/float(LOCAL_SIZE+1), GREEN, RED), 1.0f);
        return;
#endif

// 1. LOCAL STORAGE
		for(int i=0; i<counterTotal; i++)
			fragments[i] = vec2(float(i), getPixelFragDepthValue(i));

// 2. SORT
        sort(counterTotal);

// LAYER
//      int id = int(fragments[layer].r);
//      out_frag_color = vec4(getPixelFragColorValue(id));

// 3. RESOLVE
        vec4 finalColor = vec4(0.0f);
        for(int i=0; i<counterTotal; i++)
            finalColor += getPixelFragColorValue(int(fragments[i].r))*(1.0f-finalColor.a);
        out_frag_color = finalColor;
    }
    else
        discard;
}