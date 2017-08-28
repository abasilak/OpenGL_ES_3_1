#version 310 es

layout (location = 0) in vec4 position;

out VS_OUT
{
	vec2 texcoord_v;		// the texture coordinates
} vs_out;


void main()
{
    vs_out.texcoord_v	= position.zw;
    
	gl_Position = vec4(position.xy, 0.0f, 1.0f); 
}  