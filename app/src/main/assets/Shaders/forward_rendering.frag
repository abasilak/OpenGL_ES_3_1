#include "version.h"

precision highp float;

#include "inout.h"
#include "uniforms.h"
#include "illumination.h"

void main()
{
	out_frag_color = compute_color();
}