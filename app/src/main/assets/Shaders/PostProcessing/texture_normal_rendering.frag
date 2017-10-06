#include "Include/version.h"

precision highp float;

                     uniform ivec2      uniform_viewport_resolution;
                     uniform ivec2      uniform_viewport_left_corner;
layout(location = 0) uniform sampler2D	uniform_texture_color;

layout(location = 0) out vec4 out_color;

void main()
{
    vec2 coords = ((vec2(gl_FragCoord.xy) - vec2(uniform_viewport_left_corner)))/vec2(uniform_viewport_resolution);
    vec3 normal    = texture(uniform_texture_color, coords).rgb;
         out_color = (all(equal(normal, vec3(0.0f)))) ? vec4(0.0f, 0.0f, 0.0f, 1.0f) : vec4(normal*0.5f + 0.5f, 1.0f);
}