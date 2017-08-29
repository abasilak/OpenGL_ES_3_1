package grabasilak.iti.www.myapplication;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import static android.opengl.GLES20.GL_ARRAY_BUFFER;
import static android.opengl.GLES20.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_STATIC_DRAW;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TRIANGLES;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindBuffer;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glBufferData;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGenBuffers;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform1f;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLES30.glBindVertexArray;
import static android.opengl.GLES30.glGenVertexArrays;

class ScreenQuad {

    private int []        m_vao             = new int[1];
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
                    // Positions   // TexCoords
                    -1.0f, 1.0f, 0.0f, 1.0f,
                    -1.0f, -1.0f, 0.0f, 0.0f,
                    1.0f, -1.0f, 1.0f, 0.0f,

                    -1.0f, 1.0f, 0.0f, 1.0f,
                    1.0f, -1.0f, 1.0f, 0.0f,
                    1.0f, 1.0f, 1.0f, 1.0f
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
            glVertexAttribPointer(0, 4, GL_FLOAT, false, 4 * Float.BYTES, 0);
        }
        glBindVertexArray(0);
    }

    void    draw()
    {
        m_viewport.setViewport();

        glClear(GL_DEPTH_BUFFER_BIT);

        glUseProgram(m_shaders.get(m_id).getProgram());
        {
            if(!m_textures_ids.isEmpty())
            for (int i = 0; i < m_textures_ids.get(m_id).size(); i++)
                glUniform1i(glGetUniformLocation(m_shaders.get(m_id).getProgram(), m_textures_strings.get(m_id).get(i)), i);

            if(!m_float_ids.isEmpty())
            for (int i = 0; i < m_float_ids.get(m_id).size(); i++)
                glUniform1f(glGetUniformLocation(m_shaders.get(m_id).getProgram(), m_float_strings.get(m_id).get(i)), m_float_ids.get(m_id).get(i));

            glBindVertexArray(m_vao[0]);
            {
                for (int i = 0; i < m_textures_ids.get(m_id).size(); i++)
                {
                    glActiveTexture(GL_TEXTURE0 + i);
                    glBindTexture(GL_TEXTURE_2D, m_textures_ids.get(m_id).get(i));
                }
                glDrawArrays(GL_TRIANGLES, 0, 6);
            }
            glBindVertexArray(0);
        }
        glUseProgram(0);
    }

    void    setViewport(int width, int height)
    {
        int left_corner_x  = width - width / m_window_percentage;
        int left_corner_y  = height - height / m_window_percentage;
        int right_corner_x = width / m_window_percentage;
        int right_corner_y = height / m_window_percentage;

        m_viewport = new Viewport(left_corner_x, left_corner_y, right_corner_x, right_corner_y);
    }

    void	addShader			(Shader		shader) { m_shaders.add(shader); }

    void	addTextureList(ArrayList<Integer> tex_ids, ArrayList<String> tex_strings)
    {
        m_textures_ids.add(tex_ids);
        m_textures_strings.add(tex_strings);
    }

    void    addUniformFloats(ArrayList<Float> float_ids, ArrayList<String> float_strings)
    {
        m_float_ids.add(float_ids);
        m_float_strings.add(float_strings);
    }
}
