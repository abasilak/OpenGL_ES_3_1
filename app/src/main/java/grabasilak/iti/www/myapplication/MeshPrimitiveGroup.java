package grabasilak.iti.www.myapplication;

import java.util.ArrayList;

/**
 * Created by Andreas on 07-Sep-17.
 */

class MeshPrimitiveGroup
{
    ArrayList<Primitive> m_primitives;
    Material             m_material;
    String               m_material_name;
    float[]              m_material_data = new float[16];

    MeshPrimitiveGroup(String name)
    {
        m_primitives    = new ArrayList<>();
        m_material_name = name;
    }

    void setMaterialData()
    {
        //material_has_tex_loaded
        m_material_data[0 ] = m_material.m_diffuse_tex.m_loaded  ? 1 : 0;
        m_material_data[1 ] = m_material.m_normal_tex.m_loaded   ? 1 : 0;
        m_material_data[2 ] = m_material.m_specular_tex.m_loaded ? 1 : 0;
        m_material_data[3 ] = m_material.m_emission_tex.m_loaded ? 1 : 0;
        //material_diffuse_opacity
        m_material_data[4 ] = m_material.m_diffuse[0];
        m_material_data[5 ] = m_material.m_diffuse[1];
        m_material_data[6 ] = m_material.m_diffuse[2];
        m_material_data[7 ] = m_material.m_opacity;
        //material_specular_gloss
        m_material_data[8 ] = m_material.m_specular[0];
        m_material_data[9 ] = m_material.m_specular[1];
        m_material_data[10] = m_material.m_specular[2];
        m_material_data[11] = m_material.m_gloss;
        //material_emission
        m_material_data[12] = m_material.m_emission[0];
        m_material_data[13] = m_material.m_emission[1];
        m_material_data[14] = m_material.m_emission[2];
        m_material_data[15] = 0.0f; // not used
    }
}
