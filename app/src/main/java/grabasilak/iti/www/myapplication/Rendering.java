package grabasilak.iti.www.myapplication;

import java.util.ArrayList;

public abstract class Rendering
{
    protected String    m_name;
    protected int       m_total_passes;
    public    int []    m_texture_color = new int[1];

    public Rendering(String name)
    {
        m_name          = name;
        m_total_passes  = 1;
    }

    abstract boolean     createFBO(Viewport viewport);
    abstract void        draw     (RenderingSettings rendering_settings, TextManager text_manager, ArrayList<Mesh> meshes, Light light, Camera camera, int ubo_matrices);
    abstract int         getTextureDepth();
}
