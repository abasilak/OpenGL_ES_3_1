package grabasilak.iti.www.myapplication;

import android.content.res.Resources;
import android.opengl.GLES20;
import android.util.Log;

import static android.opengl.GLES20.GL_FRAMEBUFFER_COMPLETE;
import static android.opengl.GLES20.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT;
import static android.opengl.GLES20.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS;
import static android.opengl.GLES20.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT;
import static android.opengl.GLES20.GL_FRAMEBUFFER_UNSUPPORTED;
import static android.opengl.GLES20.glCheckFramebufferStatus;
import static android.opengl.GLES31.GL_NO_ERROR;
import static android.opengl.GLES31.glGetError;

public class RenderingSettings {

    public       FPS        m_fps;
    public       Viewport   m_viewport;
    public final float      m_depth;
    public final float []   m_background_color;

    // private constructor
    public RenderingSettings ()
    {
        m_fps                   = new FPS();
        m_viewport              = new Viewport(0,0, 1080, 1920);

        m_depth                 = 1.0f;

        m_background_color      = new float [4];
        m_background_color[0]   = 0.0f;
        m_background_color[1]   = 0.0f;
        m_background_color[2]   = 0.0f;
        m_background_color[3]   = 1.0f;
    }

    public static void checkGlError(String glOperation) {

        int error;
        while ((error = glGetError()) != GL_NO_ERROR)
        {
            Log.e(Resources.getSystem().getString(R.string.APP_TITLE), glOperation + ": glError " + error);
            throw new RuntimeException(glOperation + ": glError " + error);
        }
    }

    public static void checkFramebufferStatus() {
        int status = glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
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
