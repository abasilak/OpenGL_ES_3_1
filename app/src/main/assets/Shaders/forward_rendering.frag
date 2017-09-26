#include "version.h"

precision highp float;

#include "inout.h"
#include "uniforms.h"
#include "illumination.h"

layout (early_fragment_tests) in;

//subroutine vec4     color_t();
//subroutine uniform  color_t compute_color;

void main()
{
	out_frag_color = compute_color();
}