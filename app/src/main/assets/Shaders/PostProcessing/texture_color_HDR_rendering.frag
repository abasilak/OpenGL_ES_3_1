#include "Include/version.h"

precision highp float;

                     uniform float      uniform_gamma;
                     uniform float      uniform_exposure;
                     uniform ivec2      uniform_viewport_resolution;
                     uniform ivec2      uniform_viewport_left_corner;
layout(location = 0) uniform sampler2D	uniform_texture_color;

layout(location = 0) out vec4 out_color;

vec3 ToneMappingReinhard(vec3 color)
{
    return color / (color + vec3(1.f));
}

vec3 ToneMappingExposure(vec3 color)
{
    return vec3(1.f) - exp(-color * uniform_exposure);
}

vec3 GammaCorrection(vec3 color)
{
    return pow(color, vec3(1.0f / uniform_gamma));
}

void main()
{
    vec2 coords = ((vec2(gl_FragCoord.xy) - vec2(uniform_viewport_left_corner)))/vec2(uniform_viewport_resolution);

  	vec3 hdrColor = texture(uniform_texture_color, coords).rgb;

    vec3 ldrColor;
    //ldrColor = hdrColor;
    //ldrColor = ToneMappingReinhard(hdrColor);
	ldrColor  = ToneMappingExposure(hdrColor);

    ldrColor  = GammaCorrection(ldrColor);

    out_color = vec4(ldrColor, 1.0f);
}