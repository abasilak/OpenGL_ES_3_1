/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package grabasilak.iti.www.myapplication;

import android.content.Context;
import android.opengl.Matrix;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;

import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES31.GL_ARRAY_BUFFER;
import static android.opengl.GLES31.GL_DYNAMIC_DRAW;
import static android.opengl.GLES31.GL_ELEMENT_ARRAY_BUFFER;
import static android.opengl.GLES31.GL_FLOAT;
import static android.opengl.GLES31.GL_STATIC_DRAW;
import static android.opengl.GLES31.GL_TEXTURE4;
import static android.opengl.GLES31.GL_TEXTURE_2D;
import static android.opengl.GLES31.GL_TRIANGLES;
import static android.opengl.GLES31.GL_UNIFORM_BUFFER;
import static android.opengl.GLES31.GL_UNSIGNED_SHORT;
import static android.opengl.GLES31.glActiveTexture;
import static android.opengl.GLES31.glBindBuffer;
import static android.opengl.GLES31.glBindBufferBase;
import static android.opengl.GLES31.glBindTexture;
import static android.opengl.GLES31.glBindVertexArray;
import static android.opengl.GLES31.glBufferData;
import static android.opengl.GLES31.glBufferSubData;
import static android.opengl.GLES31.glDrawRangeElements;
import static android.opengl.GLES31.glEnableVertexAttribArray;
import static android.opengl.GLES31.glGenBuffers;
import static android.opengl.GLES31.glGenVertexArrays;
import static android.opengl.GLES31.glGetUniformBlockIndex;
import static android.opengl.GLES31.glGetUniformLocation;
import static android.opengl.GLES31.glProgramUniform3f;
import static android.opengl.GLES31.glUniform1i;
import static android.opengl.GLES31.glUniformBlockBinding;
import static android.opengl.GLES31.glUseProgram;
import static android.opengl.GLES31.glVertexAttribPointer;
import static grabasilak.iti.www.myapplication.Util.m_sizeofM44;
import static grabasilak.iti.www.myapplication.Util.m_sizeofV4;

class Mesh {
                AABB            m_aabb;

    // Vertex Array Object
    private final int []        m_vao             = new int[1];
    // Vertex Buffer Objects
    private final int []        m_vertices_vbo    = new int[1];
    private final int []        m_normals_vbo     = new int[1];
    private final int []        m_uvs_vbo         = new int[1];
    private final int []        m_indices_vbo     = new int[1];
    // Uniform Buffer Object
    private final int []        m_ubo             = new int[1];

    private float[]             m_light_matrix      = new float[16];
    private float[]             m_model_matrix      = new float[16];
    private float[]             m_normal_matrix     = new float[16];

    // Buffers
    private FloatBuffer         m_vertices_buffer;
    private FloatBuffer         m_normals_buffer;
    private FloatBuffer         m_uvs_buffer;
    private ShortBuffer         m_indices_buffer;

    private FloatBuffer         mw_matrix_buffer;
    private FloatBuffer         v_matrix_buffer;
    private FloatBuffer         p_matrix_buffer;
    private FloatBuffer         n_matrix_buffer;
    private FloatBuffer         l_matrix_buffer;
    private FloatBuffer         m_material_buffer;

    private ArrayList<float[]>  m_vertices;
    private ArrayList<float[]>  m_normals;
    private ArrayList<float[]>  m_uvs;
    private ArrayList<Face3D>   m_faces;

    private float m_vertices_data[];
    private float m_normals_data[];
    private float m_uvs_data[];
    private short m_indices_data[];

