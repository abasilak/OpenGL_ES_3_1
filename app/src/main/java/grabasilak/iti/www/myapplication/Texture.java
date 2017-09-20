package grabasilak.iti.www.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLUtils;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.IntBuffer;

import static android.opengl.GLES20.GL_LINEAR;
import static android.opengl.GLES20.GL_RGBA;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES20.GL_UNSIGNED_BYTE;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glTexImage2D;
import static android.opengl.GLES20.glTexParameteri;

class Texture
{
            int     m_id;
            boolean m_loaded;
    private String  m_filename;

    Texture()
    {
        m_id = 0;
        m_loaded = false;
    }

    void load(Context context, String name)
    {
        m_filename = name;

        InputStream istr;
        Bitmap      bmp;
        IntBuffer   texBuffer;

        int[] tex_id = new int[1];
        glGenTextures(1, tex_id, 0);

        try
        {
            istr = context.getAssets().open(m_filename);

            String [] Blocks = m_filename.split("\\."); //split by '.'

            glBindTexture(GL_TEXTURE_2D, tex_id[0]);

            if(Blocks[1].equals("tga"))
            {
                byte [] buffer = new byte[istr.available()];
                istr.read(buffer);
                istr.close();

                int [] pixels   = TGAReader.read(buffer, TGAReader.ABGR);
                texBuffer       = IntBuffer.wrap(pixels);

                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, TGAReader.getWidth(buffer), TGAReader.getHeight(buffer), 0, GL_RGBA, GL_UNSIGNED_BYTE, texBuffer);
            }
            else
            {
                bmp = BitmapFactory.decodeStream(istr);
                GLUtils.texImage2D(GL_TEXTURE_2D, 0, bmp, 0);
                assert bmp != null;
                bmp.recycle();
            }

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            //glBindTexture(GL_TEXTURE_2D, 0);

        }
        catch (IOException e)
        {
            Log.d("LOADING FILE", "FILE LOADED UNSUCCESSFULLY !");
        }

        m_id = tex_id[0];
        m_loaded = true;
    }
}
