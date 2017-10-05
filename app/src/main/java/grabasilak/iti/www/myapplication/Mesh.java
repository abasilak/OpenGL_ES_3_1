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
import java.nio.IntBuffer;
import java.util.ArrayList;

import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE1;
import static android.opengl.GLES20.GL_TEXTURE2;
import static android.opengl.GLES20.GL_TEXTURE3;
import static android.opengl.GLES20.GL_TEXTURE5;
import static android.opengl.GLES20.GL_UNSIGNED_INT;
import static android.opengl.GLES20.glUniform2i;
import static android.opengl.GLES20.glUniform3f;
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
import static android.opengl.GLES31.glGetUniformLocation;
import static android.opengl.GLES31.glUseProgram;
import static android.opengl.GLES31.glVertexAttribPointer;
import static grabasilak.iti.www.myapplication.Util.m_sizeofM44;

class Mesh
{
            ArrayList  <Material> m_mtl_materials;
    private ArrayList  <String>   m_mtl_filenames;

    AABB                        m_aabb;

    // Vertex Array Object
    private final int []        m_vao             = new int[1];
    // Vertex Buffer Objects
    private final int []        m_vertices_vbo    = new int[1];
    private final int []        m_normals_vbo     = new int[1];
    private final int []        m_uvs_vbo         = new int[1];
    private final int []        m_indices_vbo     = new int[1];

    // Indirect Buffer Objects
    //private final int []        m_indirect_bo    = new int[1];

    // Uniform Buffer Object
    private final int []        m_ubo             = new int[1];

    private float[]             m_light_matrix      = new float[16];
    private float[]             m_model_matrix      = new float[16];
    private float[]             m_normal_matrix     = new float[16];

    // Buffers
    private FloatBuffer         m_vertices_buffer;
    private FloatBuffer         m_normals_buffer;
    private FloatBuffer         m_uvs_buffer;
    private IntBuffer           m_indices_buffer;
    //private IntBuffer           m_indirect_buffer;

    private FloatBuffer         mw_matrix_buffer;
    private FloatBuffer         v_matrix_buffer;
    private FloatBuffer         p_matrix_buffer;
    private FloatBuffer         n_matrix_buffer;
    private FloatBuffer         l_matrix_buffer;

    private ArrayList<float[]>  m_vertices;
    private ArrayList<float[]>  m_normals;
    private ArrayList<float[]>  m_uvs;

    private float m_vertices_data[];
    private float m_normals_data[];
    private float m_uvs_data[];
    private int   m_indices_data[];

    private ArrayList<MeshPrimitiveGroup>   m_primitive_groups;

    //private int   m_indirect_data[];
    //private ElementsIndirectCommand m_commands[];