    Mesh(Context context, String name)
    {
        m_aabb              = new AABB();
        m_vertices          = new ArrayList<>();
        m_normals           = new ArrayList<>();
        m_uvs               = new ArrayList<>();
        m_faces             = new ArrayList<>();

        mw_matrix_buffer    = ByteBuffer.allocateDirect ( m_sizeofM44    ).order ( ByteOrder.nativeOrder() ).asFloatBuffer();
        v_matrix_buffer     = ByteBuffer.allocateDirect ( m_sizeofM44    ).order ( ByteOrder.nativeOrder() ).asFloatBuffer();
        p_matrix_buffer     = ByteBuffer.allocateDirect ( m_sizeofM44    ).order ( ByteOrder.nativeOrder() ).asFloatBuffer();
        n_matrix_buffer     = ByteBuffer.allocateDirect ( m_sizeofM44    ).order ( ByteOrder.nativeOrder() ).asFloatBuffer();
        l_matrix_buffer     = ByteBuffer.allocateDirect ( m_sizeofM44    ).order ( ByteOrder.nativeOrder() ).asFloatBuffer();
        m_material_buffer   = ByteBuffer.allocateDirect ( m_sizeofM44 * 4).order ( ByteOrder.nativeOrder() ).asFloatBuffer();

        setIdentity();

        readMesh(context, name);
    }

    void setIdentity()
    {
        Matrix.setIdentityM (m_model_matrix, 0);
    }

    void translate(float tx, float ty, float tz)
    {
        Matrix.translateM   (m_model_matrix, 0,     tx, ty, tz);
    }

    void rotate(float ra, float rx, float ry, float rz)
    {
        Matrix.rotateM      (m_model_matrix, 0, ra, rx, ry, rz);
    }

    void scale(float sx, float sy, float sz)
    {
        Matrix.scaleM       (m_model_matrix, 0,     sx, sy, sz);
    }

    void draw(int program, Camera camera, Light light, int UBO_Matrices )
    {
        float[] mw_matrix       = new float[16];
        float[] lmw_matrix      = new float[16];
        float[] inv_w_matrix    = new float[16];
        float[] material_data   = new float[16];

        Matrix.invertM(inv_w_matrix, 0, camera.m_world_matrix, 0 );
        Matrix.transposeM(m_normal_matrix, 0, inv_w_matrix, 0 );
        Matrix.multiplyMM(mw_matrix, 0, camera.m_world_matrix, 0, m_model_matrix, 0 );

        Matrix.multiplyMM(lmw_matrix    , 0, light.m_camera.m_view_matrix       , 0, mw_matrix  , 0 );
        Matrix.multiplyMM(m_light_matrix, 0, light.m_camera.m_projection_matrix , 0, lmw_matrix , 0 );

        glBindBuffer(GL_UNIFORM_BUFFER, UBO_Matrices);
        {
            mw_matrix_buffer.put (mw_matrix);
            mw_matrix_buffer.position ( 0 );
            glBufferSubData(GL_UNIFORM_BUFFER, 0			, m_sizeofM44, mw_matrix_buffer);

            v_matrix_buffer.put (camera.m_view_matrix);
            v_matrix_buffer.position ( 0 );
            glBufferSubData(GL_UNIFORM_BUFFER, 1 * m_sizeofM44, m_sizeofM44, v_matrix_buffer);

            p_matrix_buffer.put (camera.m_projection_matrix);
            p_matrix_buffer.position ( 0 );
            glBufferSubData(GL_UNIFORM_BUFFER, 2 * m_sizeofM44, m_sizeofM44, p_matrix_buffer);

            n_matrix_buffer.put (m_normal_matrix);
            n_matrix_buffer.position ( 0 );
            glBufferSubData(GL_UNIFORM_BUFFER, 3 * m_sizeofM44, m_sizeofM44, n_matrix_buffer);

            l_matrix_buffer.put (m_light_matrix);
            l_matrix_buffer.position ( 0 );
            glBufferSubData(GL_UNIFORM_BUFFER, 4 * m_sizeofM44, m_sizeofM44, l_matrix_buffer);
        }
        glBindBuffer(GL_UNIFORM_BUFFER, 0);

        // Add program to OpenGL environment
        glUseProgram(program);
        {
            // 2. SET UNIFORMS
            glUniformBlockBinding(program, glGetUniformBlockIndex(program, "Matrices")  , 0);
            glUniformBlockBinding(program, glGetUniformBlockIndex(program, "Material")  , 1);
            glUniformBlockBinding(program, glGetUniformBlockIndex(program, "Light")     , 2);

            glUniform1i(glGetUniformLocation(program, "uniform_textures.diffuse")       , 0);
            glUniform1i(glGetUniformLocation(program, "uniform_textures.normal")        , 1);
            glUniform1i(glGetUniformLocation(program, "uniform_textures.specular")      , 2);
            glUniform1i(glGetUniformLocation(program, "uniform_textures.emission")      , 3);
            glUniform1i(glGetUniformLocation(program, "uniform_textures.shadow_map")    , 4);

            glProgramUniform3f(program, glGetUniformLocation(program, "uniform_camera.position_wcs"), camera.m_eye[0], camera.m_eye[1], camera.m_eye[2]);

            // 3. SET UBOs

            //material_has_tex_loaded
            material_data[0 ] = 0;
            material_data[1 ] = 0;
            material_data[2 ] = 0;
            material_data[3 ] = 0;
            //material_diffuse_opacity
            material_data[4 ] = 1.0f;
            material_data[5 ] = 0.5f;
            material_data[6 ] = 0.5f;
            material_data[7 ] = 1;
            //material_specular_gloss
            material_data[8 ] = 1;
            material_data[9 ] = 1;
            material_data[10] = 1;
            material_data[11] = 40;
            //material_emission
            material_data[12] = 0;
            material_data[13] = 0;
            material_data[14] = 0;
            material_data[15] = 0;

            glBindBuffer(GL_UNIFORM_BUFFER, m_ubo[0]);
            {
                m_material_buffer.put (material_data);
                m_material_buffer.position ( 0 );
                glBufferSubData(GL_UNIFORM_BUFFER, 0, m_sizeofV4*4, m_material_buffer);
            }
            glBindBuffer(GL_UNIFORM_BUFFER, 0);

            // 4. SET TEXTURES

            // bind the depth texture to the active texture unit
            glActiveTexture(GL_TEXTURE4);
            glBindTexture(GL_TEXTURE_2D, light.m_shadow_map_texture_depth[0]);

            // 3. DRAW
            glBindVertexArray ( m_vao[0] );
            {
                glDrawRangeElements(GL_TRIANGLES, 0, m_indices_data.length, m_indices_data.length, GL_UNSIGNED_SHORT, 0);
            }
            glBindVertexArray ( 0 );
        }
        glUseProgram(0);
    }

