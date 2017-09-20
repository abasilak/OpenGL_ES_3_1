package grabasilak.iti.www.myapplication;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import static grabasilak.iti.www.myapplication.Util.m_sizeofM44;

class MeshPrimitiveGroup
{
    ArrayList<MeshPrimitive>    m_primitives;
    Material                    m_material;
    String                      m_material_name = null;
    FloatBuffer                 m_material_buffer;

    MeshPrimitiveGroup()
    {
                m_primitives    = new ArrayList<>();
    }

    void setMaterialData()
    {
        float[] material_data = new float[16];

        //material_has_tex_loaded
        material_data[0 ] = m_material.m_diffuse_tex.m_loaded  ? 1 : 0;
        material_data[1 ] = m_material.m_normal_tex.m_loaded   ? 1 : 0;
        material_data[2 ] = m_material.m_specular_tex.m_loaded ? 1 : 0;
        material_data[3 ] = m_material.m_emission_tex.m_loaded ? 1 : 0;
        //material_diffuse_opacity
        material_data[4 ] = m_material.m_diffuse[0];
        material_data[5 ] = m_material.m_diffuse[1];
        material_data[6 ] = m_material.m_diffuse[2];
        material_data[7 ] = m_material.m_opacity;
        //material_specular_gloss
        material_data[8 ] = m_material.m_specular[0];
        material_data[9 ] = m_material.m_specular[1];
        material_data[10] = m_material.m_specular[2];
        material_data[11] = m_material.m_gloss;
        //material_emission
        material_data[12] = m_material.m_emission[0];
        material_data[13] = m_material.m_emission[1];
        material_data[14] = m_material.m_emission[2];
        material_data[15] = 0.0f; // not used

        m_material_buffer = ByteBuffer.allocateDirect ( m_sizeofM44 * 4).order ( ByteOrder.nativeOrder() ).asFloatBuffer();
        m_material_buffer.put (material_data);
        m_material_buffer.position ( 0 );
    }
}