    Mesh(Context context, String name)
    {
        m_aabb              = new AABB();

        m_mtl_filenames     = new ArrayList<>();
        m_mtl_materials     = new ArrayList<>();
        m_mtl_materials.add(new Material());

        m_vertices          = new ArrayList<>();
        m_normals           = new ArrayList<>();
        m_uvs               = new ArrayList<>();

        m_primitive_groups  = new ArrayList<>();

        mw_matrix_buffer    = ByteBuffer.allocateDirect ( m_sizeofM44    ).order ( ByteOrder.nativeOrder() ).asFloatBuffer();
        v_matrix_buffer     = ByteBuffer.allocateDirect ( m_sizeofM44    ).order ( ByteOrder.nativeOrder() ).asFloatBuffer();
        p_matrix_buffer     = ByteBuffer.allocateDirect ( m_sizeofM44    ).order ( ByteOrder.nativeOrder() ).asFloatBuffer();
        n_matrix_buffer     = ByteBuffer.allocateDirect ( m_sizeofM44    ).order ( ByteOrder.nativeOrder() ).asFloatBuffer();
        l_matrix_buffer     = ByteBuffer.allocateDirect ( m_sizeofM44    ).order ( ByteOrder.nativeOrder() ).asFloatBuffer();

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

    void drawSimple(int program, Camera camera)
    {
        float[]  mw_matrix  = new float[16];
        float[] vmw_matrix  = new float[16];
        float[] pvmw_matrix = new float[16];

        Matrix.multiplyMM(mw_matrix  , 0, camera.m_world_matrix      , 0, m_model_matrix, 0 );
        Matrix.multiplyMM(vmw_matrix , 0, camera.m_view_matrix       , 0, mw_matrix  , 0 );
        Matrix.multiplyMM(pvmw_matrix, 0, camera.m_projection_matrix , 0, vmw_matrix , 0 );

        // Add program to OpenGL environment
        glUseProgram(program);
        {
            glUniformMatrix4fv(glGetUniformLocation(program, "uniform_mvp"  ), 1, false, pvmw_matrix, 0);

            glUniform3f(glGetUniformLocation(program, "uniform_color"), m_mtl_materials.get(0).m_diffuse[0], m_mtl_materials.get(0).m_diffuse[1], m_mtl_materials.get(0).m_diffuse[2]);

            // 3. DRAW
            int end;
            glBindVertexArray ( m_vao[0] );
            for (int i = 0, start = 0; i < m_primitive_groups.size(); i++, start += end)
            {
                end = m_primitive_groups.get(i).m_primitives.size()*3;
                glDrawRangeElements(GL_TRIANGLES, start, start + end, end, GL_UNSIGNED_INT, start*Integer.BYTES);
            }
            glBindVertexArray ( 0 );
        }
        glUseProgram(0);
    }

    void drawShadowMapping(int program, Camera camera, Light light)
    {
        float[] mw_matrix   = new float[16];
        float[] vmw_matrix  = new float[16];
        float[] pvmw_matrix = new float[16];

        Matrix.multiplyMM(mw_matrix  , 0, camera.m_world_matrix              , 0, m_model_matrix, 0 );
        Matrix.multiplyMM(vmw_matrix , 0, light.m_camera.m_view_matrix       , 0, mw_matrix     , 0 );
        Matrix.multiplyMM(pvmw_matrix, 0, light.m_camera.m_projection_matrix , 0, vmw_matrix    , 0 );

        glUseProgram(program);
        {
            glUniformMatrix4fv(glGetUniformLocation(program, "uniform_mvp"  ), 1, false, pvmw_matrix, 0);

            glBindVertexArray ( m_vao[0] );
            {
                glDrawRangeElements(GL_TRIANGLES, 0, m_indices_data.length, m_indices_data.length, GL_UNSIGNED_INT, 0);

                //glBindBuffer(GL_DRAW_INDIRECT_BUFFER, m_indirect_bo[0]);
                //glDrawElementsIndirect(GL_TRIANGLES, GL_UNSIGNED_INT, (5* Integer.SIZE));
            }
            glBindVertexArray ( 0 );
        }
        glUseProgram(0);
    }

    void draw(int program, Camera camera, ArrayList<Light> lights, int UBO_Matrices)
    {
        float[] mw_matrix       = new float[16];
        float[] lmw_matrix      = new float[16];
        float[] inv_w_matrix    = new float[16];

        Matrix.invertM(inv_w_matrix, 0, camera.m_world_matrix, 0 );
        Matrix.transposeM(m_normal_matrix, 0, inv_w_matrix, 0 );
        Matrix.multiplyMM(mw_matrix, 0, camera.m_world_matrix, 0, m_model_matrix, 0 );

        Matrix.multiplyMM(lmw_matrix    , 0, lights.get(0).m_camera.m_view_matrix       , 0, mw_matrix  , 0 );
        Matrix.multiplyMM(m_light_matrix, 0, lights.get(0).m_camera.m_projection_matrix , 0, lmw_matrix , 0 );

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

            glUniform3f(glGetUniformLocation(program, "uniform_camera_position_wcs"), camera.m_eye[0], camera.m_eye[1], camera.m_eye[2]);

            // 3. SET TEXTURES

            // bind the depth texture to the active texture unit
            glActiveTexture(GL_TEXTURE4);
            glBindTexture(GL_TEXTURE_2D, lights.get(0).m_shadow_mapping.getTextureDepth());

            int end;
            glBindVertexArray ( m_vao[0] );
            for (int i = 0, start = 0; i < m_primitive_groups.size(); i++, start += end)
            {
                if ( m_primitive_groups.get(i).m_material.m_diffuse_tex.m_loaded)
                {
                    glActiveTexture(GL_TEXTURE0);
                    glBindTexture(GL_TEXTURE_2D,  m_primitive_groups.get(i).m_material.m_diffuse_tex.m_id);
                }

                if ( m_primitive_groups.get(i).m_material.m_normal_tex.m_loaded)
                {
                    glActiveTexture(GL_TEXTURE1);
                    glBindTexture(GL_TEXTURE_2D,  m_primitive_groups.get(i).m_material.m_normal_tex.m_id);
                }

                if ( m_primitive_groups.get(i).m_material.m_specular_tex.m_loaded)
                {
                    glActiveTexture(GL_TEXTURE2);
                    glBindTexture(GL_TEXTURE_2D,  m_primitive_groups.get(i).m_material.m_specular_tex.m_id);
                }

                if ( m_primitive_groups.get(i).m_material.m_emission_tex.m_loaded)
                {
                    glActiveTexture(GL_TEXTURE3);
                    glBindTexture(GL_TEXTURE_2D,  m_primitive_groups.get(i).m_material.m_emission_tex.m_id);
                }

                // 4. SET UBO
                glBindBuffer(GL_UNIFORM_BUFFER, m_ubo[0]);
                {
                    glBufferSubData(GL_UNIFORM_BUFFER, 0, m_sizeofM44, m_primitive_groups.get(i).m_material_buffer);
                }
                glBindBuffer(GL_UNIFORM_BUFFER, 0);

                // 3. DRAW
                {
                    end = m_primitive_groups.get(i).m_primitives.size()*3;
                    glDrawRangeElements(GL_TRIANGLES, start, start + end, end, GL_UNSIGNED_INT, start*Integer.BYTES);
                }
            }
            glBindVertexArray ( 0 );

            for (int i = 0; i < 5; i++)
            {
                glActiveTexture(GL_TEXTURE0 + i);
                glBindTexture(GL_TEXTURE_2D, 0);
            }
        }
        glUseProgram(0);
    }

