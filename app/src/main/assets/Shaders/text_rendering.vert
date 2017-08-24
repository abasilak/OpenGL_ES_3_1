#version 310 es

layout(location = 0) in vec3 position;
layout(location = 1) in vec4 color;
layout(location = 2) in vec2 texcoord0;

uniform mat4 uniform_mvp;

// attributes that are passed to the fragment shader
out VS_OUT
{
    vec4 color_v;		    // the texture coordinates
	vec2 texcoord_v;		// the texture coordinates
} vs_out;

void main(void)
{
    vs_out.color_v    = color;
    vs_out.texcoord_v = texcoord0;

	gl_Position = uniform_mvp * vec4(position, 1);
}