#include "Include/version.h"
#extension GL_OES_shader_image_atomic  : enable

precision highp uint;
precision highp float;

#include "Include/inout_frag.h"
#include "Include/uniforms.h"
#include "Include/illumination.h"
#include "Include/data_structs.h"

layout(binding = 0, r32ui)      uniform highp    coherent  uimage2D     uniform_image_head;
layout(binding = 4, std430)		        highp    coherent  buffer       LinkedLists     { NodeTypeLL nodes[]; };
layout(binding = 3, offset = 0)	uniform atomic_uint next_address;

void main()
{
    uint page_id = atomicCounterIncrement(next_address) + 1u;

    if(page_id < uint(nodes.length()))
    {
        vec4 diffuse_tex = vec4(1.0f);
        if (uniform_material_has_tex.x > 0)
        {
            diffuse_tex  = texture(uniform_textures_diffuse, fs_in.texcoord_v.xy);
            if (diffuse_tex.a < 1.0f)
                return;
        }

        vec4 specular_tex = vec4(1.0f);
        if (uniform_material_has_tex.z > 0)
            specular_tex  = texture(uniform_textures_specular, fs_in.texcoord_v.xy);

        vec4 emission_tex = vec4(1.0f);
        if (uniform_material_has_tex.w > 0)
            emission_tex  = texture(uniform_textures_emission, fs_in.texcoord_v.xy);

        // [FINAL COLORS]
        vec4	diffuse_color	= vec4(uniform_material_diffuse_color.rgb  * diffuse_tex.rgb , uniform_material_diffuse_color.a );
        vec4	specular_color	= vec4(uniform_material_specular_color.rgb * specular_tex.rgb, uniform_material_specular_color.a);
        vec3	emission_color	=      uniform_material_emission_color.rgb * emission_tex.rgb;

        vec4    color_final = compute_color(
                                    fs_in.position_wcs_v,
                                    fs_in.position_lcs_v,
                                    fs_in.normal_wcs_v,
                              	    diffuse_color,
                              	    specular_color,
                              	    emission_color
                              	);

        // A-Buffer Construction
        {
            uint prev_id         = imageAtomicExchange(uniform_image_head, ivec2(gl_FragCoord.xy), page_id);
            nodes[page_id].color = packUnorm4x8(color_final);
            nodes[page_id].depth = gl_FragCoord.z;
            nodes[page_id].next  = prev_id;
        }
    }
}