    void peel(int program, Camera camera, ArrayList<Light> lights, int UBO_Matrices, int texture_depth, Viewport viewport)
    {
        float[] mw_matrix       = new float[16];
        float[] lmw_matrix      = new float[16];
        float[] inv_w_matrix    = new float[16];

        Matrix.invertM(inv_w_matrix, 0, camera.m_world_matrix, 0 );
        Matrix.transposeM(m_normal_matrix, 0, inv_w_matrix, 0 );
        Matrix.multiplyMM(mw_matrix, 0, camera.m_world_matrix, 0, m_model_matrix, 0 );

        Matrix.multiplyMM(lmw_matrix    , 0, lights.get(0).m_camera.m_view_matrix       , 0, mw_matrix  , 0 );
        Matrix.multiplyMM(m_light_matrix, 0, lights.get(0).m_camera.m_projection_matrix , 0, lmw_matrix , 0 );

        glBindBuffer(GL_UNIFORM_BUFFER, UBO_Matrices);
        {
            mw_matrix_buffer.put (mw_matrix);
            mw_matrix_buffer.position ( 0 );
            glBufferSubData(GL_UNIFORM_BUFFER, 0			  , m_sizeofM44, mw_matrix_buffer);

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
            glUniform2i(glGetUniformLocation(program, "uniform_resolution"), viewport.m_width, viewport.m_height);
            glUniform3f(glGetUniformLocation(program, "uniform_camera_position_wcs"), camera.m_eye[0], camera.m_eye[1], camera.m_eye[2]);

            // 3. SET TEXTURES
            glActiveTexture(GL_TEXTURE4);
            glBindTexture(GL_TEXTURE_2D, lights.get(0).m_shadow_mapping.getTextureDepth());

            glActiveTexture(GL_TEXTURE5);
            glBindTexture(GL_TEXTURE_2D, texture_depth);

            int end;
            glBindVertexArray ( m_vao[0] );
            for (int i = 0, start = 0; i < m_primitive_groups.size(); i++, start += end)
            {
                if ( m_primitive_groups.get(i).m_material.m_diffuse_tex.m_loaded)
                {
                    glActiveTexture(GL_TEXTURE0);
                    glBindTexture(GL_TEXTURE_2D,  m_primitive_groups.get(i).m_material.m_diffuse_tex.m_id);
                }

                if ( m_primitive_groups.get(i).m_material.m_normal_tex.m_loaded)
                {
                    glActiveTexture(GL_TEXTURE1);
                    glBindTexture(GL_TEXTURE_2D,  m_primitive_groups.get(i).m_material.m_normal_tex.m_id);
                }

                if ( m_primitive_groups.get(i).m_material.m_specular_tex.m_loaded)
                {
                    glActiveTexture(GL_TEXTURE2);
                    glBindTexture(GL_TEXTURE_2D,  m_primitive_groups.get(i).m_material.m_specular_tex.m_id);
                }

                if ( m_primitive_groups.get(i).m_material.m_emission_tex.m_loaded)
                {
                    glActiveTexture(GL_TEXTURE3);
                    glBindTexture(GL_TEXTURE_2D,  m_primitive_groups.get(i).m_material.m_emission_tex.m_id);
                }

                // 4. SET UBO
                glBindBuffer(GL_UNIFORM_BUFFER, m_ubo[0]);
                {
                    glBufferSubData(GL_UNIFORM_BUFFER, 0, m_sizeofM44, m_primitive_groups.get(i).m_material_buffer);
                }
                glBindBuffer(GL_UNIFORM_BUFFER, 0);

                // 5. DRAW
                {
                    end = m_primitive_groups.get(i).m_primitives.size()*3;
                    glDrawRangeElements(GL_TRIANGLES, start, start + end, end, GL_UNSIGNED_INT, start*Integer.BYTES);
                }
            }
            glBindVertexArray ( 0 );

            for (int i = 0; i < 6; i++)
            {
                glActiveTexture(GL_TEXTURE0 + i);
                glBindTexture(GL_TEXTURE_2D, 0);
            }
        }
        glUseProgram(0);
    }