    void drawSimple(int program, Camera camera)
    {
        float[] vmw_matrix  = new float[16];
        float[] pvmw_matrix = new float[16];

        Matrix.multiplyMM(vmw_matrix , 0, camera.m_view_matrix       , 0, m_model_matrix  , 0 );
        Matrix.multiplyMM(pvmw_matrix, 0, camera.m_projection_matrix , 0, vmw_matrix , 0 );

        // Add program to OpenGL environment
        glUseProgram(program);
        {
            glUniformMatrix4fv(glGetUniformLocation(program, "uniform_mvp"), 1, false, pvmw_matrix, 0);

            // 3. DRAW
            glBindVertexArray ( m_vao[0] );
            {
                glDrawRangeElements(GL_TRIANGLES, 0, m_indices_data.length, m_indices_data.length, GL_UNSIGNED_SHORT, 0);
            }
            glBindVertexArray ( 0 );
        }
        glUseProgram(0);
    }

    private boolean readMesh(Context context, String name) {

        if ( name == null )
            return false;

        InputStream     is;
        BufferedReader  in;

        try {
            is = context.getAssets().open ( "Models/" + name );
            in = new BufferedReader(new InputStreamReader(is));

            loadOBJ(in);

            in.close();

            Log.d("LOADING FILE", "FILE LOADED SUCCESSFULLY !");
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return true;
    }

    private void loadOBJ(BufferedReader in) throws IOException
    {
        //Log.d("LOADING FILE", "STARTING!====================");

        String   Line;              // Stores ever line we read!
        String[] Blocks;            // Stores string fragments after the split!!
        String   CommandBlock;      // Stores Command Blocks such as: v, vt, vn, g, etc...

        while((Line = in.readLine()) != null)
        {
            if(Line.isEmpty() || Line.length() == 1)
                continue;

            Blocks = Line.split("\\s+"); //split by space character
            CommandBlock = Blocks[0];

           // Log.d("COMMAND BLOCK" , "---------- " + CommandBlock + " ----------");

            switch (CommandBlock) {
                case "v":
                    float [] vertex = new float[3];
                    vertex[0] = Float.parseFloat(Blocks[1]);
                    vertex[1] = Float.parseFloat(Blocks[2]);
                    vertex[2] = Float.parseFloat(Blocks[3]);
                    m_vertices.add(vertex);

                    m_aabb.m_min[0] = Math.min(m_aabb.m_min[0], vertex[0]);
                    m_aabb.m_min[1] = Math.min(m_aabb.m_min[1], vertex[1]);
                    m_aabb.m_min[2] = Math.min(m_aabb.m_min[2], vertex[2]);

                    m_aabb.m_max[0] = Math.max(m_aabb.m_max[0], vertex[0]);
                    m_aabb.m_max[1] = Math.max(m_aabb.m_max[1], vertex[1]);
                    m_aabb.m_max[2] = Math.max(m_aabb.m_max[2], vertex[2]);

                    // Log.d("VERTEX DATA", " " + vertex.x + ", " + vertex.y + ", " + vertex.z);
                    break;
                case "vt":
                    float [] vertexTex = new float[3];
                    vertexTex[0] = Float.parseFloat(Blocks[1]);
                    vertexTex[1] = Float.parseFloat(Blocks[2]);
                    vertexTex[2] = 0.0f;

                    m_uvs.add(vertexTex);
                    // Log.d("TEXTURE DATA", " " + vertexTex.x + ", " + vertexTex.y + ", " + vertexTex.z);
                    break;
                case "vn":
                    float [] vertexNorm = new float[3];
                    vertexNorm[0] = Float.parseFloat(Blocks[1]);
                    vertexNorm[1] = Float.parseFloat(Blocks[2]);
                    vertexNorm[2] = Float.parseFloat(Blocks[3]);

                    m_normals.add(vertexNorm);
                    // Log.d("NORMAL DATA", " " + vertexNorm.x + ", " + vertexNorm.y + ", " + vertexNorm.z);
                    break;
                case "f":
                    String[] faceParams;

                    Face3D face = new Face3D();
                    for (int i = 1; i < Blocks.length; i++) {
                        String split_char = "/";
                        if (Blocks[i].contains("//"))
                            split_char = "//";

                        faceParams = Blocks[i].split(split_char);

                        face.vertices.add(Integer.parseInt(faceParams[0]) - 1);
                        if (faceParams.length == 2) {
                            if (!m_uvs.isEmpty())
                                face.uvs.add(Integer.parseInt(faceParams[1]) - 1);
                            else if (!m_normals.isEmpty())
                                face.normals.add(Integer.parseInt(faceParams[1]) - 1);
                        } else if (faceParams.length == 3) {
                            if (!m_uvs.isEmpty())
                                face.uvs.add(Integer.parseInt(faceParams[1]) - 1);
                            if (!m_normals.isEmpty())
                                face.normals.add(Integer.parseInt(faceParams[2]) - 1);
                        }
                    }
                    m_faces.add(face);
                    break;
            }
        }

        Log.d("OBJ OBJECT DATA", "V = " + m_vertices.size() + " VN = " + m_uvs.size() + " VT = " + m_normals.size() + " F = " + m_faces.size() );

        fillInBuffers();

        m_aabb.computeCenter();
        m_aabb.computeRadius();
    }

    private void fillInBuffers()
    {
        m_vertices_data = new float[m_faces.size() * 3 * 3];
        if(!m_normals.isEmpty())    m_normals_data  = new float[m_faces.size() * 3 * 3];
        if(!m_uvs.isEmpty())        m_uvs_data      = new float[m_faces.size() * 3 * 2];
        m_indices_data  = new short[m_faces.size() * 3];

        for(int i = 0; i < m_faces.size(); i++)
        {
            Face3D face = m_faces.get(i);
            m_vertices_data[i * 9]     = m_vertices.get(face.vertices.get(0))[0];
            m_vertices_data[i * 9 + 1] = m_vertices.get(face.vertices.get(0))[1];
            m_vertices_data[i * 9 + 2] = m_vertices.get(face.vertices.get(0))[2];
            m_vertices_data[i * 9 + 3] = m_vertices.get(face.vertices.get(1))[0];
            m_vertices_data[i * 9 + 4] = m_vertices.get(face.vertices.get(1))[1];
            m_vertices_data[i * 9 + 5] = m_vertices.get(face.vertices.get(1))[2];
            m_vertices_data[i * 9 + 6] = m_vertices.get(face.vertices.get(2))[0];
            m_vertices_data[i * 9 + 7] = m_vertices.get(face.vertices.get(2))[1];
            m_vertices_data[i * 9 + 8] = m_vertices.get(face.vertices.get(2))[2];
        }

        if(!m_normals.isEmpty())
            for(int i = 0; i < m_faces.size(); i++)
            {
                Face3D face = m_faces.get(i);
                m_normals_data[i * 9]     = m_normals.get(face.normals.get(0))[0];
                m_normals_data[i * 9 + 1] = m_normals.get(face.normals.get(0))[1];
                m_normals_data[i * 9 + 2] = m_normals.get(face.normals.get(0))[2];
                m_normals_data[i * 9 + 3] = m_normals.get(face.normals.get(1))[0];
                m_normals_data[i * 9 + 4] = m_normals.get(face.normals.get(1))[1];
                m_normals_data[i * 9 + 5] = m_normals.get(face.normals.get(1))[2];
                m_normals_data[i * 9 + 6] = m_normals.get(face.normals.get(2))[0];
                m_normals_data[i * 9 + 7] = m_normals.get(face.normals.get(2))[1];
                m_normals_data[i * 9 + 8] = m_normals.get(face.normals.get(2))[2];
            }

        if(!m_uvs.isEmpty())
            for(int i = 0; i < m_faces.size(); i++)
            {
                Face3D face = m_faces.get(i);
                m_uvs_data[i * 6]     = m_uvs.get(face.uvs.get(0))[0];
                m_uvs_data[i * 6 + 1] = m_uvs.get(face.uvs.get(0))[1];
                m_uvs_data[i * 6 + 2] = m_uvs.get(face.uvs.get(1))[0];
                m_uvs_data[i * 6 + 3] = m_uvs.get(face.uvs.get(1))[1];
                m_uvs_data[i * 6 + 4] = m_uvs.get(face.uvs.get(2))[0];
                m_uvs_data[i * 6 + 5] = m_uvs.get(face.uvs.get(2))[1];
            }

        for(int i = 0; i < m_faces.size(); i++)
        {
            m_indices_data[i * 3]     = (short) (i * 3);
            m_indices_data[i * 3 + 1] = (short) (i * 3 + 1);
            m_indices_data[i * 3 + 2] = (short) (i * 3 + 2);
        }

        createBuffers();
        createVBOs();
        createVAO();
        createUBO();
    }

    private void createBuffers()
    {
        // initialize m_vertices byte buffer for shape coordinates
        m_vertices_buffer = ByteBuffer.allocateDirect ( m_vertices_data.length * Float.BYTES ).order ( ByteOrder.nativeOrder() ).asFloatBuffer();
        m_vertices_buffer.put (m_vertices_data);
        m_vertices_buffer.position ( 0 );

        // initialize normals byte buffer for shape coordinates
        if(!m_normals.isEmpty())
        {
            m_normals_buffer = ByteBuffer.allocateDirect(m_normals_data.length * Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
            m_normals_buffer.put(m_normals_data);
            m_normals_buffer.position(0);
        }

        if(!m_uvs.isEmpty())
        {
            m_uvs_buffer = ByteBuffer.allocateDirect(m_uvs_data.length * Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
            m_uvs_buffer.put(m_uvs_data);
            m_uvs_buffer.position(0);
        }

        // initialize indices byte buffer for shape coordinates
        m_indices_buffer = ByteBuffer.allocateDirect ( m_indices_data.length * Short.BYTES ).order ( ByteOrder.nativeOrder() ).asShortBuffer();
        m_indices_buffer.put (m_indices_data);
        m_indices_buffer.position ( 0 );
    }

    private void createVBOs()
    {
        // Generate VBO Ids and load the VBOs with data
        glGenBuffers ( 1, m_vertices_vbo, 0 );
        {
            glBindBuffer(GL_ARRAY_BUFFER, m_vertices_vbo[0]);
            m_vertices_buffer.position(0);
            glBufferData(GL_ARRAY_BUFFER, m_vertices_data.length * Float.BYTES, m_vertices_buffer, GL_STATIC_DRAW);
        }
        glBindBuffer ( GL_ARRAY_BUFFER, 0 );

        if(!m_normals.isEmpty())
        {
            glGenBuffers(1, m_normals_vbo, 0);
            {
                glBindBuffer(GL_ARRAY_BUFFER, m_normals_vbo[0]);
                m_normals_buffer.position(0);
                glBufferData(GL_ARRAY_BUFFER, m_normals_data.length * Float.BYTES, m_normals_buffer, GL_STATIC_DRAW);
            }
            glBindBuffer(GL_ARRAY_BUFFER, 0);
        }

        if(!m_uvs.isEmpty())
        {
            glGenBuffers(1, m_uvs_vbo, 0);
            {
                glBindBuffer(GL_ARRAY_BUFFER, m_uvs_vbo[0]);
                m_uvs_buffer.position(0);
                glBufferData(GL_ARRAY_BUFFER, m_uvs_data.length * Float.BYTES, m_uvs_buffer, GL_STATIC_DRAW);
            }
            glBindBuffer(GL_ARRAY_BUFFER, 0);
        }

        glGenBuffers ( 1, m_indices_vbo, 0 );
        {
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, m_indices_vbo[0]);
            m_indices_buffer.position(0);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, m_indices_data.length * Short.BYTES, m_indices_buffer, GL_STATIC_DRAW);
        }
        glBindBuffer ( GL_ELEMENT_ARRAY_BUFFER, 0 );
    }

    private void createVAO()
    {
        // Generate VAO Id
        glGenVertexArrays ( 1, m_vao, 0 );
        // Bind the VAO and then setup the vertex attributes
        glBindVertexArray ( m_vao[0] );
        {
            glBindBuffer(GL_ARRAY_BUFFER, m_vertices_vbo[0]);
            glEnableVertexAttribArray(0);
            glVertexAttribPointer(0, 3, GL_FLOAT, false, Float.BYTES * 3, 0);

            if (!m_normals.isEmpty())
            {
                glBindBuffer(GL_ARRAY_BUFFER, m_normals_vbo[0]);
                glEnableVertexAttribArray(1);
                glVertexAttribPointer(1, 3, GL_FLOAT, false, Float.BYTES * 3, 0);
            }

            if (!m_uvs.isEmpty())
            {
                glBindBuffer(GL_ARRAY_BUFFER, m_uvs_vbo[0]);
                glEnableVertexAttribArray(2);
                glVertexAttribPointer(2, 2, GL_FLOAT, false, Float.BYTES * 2, 0);
            }

            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, m_indices_vbo[0]);
        }
        // Reset to the default VAO
        glBindVertexArray ( 0 );
    }

    private void createUBO()
    {
        glGenBuffers(1, m_ubo, 0);
        glBindBuffer(GL_UNIFORM_BUFFER, m_ubo[0]);
        {
            glBufferData(GL_UNIFORM_BUFFER, 4 * m_sizeofV4, null, GL_DYNAMIC_DRAW);
        }
        glBindBuffer(GL_UNIFORM_BUFFER, 0);
        glBindBufferBase(GL_UNIFORM_BUFFER, 1, m_ubo[0]);
    }
}