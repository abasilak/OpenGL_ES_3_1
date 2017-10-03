package grabasilak.iti.www.myapplication;

import java.util.ArrayList;

import static android.opengl.GLES30.glGenQueries;

abstract class Rendering
{
    String    m_name;
    int       m_struct_size;
    int       m_total_passes;
    int []    m_texture_color   = new int[1];
    int []    m_occlusion_query = new int[1];
    int []    m_occlusion_query_result = new int[1];

    Viewport  m_viewport;

    Rendering(String name, Viewport viewport)
    {
        m_name          = name;
        m_viewport      = viewport;
        m_total_passes  = 1;

        glGenQueries(1, m_occlusion_query, 0);
    }

    abstract boolean     createFBO();
    abstract void        draw     (RenderingSettings rendering_settings, TextManager text_manager, ArrayList<Mesh> meshes, ArrayList<Light> lights, Camera camera, int ubo_matrices);

    abstract int         getFBO();
    abstract Viewport    getViewport();
    abstract int         getTextureDepth();
}