    private boolean readMesh(Context context, String name) {

        if ( name == null )
            return false;

        InputStream     is;
        BufferedReader  in;

        try
        {
            is = context.getAssets().open ( "Models/" + name );
            in = new BufferedReader(new InputStreamReader(is));

            loadOBJ(in);

            in.close();

            Log.d("LOADING FILE", name + " - FILE LOADED SUCCESSFULLY !");
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        try
        {
            for(int i = 0; i < m_mtl_filenames.size(); i++)
            {
                is = context.getAssets().open("Materials/" + m_mtl_filenames.get(i));
                in = new BufferedReader(new InputStreamReader(is));

                loadMaterial(in, context);

                in.close();

                Log.d("LOADING MATERIAL", "MATERIAL LOADED SUCCESSFULLY !");
            }

            for (int i = 0; i < m_primitive_groups.size(); i++)
            {
                Material material = m_mtl_materials.get(0);
                if(m_primitive_groups.get(i).m_material_name != null)
                    for (int j = 1; j < m_mtl_materials.size(); j++)
                        if (m_mtl_materials.get(j).m_name.equals(m_primitive_groups.get(i).m_material_name))
                        {
                            material = m_mtl_materials.get(j);
                            break;
                        }
                m_primitive_groups.get(i).m_material        = material;
                m_primitive_groups.get(i).m_material_name   = material.m_name;
                m_primitive_groups.get(i).setMaterialData();
            }
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
                case "#":
                    //Log.d("COMMENT", "" + Blocks[1] );
                    break;
                case "s":
                    break;
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
                    vertexTex[2] = (Blocks.length ==4) ? Float.parseFloat(Blocks[3]) : 0.0f;

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

                    MeshPrimitive primitive = new MeshPrimitive();
                    for (int i = 1; i < Blocks.length; i++)
                    {
                        String split_char = "/";
                        if (Blocks[i].contains("//"))
                            split_char = "//";

                        faceParams = Blocks[i].split(split_char);

                        primitive.vertices.add(Integer.parseInt(faceParams[0]) - 1);
                        if (faceParams.length == 2)
                        {
                            //if (!m_uvs.isEmpty())
                              //  primitive.uvs.add(Integer.parseInt(faceParams[1]) - 1);
                            //else if (!m_normals.isEmpty())
                                primitive.normals.add(Integer.parseInt(faceParams[1]) - 1);
                        }
                        else if (faceParams.length == 3)
                        {
                            //if (!m_uvs.isEmpty())
                                primitive.uvs.add(Integer.parseInt(faceParams[1]) - 1);
                            //if (!m_normals.isEmpty())
                                primitive.normals.add(Integer.parseInt(faceParams[2]) - 1);
                        }
                    }
                    m_primitive_groups.get(m_primitive_groups.size()-1).m_primitives.add(primitive);

                    //Log.d("PRIMITIVE DATA", " " + primitive.vertices.get(0) + ", " + primitive.vertices.get(1) + ", " + primitive.vertices.get(2));
                    break;
                case "g":
                    MeshPrimitiveGroup new_primitive_group = new MeshPrimitiveGroup();
                    m_primitive_groups.add(new_primitive_group);
                    break;
                case "mtllib":
                    m_mtl_filenames.add(Blocks[1]);
                    break;
                case "usemtl":
                    m_primitive_groups.get(m_primitive_groups.size()-1).m_material_name = Blocks[1];
                    break;
            }
        }

        Log.d("OBJ OBJECT DATA", "V = " + m_vertices.size() + " VT = " + m_uvs.size() + " VN = " + m_normals.size() + " G = " + m_primitive_groups.size() );

        fillInBuffers();

        m_aabb.computeCenter();
        m_aabb.computeRadius();
    }

