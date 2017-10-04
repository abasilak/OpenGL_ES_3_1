#include "Include/version.h"
#include "Include/inout_vert.h"

void main()
{
    vs_out.position_wcs_v   = vec3(uniform_m * vec4(position, 1.0f)).xyz;

    vs_out.position_lcs_v	= uniform_l * vec4(position, 1.0);

	vs_out.normal_wcs_v	    = vec3(uniform_n * vec4(normal, 0.0f)).xyz;

    vs_out.texcoord_v	    = texcoord0;

    gl_Position             = uniform_p * vec4((uniform_v * vec4(vs_out.position_wcs_v, 1.0)).xyz, 1.0f);
}