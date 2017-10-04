#include "Include/version.h"

precision mediump float;

uniform   vec3	  uniform_color;

layout(location = 0) out vec4 out_frag_color;

void main(void)
{
	out_frag_color = vec4(uniform_color,1);
}