    private void loadMaterial(BufferedReader in, Context context) throws IOException
    {
        String   Line;              // Stores ever line we read!
        String[] Blocks;            // Stores string fragments after the split!!
        String   CommandBlock;      // Stores Command Blocks such as: v, vt, vn, g, etc...

        while((Line = in.readLine()) != null)
        {
            if(Line.isEmpty() || Line.length() == 1)
                continue;

            Blocks = Line.split("\\s+"); //split by space character
            CommandBlock = Blocks[0];

            switch (CommandBlock)
            {
                case "newmtl":
                    Material new_material = new Material(Blocks[1]);
                    m_mtl_materials.add(new_material);
                    break;
                case "Kd":
                    m_mtl_materials.get(m_mtl_materials.size()-1).m_diffuse[0] = Float.parseFloat(Blocks[1]);
                    m_mtl_materials.get(m_mtl_materials.size()-1).m_diffuse[1] = Float.parseFloat(Blocks[2]);
                    m_mtl_materials.get(m_mtl_materials.size()-1).m_diffuse[2] = Float.parseFloat(Blocks[3]);
                    break;
                case "Ks":
                    m_mtl_materials.get(m_mtl_materials.size()-1).m_specular[0] = Float.parseFloat(Blocks[1]);
                    m_mtl_materials.get(m_mtl_materials.size()-1).m_specular[1] = Float.parseFloat(Blocks[2]);
                    m_mtl_materials.get(m_mtl_materials.size()-1).m_specular[2] = Float.parseFloat(Blocks[3]);
                    break;
                case "Ke":
                    m_mtl_materials.get(m_mtl_materials.size()-1).m_emission[0] = Float.parseFloat(Blocks[1]);
                    m_mtl_materials.get(m_mtl_materials.size()-1).m_emission[1] = Float.parseFloat(Blocks[2]);
                    m_mtl_materials.get(m_mtl_materials.size()-1).m_emission[2] = Float.parseFloat(Blocks[3]);
                    break;
                case "Ni":
                    m_mtl_materials.get(m_mtl_materials.size()-1).m_refraction_index = Float.parseFloat(Blocks[1]);
                    break;
                case "Ns":
                    m_mtl_materials.get(m_mtl_materials.size()-1).m_gloss = Float.parseFloat(Blocks[1]);
                    break;
                case "d":
                    m_mtl_materials.get(m_mtl_materials.size()-1).m_opacity    = Float.parseFloat(Blocks[1]);
                    break;
                case "Tr":
                    m_mtl_materials.get(m_mtl_materials.size()-1).m_opacity    = 1.f - Float.parseFloat(Blocks[1]);
                    break;
                case "map_Ka":
                    //m_mtl_materials.get(m_mtl_materials.size()-1).m_ambient_tex = Blocks[1];
                    break;
                case "map_Kd":
                    m_mtl_materials.get(m_mtl_materials.size()-1).m_diffuse_tex.load(context, "Textures/" + Blocks[1]);
                    break;
                case "map_Ks":
                    m_mtl_materials.get(m_mtl_materials.size()-1).m_specular_tex.load(context, "Textures/" + Blocks[1]);
                    break;
                case "map_Ke":
                    m_mtl_materials.get(m_mtl_materials.size()-1).m_emission_tex.load(context, "Textures/" + Blocks[1]);
                    break;
                case "map_bump":
                    m_mtl_materials.get(m_mtl_materials.size()-1).m_normal_tex.load(context, "Textures/" + Blocks[1]);
                    break;
            }
        }

        Log.d("MATERIAL DATA", "" + m_mtl_materials.size() );
    }

