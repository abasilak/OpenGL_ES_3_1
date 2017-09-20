package grabasilak.iti.www.myapplication;

class Util {

    static final int    m_sizeofV4  = 4 * Float.BYTES;
    static final int    m_sizeofM44 = 16 * Float.BYTES;
    static final float  m_theta     = (float) Math.sin(Math.PI / 8.0f);

    static float[] normalize(final float[] vec)
    {
        float sum = 0.0f;
        for (float aVec : vec) sum += aVec * aVec;
        final float divisor = (float) Math.sqrt(sum);
        float[] a = new float[vec.length];
        for (int i = 0; i < vec.length; ++i) a[i] = vec[i]/divisor;
        return a;
    }

    static float dot(final float[] a, final float[] b)
    {
        float sum = 0.f;
        for (int i = 0; i < a.length; i++)
            sum += a[i] * b[i];
        return sum;
    }
}
