#include "Include/version.h"

precision mediump float;

uniform ivec2       uniform_viewport_resolution;
uniform sampler2D	uniform_texture_color;

layout(location = 0) out vec4 out_color;

void main()
{
    vec2 coords = gl_FragCoord.xy/vec2(uniform_viewport_resolution);
    out_color  = texture(uniform_texture_color, coords);
}