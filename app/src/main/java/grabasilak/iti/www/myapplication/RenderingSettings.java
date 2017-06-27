package grabasilak.iti.www.myapplication;

import android.content.res.Resources;
import android.opengl.GLES31;
import android.util.Log;

/**
 * Created by Andreas on 16-Jun-17.
 */

public class RenderingSettings {

    public       FPS        m_fps;
    public       Viewport   m_viewport;
    public final float      m_depth;
    public final float []   m_background_color;

    // private constructor
    public RenderingSettings ()
    {
        m_fps                   = new FPS();
        m_viewport              = new Viewport(0,0,1024,768);
        m_depth                 = 1.0f;

        m_background_color      = new float [4];
        m_background_color[0]   = 1.0f;
        m_background_color[1]   = 1.0f;
        m_background_color[2]   = 1.0f;
        m_background_color[3]   = 1.0f;
    }

    public void checkGlError(String glOperation) {

        int error;
        while ((error = GLES31.glGetError()) != GLES31.GL_NO_ERROR)
        {
            Log.e(Resources.getSystem().getString(R.string.APP_TITLE), glOperation + ": glError " + error);
            throw new RuntimeException(glOperation + ": glError " + error);
        }
    }

}
