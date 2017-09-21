package grabasilak.iti.www.myapplication;

import java.util.ArrayList;

abstract class Rendering
{
    String    m_name;
    int       m_total_passes;
    int []    m_texture_color   = new int[1];
//    int []    m_occlusion_query = new int[1];

    Rendering(String name)
    {
        m_name          = name;
        m_total_passes  = 1;
    }

    abstract boolean     createFBO(Viewport viewport);
    abstract void        draw     (RenderingSettings rendering_settings, TextManager text_manager, ArrayList<Mesh> meshes, ArrayList<Light> lights, Camera camera, int ubo_matrices);
    abstract int         getTextureDepth();
}
