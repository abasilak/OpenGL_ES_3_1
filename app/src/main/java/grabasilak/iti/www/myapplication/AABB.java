package grabasilak.iti.www.myapplication;

class AABB {

    float   m_radius;

    float[] m_min    = new float[3];
    float[] m_max    = new float[3];
    float[] m_center = new float[3];

    AABB ()
    {
        m_radius = 0.0f;
        m_center[0] = m_center[1] = m_center[2] =  0.0f;
        m_min   [0] = m_min   [1] = m_min   [2] =  Float.MAX_VALUE;
        m_max   [0] = m_max   [1] = m_max   [2] = -Float.MAX_VALUE;
    }

    void computeCenter()
    {
        m_center[0] = (m_min[0] + m_max[0])*0.5f;
        m_center[1] = (m_min[1] + m_max[1])*0.5f;
        m_center[2] = (m_min[2] + m_max[2])*0.5f;
    }

    void computeRadius()
    {
        m_radius = Math.max(Math.max(m_max[0]-m_center[0],m_max[1]-m_center[1]),m_max[2]-m_center[2]);
    }
}
