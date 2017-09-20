package grabasilak.iti.www.myapplication;

import static android.opengl.GLES31.glViewport;

public class Viewport {

    int	    m_width;
    int	    m_height;
    int	    m_left_corner_x;
    int	    m_left_corner_y;
    float	m_aspect_ratio;

    public Viewport(int x, int y, int w, int h)
    {
        m_left_corner_x = x;
        m_left_corner_y = y;
        m_width         = w;
        m_height        = h;
    }

    float getAspectRatio (){
        return m_aspect_ratio ;
    }

    void setAspectRatio  (){
        m_aspect_ratio = (float)(m_width - m_left_corner_x) / (float)(m_height - m_left_corner_y);
    }

    void setViewport     () {
        glViewport(m_left_corner_x, m_left_corner_y, m_width, m_height);
    }

    void setViewport     (int w, int h) {
        m_width  = w;
        m_height = h;

        glViewport(m_left_corner_x, m_left_corner_y, m_width, m_height);
    }
}
