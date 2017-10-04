//#define HEATMAP_ENABLED

#ifdef HEATMAP_ENABLED
const vec3 RED      = vec3(1,0,0);
const vec3 GREEN    = vec3(0,1,0);
const vec3 BLUE     = vec3(0,0,1);

vec3 getHeatMap(float value, float minF, float maxF)
	{
        vec3 h_color;

        value = (value-minF)/(maxF-minF);
        value = clamp(value,0.f,1.f);

        int NUM_COLORS = 26;
        // column wise
        mat4 color = mat4(
                0.f,0.f,1.f,0.f,
                0.f,1.f,0.f,0.f,
                1.f,1.f,0.f,0.f,
                1.f,0.f,0.f,0.f
                );

        int     idx1;
        int     idx2;
        float   fractBetween = 0.f;

        value *= float(NUM_COLORS)-1.f;
        idx1   = int(floor(value));
        idx2   = idx1+1;
        fractBetween = value - float(idx1);

        h_color.r = (color[idx2][0] - color[idx1][0])*fractBetween + color[idx1][0];
        h_color.g = (color[idx2][1] - color[idx1][1])*fractBetween + color[idx1][1];
        h_color.b = (color[idx2][2] - color[idx1][2])*fractBetween + color[idx1][2];

        return h_color;
	}

vec3 getValueBetweenTwoFixedColors(float val, vec3 A, vec3 B)
{
    vec3 h_color;

    h_color.r = (B.r - A.r) * val + A.r;      // Evaluated as -255*value + 255.
    h_color.g = (B.g - A.g) * val + A.g;      // Evaluates as 0.
    h_color.b = (B.b - A.b) * val + A.b;      // Evaluates as 255*value + 0.

    return h_color;
}
#endif