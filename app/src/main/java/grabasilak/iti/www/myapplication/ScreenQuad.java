package grabasilak.iti.www.myapplication;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import static android.opengl.GLES20.GL_ARRAY_BUFFER;
import static android.opengl.GLES20.GL_BACK;
import static android.opengl.GLES20.GL_COLOR_ATTACHMENT0;
import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_NEAREST;
import static android.opengl.GLES20.GL_STATIC_DRAW;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TRIANGLES;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindBuffer;
import static android.opengl.GLES20.glBindFramebuffer;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glBufferData;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGenBuffers;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform1f;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUniform2i;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLES30.GL_DRAW_FRAMEBUFFER;
import static android.opengl.GLES30.GL_READ_FRAMEBUFFER;
import static android.opengl.GLES30.glBindVertexArray;
import static android.opengl.GLES30.glBlitFramebuffer;
import static android.opengl.GLES30.glDrawBuffers;
import static android.opengl.GLES30.glGenVertexArrays;
import static android.opengl.GLES30.glReadBuffer;

class ScreenQuad {

    private int []        m_vao                                 = new int[1];
    private Viewport      m_viewport;
    private int		      m_window_percentage;

    private int			  m_id;

    private ArrayList<Shader>              m_shaders            = new ArrayList<>();
    private ArrayList<ArrayList<Float>>    m_float_ids          = new ArrayList<>();
    private ArrayList<ArrayList<String>>   m_float_strings      = new ArrayList<>();
    private ArrayList<ArrayList<Integer>>  m_textures_ids       = new ArrayList<>();
    private ArrayList<ArrayList<String>>   m_textures_strings   = new ArrayList<>();

    ScreenQuad(int window_percentage)
    {
        m_id = 0;
        m_window_percentage = window_percentage;

        initVAO();
    }

    private void initVAO()
    {
	    final float m_vertices_data[] = // Vertex attributes for a quad that fills the entire screen in Normalized Device Coordinates.
        {
            // Positions of one BIG Triangle
            -1.0f, 6.0f,
            -1.0f, -1.0f,
            6.0f, -1.0f,

            //-1.0f, 1.0f,
            //1.0f, -1.0f,
            //1.0f, 1.0f
        };

        FloatBuffer m_vertices_buffer;
        m_vertices_buffer = ByteBuffer.allocateDirect ( m_vertices_data.length * Float.BYTES ).order ( ByteOrder.nativeOrder() ).asFloatBuffer();
        m_vertices_buffer.put (m_vertices_data);
        m_vertices_buffer.position ( 0 );

        int [] vbo = new int[1];
        glGenBuffers(1, vbo, 0);
        glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
        {
            m_vertices_buffer.position(0);
            glBufferData(GL_ARRAY_BUFFER, m_vertices_data.length * Float.BYTES, m_vertices_buffer, GL_STATIC_DRAW);
        }
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        // Setup screen VAO
        glGenVertexArrays ( 1, m_vao, 0 );
        glBindVertexArray(m_vao[0]);
        {
            glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
            glEnableVertexAttribArray(0);
            glVertexAttribPointer(0, 2, GL_FLOAT, false, 2 * Float.BYTES, 0);
        }
        glBindVertexArray(0);
    }

    void    drawBlit(Rendering rendering_method)
    {
        m_viewport.setViewport();

        glBindFramebuffer(GL_READ_FRAMEBUFFER, rendering_method.getFBO());
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);

        glReadBuffer(GL_COLOR_ATTACHMENT0);
        glDrawBuffers(1, new int[]{GL_BACK}, 0);

        glBlitFramebuffer(  0, 0, rendering_method.getViewport().m_width, rendering_method.getViewport().m_height,
                            0, 0, m_viewport.m_width, m_viewport.m_height,
                            GL_COLOR_BUFFER_BIT, GL_NEAREST);
    }

    void    draw()
    {
        m_viewport.setViewport();

        glUseProgram(m_shaders.get(m_id).getProgram());
        {
            if(!m_textures_ids.isEmpty())
            for (int i = 0; i < m_textures_ids.get(m_id).size(); i++)
                glUniform1i(glGetUniformLocation(m_shaders.get(m_id).getProgram(), m_textures_strings.get(m_id).get(i)), i);

            if(!m_float_ids.isEmpty())
            for (int i = 0; i < m_float_ids.get(m_id).size(); i++)
                glUniform1f(glGetUniformLocation(m_shaders.get(m_id).getProgram(), m_float_strings.get(m_id).get(i)), m_float_ids.get(m_id).get(i));

            glUniform2i(glGetUniformLocation(m_shaders.get(m_id).getProgram(), "uniform_viewport_resolution"), m_viewport.m_width, m_viewport.m_height);
            glUniform1i(glGetUniformLocation(m_shaders.get(m_id).getProgram(), "uniform_window_percentage"), m_window_percentage);
            glUniform2i(glGetUniformLocation(m_shaders.get(m_id).getProgram(), "uniform_viewport_left_corner"), m_viewport.m_left_corner_x, m_viewport.m_left_corner_y);

            if(!m_textures_ids.isEmpty())
            for (int i = 0; i < m_textures_ids.get(m_id).size(); i++)
            {
                glActiveTexture(GL_TEXTURE0 + i);
                glBindTexture(GL_TEXTURE_2D, m_textures_ids.get(m_id).get(i));
            }

            glBindVertexArray(m_vao[0]);
            {
                glDrawArrays(GL_TRIANGLES, 0, 3);
            }
            glBindVertexArray(0);

            if(!m_textures_ids.isEmpty())
            for (int i = 0; i < m_textures_ids.get(m_id).size(); i++)
            {
                glActiveTexture(GL_TEXTURE0 + i);
                glBindTexture(GL_TEXTURE_2D, 0);
            }
        }
        glUseProgram(0);
    }

    void    setViewport (int width, int height)
    {
        int left_corner_x  = width - width / m_window_percentage;
        int left_corner_y  = height - height / m_window_percentage;
        int right_corner_x = width / m_window_percentage;
        int right_corner_y = height / m_window_percentage;

        m_viewport = new Viewport(left_corner_x, left_corner_y, right_corner_x, right_corner_y);
    }

    void	addShader   (Shader		shader) { m_shaders.add(shader); }

    void	setTextureList(ArrayList<Integer> tex_ids)
    {
        m_textures_ids.clear();
        m_textures_ids.add(tex_ids);
    }

    void	addUniformTextures(ArrayList<String> tex_strings)
    {
        m_textures_strings.add(tex_strings);
    }

    void    addUniformFloats(ArrayList<Float> float_ids, ArrayList<String> float_strings)
    {
        m_float_ids.add(float_ids);
        m_float_strings.add(float_strings);
    }
}
