package grabasilak.iti.www.myapplication;


public class Material {

    String  m_name;

    float   m_opacity;
    float   m_glossiness;
    float   m_refraction_index;

    float[] m_diffuse  = new float[3];
    float[] m_specular = new float[3];
    float[] m_emission = new float[3];

    Material()
    {
        m_name              = "default";
        m_opacity           = 1.f;
        m_glossiness        = 10.f;
        m_refraction_index  = 1.f;

        m_diffuse [0] = m_diffuse [1] = m_diffuse [2] = 0.0f;
        m_specular[0] = m_specular[1] = m_specular[2] = 1.f;
        m_emission[0] = m_emission[1] = m_emission[2] = 0.f;

        m_diffuse [2] = 1.0f;
    }

    Material(String name)
    {
        m_name              = name;
        m_opacity           = 1.f;
        m_glossiness        = 40.f;
        m_refraction_index  = 1.f;

        m_diffuse [0] = m_diffuse [1] = m_diffuse [2] = 0.75f;
        m_specular[0] = m_specular[1] = m_specular[2] = 1.f;
        m_emission[0] = m_emission[1] = m_emission[2] = 0.f;
    }
}
