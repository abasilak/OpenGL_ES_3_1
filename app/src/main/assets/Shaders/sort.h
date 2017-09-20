#define LOCAL_SIZE          10
#define INSERTION_VS_SHELL  16

vec2  fragments [LOCAL_SIZE];

void sort_shell(const int num)
{
	int inc = num >> 1;
	while (inc > 0)
	{
		for (int i = inc; i < num; ++i)
		{
			vec2 tmp = fragments[i];

			int j = i;
			while (j >= inc && fragments[j - inc].g > tmp.g)
			{
				fragments[j] = fragments[j - inc];
				j -= inc;
			}
			fragments[j] = tmp;
		}
		inc = int(float(inc) / 2.2f + 0.5f);
	}
}

void sort_insert(const int num)
{
    for (int j = 1; j < num; ++j)
    {
        vec2 key = fragments[j];
        int i = j - 1;

        while (i >= 0 && fragments[i].g > key.g)
        {
            fragments[i+1] = fragments[i];
            --i;
        }
        fragments[i+1] = key;
    }
}

void sort(const int num)
{
    if(num <= INSERTION_VS_SHELL)
        sort_insert(num);
    else
        sort_shell(num);
}