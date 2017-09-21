#include "version.h"

precision highp float;

#include "inout.h"
#include "uniforms.h"
#include "illumination.h"

layout (early_fragment_tests) in;

void main()
{
	out_frag_color = compute_color();
}