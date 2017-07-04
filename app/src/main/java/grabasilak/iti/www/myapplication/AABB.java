package grabasilak.iti.www.myapplication;

import android.renderscript.Float3;

class AABB {

    float  m_radius;
    Float3  m_min;
    Float3  m_max;
    Float3  m_center;

    AABB ()
    {
        m_radius = 0.0f;
        m_center = new Float3(0.0f, 0.0f, 0.0f);
        m_min    = new Float3(Float.MAX_VALUE,Float.MAX_VALUE,Float.MAX_VALUE);
        m_max    = new Float3(-Float.MAX_VALUE,-Float.MAX_VALUE,-Float.MAX_VALUE);
    }

    void computeCenter()
    {
        m_center.x = (m_min.x + m_max.x)*0.5f;
        m_center.y = (m_min.y + m_max.y)*0.5f;
        m_center.z = (m_min.z + m_max.z)*0.5f;
    }

    void computeRadius()
    {
        m_radius = Math.max(Math.max(m_max.x-m_center.x,m_max.y-m_center.y),m_max.z-m_center.z);
    }
}
