package grabasilak.iti.www.myapplication;

import static android.opengl.GLES31.glViewport;

/**
 * Created by Andreas on 26-Jun-17.
 */

public class Viewport {

    private int	    m_width;
    private int	    m_height;
    private int	    m_left_corner_x;
    private int	    m_left_corner_y;
    private float	m_aspect_ratio;

    public Viewport(int x, int y, int w, int h)
    {
        m_left_corner_x = x;
        m_left_corner_y = y;
        m_width         = w;
        m_height        = h;
    }

    public float getAspectRatio  (){
        return m_aspect_ratio ;
    }

    public void setAspectRatio  (){
        m_aspect_ratio = (float)(m_width - m_left_corner_x) / (float)(m_height - m_left_corner_y);
    }

    public void setViewport     () {
        glViewport(m_left_corner_x, m_left_corner_y, m_width, m_height);
    }

    public void setViewport     (int w, int h) {
        m_width  = w;
        m_height = h;

        glViewport(m_left_corner_x, m_left_corner_y, m_width, m_height);
    }
}