    private void fillInBuffers()
    {
        int total_primitive_size = 0;
        for (int i = 0; i < m_primitive_groups.size(); i++)
            total_primitive_size += m_primitive_groups.get(i).m_primitives.size();

        int i=0;
        m_vertices_data = new float[total_primitive_size * 3 * 3];
        for(int j = 0; j < m_primitive_groups.size(); j++)
        for(int k = 0; k < m_primitive_groups.get(j).m_primitives.size(); k++)
        {
            MeshPrimitive face         = m_primitive_groups.get(j).m_primitives.get(k);
            m_vertices_data[i * 9]     = m_vertices.get(face.vertices.get(0))[0];
            m_vertices_data[i * 9 + 1] = m_vertices.get(face.vertices.get(0))[1];
            m_vertices_data[i * 9 + 2] = m_vertices.get(face.vertices.get(0))[2];
            m_vertices_data[i * 9 + 3] = m_vertices.get(face.vertices.get(1))[0];
            m_vertices_data[i * 9 + 4] = m_vertices.get(face.vertices.get(1))[1];
            m_vertices_data[i * 9 + 5] = m_vertices.get(face.vertices.get(1))[2];
            m_vertices_data[i * 9 + 6] = m_vertices.get(face.vertices.get(2))[0];
            m_vertices_data[i * 9 + 7] = m_vertices.get(face.vertices.get(2))[1];
            m_vertices_data[i * 9 + 8] = m_vertices.get(face.vertices.get(2))[2];
            i++;
        }

        i=0;
        if(!m_normals.isEmpty())
        {
            m_normals_data = new float[total_primitive_size * 3 * 3];
            for (int j = 0; j < m_primitive_groups.size(); j++)
                for (int k = 0; k < m_primitive_groups.get(j).m_primitives.size(); k++)
                {
                    MeshPrimitive face = m_primitive_groups.get(j).m_primitives.get(k);
                    m_normals_data[i * 9    ] = m_normals.get(face.normals.get(0))[0];
                    m_normals_data[i * 9 + 1] = m_normals.get(face.normals.get(0))[1];
                    m_normals_data[i * 9 + 2] = m_normals.get(face.normals.get(0))[2];
                    m_normals_data[i * 9 + 3] = m_normals.get(face.normals.get(1))[0];
                    m_normals_data[i * 9 + 4] = m_normals.get(face.normals.get(1))[1];
                    m_normals_data[i * 9 + 5] = m_normals.get(face.normals.get(1))[2];
                    m_normals_data[i * 9 + 6] = m_normals.get(face.normals.get(2))[0];
                    m_normals_data[i * 9 + 7] = m_normals.get(face.normals.get(2))[1];
                    m_normals_data[i * 9 + 8] = m_normals.get(face.normals.get(2))[2];
                    i++;
                }
        }

        i=0;
        if(!m_uvs.isEmpty())
        {
            m_uvs_data      = new float[total_primitive_size * 3 * 2];
            for (int j = 0; j < m_primitive_groups.size(); j++)
                for (int k = 0; k < m_primitive_groups.get(j).m_primitives.size(); k++) {
                    MeshPrimitive face = m_primitive_groups.get(j).m_primitives.get(k);

                    if(face.uvs.isEmpty())
                    {
                        m_uvs_data[i * 6]     = 0;
                        m_uvs_data[i * 6 + 1] = 0;
                        m_uvs_data[i * 6 + 2] = 0;
                        m_uvs_data[i * 6 + 3] = 0;
                        m_uvs_data[i * 6 + 4] = 0;
                        m_uvs_data[i * 6 + 5] = 0;
                    }
                    else
                    {
                        m_uvs_data[i * 6]     = m_uvs.get(face.uvs.get(0))[0];
                        m_uvs_data[i * 6 + 1] = m_uvs.get(face.uvs.get(0))[1];
                        m_uvs_data[i * 6 + 2] = m_uvs.get(face.uvs.get(1))[0];
                        m_uvs_data[i * 6 + 3] = m_uvs.get(face.uvs.get(1))[1];
                        m_uvs_data[i * 6 + 4] = m_uvs.get(face.uvs.get(2))[0];
                        m_uvs_data[i * 6 + 5] = m_uvs.get(face.uvs.get(2))[1];
                    }
                    i++;
                }
        }

        i=0;
        m_indices_data  = new int[total_primitive_size * 3];
        for(int j = 0; j < m_primitive_groups.size(); j++)
            for(int k = 0; k < m_primitive_groups.get(j).m_primitives.size(); k++)
            {
                m_indices_data[i * 3]     = (i * 3);
                m_indices_data[i * 3 + 1] = (i * 3 + 1);
                m_indices_data[i * 3 + 2] = (i * 3 + 2);
                i++;
            }

        //m_indirect_data =

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

        // initialize m_vertices byte buffer for texture coordinates
        if(!m_uvs.isEmpty())
        {
            m_uvs_buffer = ByteBuffer.allocateDirect(m_uvs_data.length * Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
            m_uvs_buffer.put(m_uvs_data);
            m_uvs_buffer.position(0);
        }

        // initialize indices byte buffer for shape coordinates
        m_indices_buffer = ByteBuffer.allocateDirect ( m_indices_data.length * Integer.BYTES ).order ( ByteOrder.nativeOrder() ).asIntBuffer();
        m_indices_buffer.put (m_indices_data);
        m_indices_buffer.position ( 0 );

        // initialize commands buffer for indirect rendering
        //m_indirect_buffer= ByteBuffer.allocateDirect ( m_indirect_data.length * Integer.BYTES ).order ( ByteOrder.nativeOrder() ).asIntBuffer();
        //m_indirect_buffer.put (m_indirect_data);
        //m_indirect_buffer.position ( 0 );
    }

    private void createVBOs()
    {
        // Generate VBO Ids and load the VBOs with data
        glGenBuffers ( 1, m_vertices_vbo, 0 );
        glBindBuffer(GL_ARRAY_BUFFER, m_vertices_vbo[0]);
        {
            glBufferData(GL_ARRAY_BUFFER, m_vertices_data.length * Float.BYTES, m_vertices_buffer, GL_STATIC_DRAW);
        }
        glBindBuffer ( GL_ARRAY_BUFFER, 0 );

        if(!m_normals.isEmpty())
        {
            glGenBuffers(1, m_normals_vbo, 0);
            glBindBuffer(GL_ARRAY_BUFFER, m_normals_vbo[0]);
            {
                glBufferData(GL_ARRAY_BUFFER, m_normals_data.length * Float.BYTES, m_normals_buffer, GL_STATIC_DRAW);
            }
            glBindBuffer(GL_ARRAY_BUFFER, 0);
        }

        if(!m_uvs.isEmpty())
        {
            glGenBuffers(1, m_uvs_vbo, 0);
            glBindBuffer(GL_ARRAY_BUFFER, m_uvs_vbo[0]);
            {
                glBufferData(GL_ARRAY_BUFFER, m_uvs_data.length * Float.BYTES, m_uvs_buffer, GL_STATIC_DRAW);
            }
            glBindBuffer(GL_ARRAY_BUFFER, 0);
        }

        glGenBuffers ( 1, m_indices_vbo, 0 );
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, m_indices_vbo[0]);
        {
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, m_indices_data.length * Integer.BYTES, m_indices_buffer, GL_STATIC_DRAW);
        }
        glBindBuffer ( GL_ELEMENT_ARRAY_BUFFER, 0 );

        //glGenBuffers ( 1, m_indirect_bo, 0 );
      //  glBindBuffer(GL_DRAW_INDIRECT_BUFFER, m_indirect_bo[0]);
    //    {
  //          glBufferData(GL_DRAW_INDIRECT_BUFFER, m_commands[i].size() * (5 * Integer.BYTES), m_indirect_buffer, GL_STATIC_DRAW);
//        }
        //glBindBuffer ( GL_DRAW_INDIRECT_BUFFER, 0 );
    }

    private void createVAO()
    {
        glGenVertexArrays ( 1, m_vao, 0 );
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
        glBindVertexArray ( 0 );
    }

    private void createUBO()
    {
        glGenBuffers(1, m_ubo, 0);
        glBindBuffer(GL_UNIFORM_BUFFER, m_ubo[0]);
        {
            glBufferData(GL_UNIFORM_BUFFER, m_sizeofM44, null, GL_DYNAMIC_DRAW);
        }
        glBindBuffer(GL_UNIFORM_BUFFER, 0);
        glBindBufferBase(GL_UNIFORM_BUFFER, 1, m_ubo[0]);
    }
}