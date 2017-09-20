#define SHADOW_MAPPING
#define SHADOW_NUM_SAMPLES_EARLY	4
#define SHADOW_NUM_SAMPLES			16
#define SHADOW_DEPTH_BIAS			0.00075f
#define SHADOW_CONSTANT_BIAS

#define SHADOW_PCF
#define SHADOW_POISSON_SAMPLING
#define SHADOW_STRATIFIED_SAMPLING
#define SHADOW_EARLY_BAILING

#ifdef SHADOW_POISSON_SAMPLING
vec2 poissonDisk[16] = vec2[]
(
   vec2( -0.94201624, -0.39906216 ),
   vec2( 0.94558609, -0.76890725 ),
   vec2( -0.094184101, -0.92938870 ),
   vec2( 0.34495938, 0.29387760 ),
   vec2( -0.91588581, 0.45771432 ),
   vec2( -0.81544232, -0.87912464 ),
   vec2( -0.38277543, 0.27676845 ),
   vec2( 0.97484398, 0.75648379 ),
   vec2( 0.44323325, -0.97511554 ),
   vec2( 0.53742981, -0.47373420 ),
   vec2( -0.26496911, -0.41893023 ),
   vec2( 0.79197514, 0.19090188 ),
   vec2( -0.24188840, 0.99706507 ),
   vec2( -0.81409955, 0.91437590 ),
   vec2( 0.19984126, 0.78641367 ),
   vec2( 0.14383161, -0.14100790 )
);
#else
vec2 kernelDisk[16] =
vec2[]
(
  vec2(  0 ,  0 ),
  vec2(  0 ,  1 ),
  vec2(  0 , -1 ),
  vec2(  1 ,  0 ),
  vec2( -1 ,  0 ),
  vec2(  1 ,  1 ),
  vec2( -1 ,  1 ),
  vec2(  1 , -1 ),
  vec2( -1 , -1 ),
  vec2(  0 ,  0.5 ),
  vec2(  0 , -0.5 ),
  vec2( 0.5,  0 ),
  vec2(-0.5,  0 ),
  vec2( 0.5,  0.5 ),
  vec2(-0.5,  0.5 ),
  vec2( 0.5, -0.5 )
);
#endif

// Returns a random number based on a vec3 and an int.
#ifdef SHADOW_STRATIFIED_SAMPLING
float random(vec3 seed, int i)
{
	vec4	seed4 = vec4(seed, i);
	float	dot_product = dot(seed4, vec4(12.9898,78.233,45.164,94.673));
	return	fract(sin(dot_product) * 43758.5453);
}
#endif

#ifdef SHADOW_MAPPING
float is_point_in_shadow(vec4 position, float bias)
{
	return (position.z > texture(uniform_textures.shadow_map, position.xy).r + bias) ? 0.0f : 1.0f;
}

#ifdef SHADOW_PCF
float is_point_in_shadow_pcf(vec4 position, float bias)
{
	int   total_samples = 0;
	float isInShadow	= 0.0f;
	float shadow_map_step = 1.0/float(textureSize(uniform_textures.shadow_map, 0));

	for (int i=0; i<SHADOW_NUM_SAMPLES; i++)
	{
#ifdef SHADOW_STRATIFIED_SAMPLING
		int index = int(16.0*random(gl_FragCoord.xyy, i)) % 16;
#else
		int index = i;
#endif

#ifdef SHADOW_POISSON_SAMPLING
		float shadow_map_z = texture(uniform_textures.shadow_map, position.xy + poissonDisk[index]*shadow_map_step).r;
#else
		float shadow_map_z = texture(uniform_textures.shadow_map, position.xy + kernelDisk [index]*shadow_map_step).r;
#endif
		if (position.z < shadow_map_z + bias)
			isInShadow += 1.0f;

		total_samples++;
#ifdef SHADOW_EARLY_BAILING
		if(i == SHADOW_NUM_SAMPLES_EARLY-1 && (isInShadow == 0.0f || isInShadow == float(SHADOW_NUM_SAMPLES_EARLY)))
			break;
#endif
	}
	isInShadow /= float(total_samples);

	return isInShadow;
}
#endif

float lightGetShadow(const float diffuse_angle_factor)
{
	float	shadow_factor		= 0.0f;
	vec4	position_lcs		= fs_in.position_lcs_v / fs_in.position_lcs_v.w;
			position_lcs.xyz	= (position_lcs.xyz + vec3(1)) * vec3(0.5f);

	// Light Frustum Culling
	if ((clamp(position_lcs.xy, vec2(0,0), vec2(1,1)) - position_lcs.xy) == vec2(0,0))
	{
#ifdef SHADOW_CONSTANT_BIAS
		float	bias = SHADOW_DEPTH_BIAS;
#else
		float	bias = SHADOW_DEPTH_BIAS*tan(acos(diffuse_angle_factor));
				bias = clamp(bias, 0, 0.001f);
#endif

#ifdef SHADOW_PCF
		shadow_factor = is_point_in_shadow_pcf	(position_lcs, bias);
#else
		shadow_factor = is_point_in_shadow		(position_lcs, bias);
#endif
	}
	return shadow_factor;
}
#endif