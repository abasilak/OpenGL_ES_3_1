package grabasilak.iti.www.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES31;
import android.opengl.GLUtils;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glTexParameteri;

public class Texture
{
    int     m_id;
    boolean m_loaded;
    String  m_filename;

    public Texture()
    {
        m_id = 0;
        m_loaded = false;
    }

    public void load(Context context, String name)
    {
        m_filename = name;

        InputStream istr;
        Bitmap bmp = null;
        try
        {
            istr = context.getAssets().open(m_filename);
            bmp = BitmapFactory.decodeStream(istr);
        }
        catch (IOException e)
        {
            Log.d("LOADING FILE", "FILE LOADED UNSUCCESSFULLY !");
        }

        int[] tex_id = new int[1];
        glGenTextures(1, tex_id, 0);
        glBindTexture(GL_TEXTURE_2D, tex_id[0]);
        {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GLES31.GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GLES31.GL_LINEAR);
            GLUtils.texImage2D(GL_TEXTURE_2D, 0, bmp, 0);
            glBindTexture(GL_TEXTURE_2D, 0);
        }
        assert bmp != null;
        bmp.recycle();

        m_id = tex_id[0];
        m_loaded = true;
    }
}
