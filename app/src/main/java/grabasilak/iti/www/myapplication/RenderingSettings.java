package grabasilak.iti.www.myapplication;

import android.content.res.Resources;
import android.util.Log;

import static android.opengl.GLES20.GL_FRAMEBUFFER;
import static android.opengl.GLES20.GL_FRAMEBUFFER_COMPLETE;
import static android.opengl.GLES20.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT;
import static android.opengl.GLES20.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS;
import static android.opengl.GLES20.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT;
import static android.opengl.GLES20.GL_FRAMEBUFFER_UNSUPPORTED;
import static android.opengl.GLES20.glCheckFramebufferStatus;
import static android.opengl.GLES31.GL_NO_ERROR;
import static android.opengl.GLES31.glGetError;

class RenderingSettings {

    FPS             m_fps;
    Viewport        m_viewport;
    final float     m_depth;
    final float []  m_background_color;
    final int       m_max_layers;
    final int       m_max_memory;

    // private constructor
    RenderingSettings (int width, int height)
    {
        m_fps                   = new FPS();
        m_viewport              = new Viewport(0,0, width, height);

        m_depth                 = 1.0f;

        m_background_color      = new float [4];
        m_background_color[0]   = 1.0f;
        m_background_color[1]   = 1.0f;
        m_background_color[2]   = 1.0f;
        m_background_color[3]   = 1.0f;

        m_max_layers            = 10;
        m_max_memory            = 50;
    }

    static void checkGlError(String glOperation) {

        int error;
        while ((error = glGetError()) != GL_NO_ERROR)
        {
            Log.e(Resources.getSystem().getString(R.string.APP_TITLE), glOperation + ": glError " + error);
            throw new RuntimeException(glOperation + ": glError " + error);
        }
    }

    static void checkFramebufferStatus() {
        int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            String msg = "";
            switch (status) {
                case GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT:
                    msg = "GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT";
                    break;
                case GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS:
                    msg = "GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS";
                    break;
                case GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT:
                    msg = "GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT";
                    break;
                case GL_FRAMEBUFFER_UNSUPPORTED:
                    msg = "GL_FRAMEBUFFER_UNSUPPORTED";
                    break;
            }
            Log.e(Resources.getSystem().getString(R.string.APP_TITLE), msg + ": glError " +  Integer.toHexString(status));
            throw new RuntimeException(msg + ": Framebuffer Status" + Integer.toHexString(status));
        }
    }
}
