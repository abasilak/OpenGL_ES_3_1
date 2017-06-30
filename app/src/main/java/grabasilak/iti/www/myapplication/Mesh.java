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
import android.renderscript.Float3;
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

import static android.opengl.GLES31.GL_ARRAY_BUFFER;
import static android.opengl.GLES31.GL_ELEMENT_ARRAY_BUFFER;
import static android.opengl.GLES31.GL_FLOAT;
import static android.opengl.GLES31.GL_STATIC_DRAW;
import static android.opengl.GLES31.GL_TRIANGLES;
import static android.opengl.GLES31.GL_UNSIGNED_SHORT;
import static android.opengl.GLES31.glBindBuffer;
import static android.opengl.GLES31.glBindVertexArray;
import static android.opengl.GLES31.glBufferData;
import static android.opengl.GLES31.glDrawRangeElements;
import static android.opengl.GLES31.glEnableVertexAttribArray;
import static android.opengl.GLES31.glGenBuffers;
import static android.opengl.GLES31.glGenVertexArrays;
import static android.opengl.GLES31.glGetUniformLocation;
import static android.opengl.GLES31.glUniform4fv;
import static android.opengl.GLES31.glUniformMatrix4fv;
import static android.opengl.GLES31.glUseProgram;
import static android.opengl.GLES31.glVertexAttribPointer;

class Mesh {

    private       String  m_name;

    // Vertex Array Object
    private final int []  m_vao             = new int[1];
    // Vertex Buffer Objects
    private final int []  m_vertices_vbo    = new int[1];
    private final int []  m_normals_vbo     = new int[1];
    private final int []  m_uvs_vbo         = new int[1];
    private final int []  m_indices_vbo     = new int[1];

    // Buffers
    private FloatBuffer   m_vertices_buffer;
    private FloatBuffer   m_normals_buffer;
    private FloatBuffer   m_uvs_buffer;
    private ShortBuffer   m_indices_buffer;

    private ArrayList<Float3> m_vertices;
    private ArrayList<Float3> m_normals;
    private ArrayList<Float3> m_uvs;
    private ArrayList<Face3D> m_faces;

    private float m_vertices_data[];
    private float m_normals_data[];
    private float m_uvs_data[];
    private short m_indices_data[];

    float m_diffuse_color[] = { 0.2f, 0.709803922f, 0.898039216f, 1.0f };

    Mesh(Context context, String name)
    {
        m_vertices      = new ArrayList<>();
        m_normals       = new ArrayList<>();
        m_uvs           = new ArrayList<>();
        m_faces         = new ArrayList<>();
        m_name          = name;

        readMesh(context, name);
    }

    /**
     * Encapsulates the OpenGL ES instructions for drawing this shape.
     *
     * @param mvp_matrix - The Model View Project matrix in which to draw this shape.
     * @param program    - The Shading program which decided how the shape is going to be rendered.
     */
    void draw(int program, float[] mvp_matrix)
    {
        // Add program to OpenGL environment
        glUseProgram(program);
        {
            // 2. SET UNIFORMS

            // Set color for drawing the triangle
            glUniform4fv(glGetUniformLocation(program, "uniform_diffuse_color"), 1, m_diffuse_color, 0);
            // Apply the projection and view transformation
            glUniformMatrix4fv(glGetUniformLocation(program, "uniform_mvp"), 1, false, mvp_matrix, 0);

            // 3. DRAW
            glBindVertexArray ( m_vao[0] );
            {
                //glDrawElements(GL_TRIANGLES, m_indices_data.length, GL_UNSIGNED_SHORT, 0);
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

            if(CommandBlock.equals("#"))
                continue;

            if(CommandBlock.equals("v"))
            {
                Float3  vertex = new Float3(Float.parseFloat(Blocks[1]), Float.parseFloat(Blocks[2]), Float.parseFloat(Blocks[3]));
                m_vertices.add(vertex);
               // Log.d("VERTEX DATA", " " + vertex.x + ", " + vertex.y + ", " + vertex.z);
            }
            else if(CommandBlock.equals("vt"))
            {
                Float3 vertexTex = new Float3(Float.parseFloat(Blocks[1]), Float.parseFloat(Blocks[2]), 0.0f);
                m_uvs.add(vertexTex);
               // Log.d("TEXTURE DATA", " " + vertexTex.x + ", " + vertexTex.y + ", " + vertexTex.z);
            }
            else if(CommandBlock.equals("vn"))
            {
                Float3 vertexNorm = new Float3(Float.parseFloat(Blocks[1]), Float.parseFloat(Blocks[2]), Float.parseFloat(Blocks[3]));
                m_normals.add(vertexNorm);
               // Log.d("NORMAL DATA", " " + vertexNorm.x + ", " + vertexNorm.y + ", " + vertexNorm.z);
            }
            else if(CommandBlock.equals("f"))
            {
                Face3D face = new Face3D();
                m_faces.add(face);

                String[] faceParams;

                for(int i = 1; i < Blocks.length ; i++)
                {
                    String split_char = "/";
                    if(Blocks[i].contains("//"))
                        split_char = "//";

                    faceParams = Blocks[i].split(split_char);

                    face.vertices.add(Integer.parseInt(faceParams[0]) - 1);

                    if(faceParams.length == 2)
                    {
                        if(!m_normals.isEmpty())
                            face.normals.add(Integer.parseInt(faceParams[1]) - 1);
                    }
                    else if(faceParams.length == 3)
                    {
                        if(!m_uvs.isEmpty())
                            face.textures.add(Integer.parseInt(faceParams[1]) - 1);
                        if(!m_normals.isEmpty())
                            face.normals.add(Integer.parseInt(faceParams[2]) - 1);
                    }
                }
            }
        }

        fillInBuffers();

       // Log.d("OBJ OBJECT DATA", "V = " + m_vertices.size() + " VN = " + m_texture_uvs.size() + " VT = " + m_normals.size());
    }

    private void fillInBuffers()
    {
        m_vertices_data = new float[m_vertices.size() * 3];
        if(!m_normals.isEmpty())
            m_normals_data  = new float[m_normals.size() * 3];
        m_indices_data  = new short[m_faces.size() * 3];

        for(int i = 0; i < m_vertices.size(); i++)
        {
            m_vertices_data[i * 3    ] = m_vertices.get(i).x;
            m_vertices_data[i * 3 + 1] = m_vertices.get(i).y;
            m_vertices_data[i * 3 + 2] = m_vertices.get(i).z;

            if(!m_normals.isEmpty())
            {
                m_normals_data[i * 3    ] = m_normals.get(i).x;
                m_normals_data[i * 3 + 1] = m_normals.get(i).y;
                m_normals_data[i * 3 + 2] = m_normals.get(i).z;
            }

            if(!m_uvs.isEmpty())
            {
                m_uvs_data[i * 3    ] = m_uvs.get(i).x;
                m_uvs_data[i * 3 + 1] = m_uvs.get(i).y;
                m_uvs_data[i * 3 + 2] = m_uvs.get(i).z;
            }
        }

        for(int i = 0; i < m_faces.size(); i++)
        {
            Face3D face = m_faces.get(i);

            m_indices_data[i * 3]     = face.vertices.get(0).shortValue();
            m_indices_data[i * 3 + 1] = face.vertices.get(1).shortValue();
            m_indices_data[i * 3 + 2] = face.vertices.get(2).shortValue();
        }

        createBuffers();
        createVBOs();
        createVAO();
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
        glBindBuffer ( GL_ARRAY_BUFFER, m_vertices_vbo[0] );
        m_vertices_buffer.position ( 0 );
        glBufferData( GL_ARRAY_BUFFER, m_vertices_data.length * Float.BYTES, m_vertices_buffer, GL_STATIC_DRAW );
        glBindBuffer ( GL_ARRAY_BUFFER, 0 );

        if(!m_normals.isEmpty())
        {
            glGenBuffers(1, m_normals_vbo, 0);
            glBindBuffer(GL_ARRAY_BUFFER, m_normals_vbo[0]);
            m_normals_buffer.position(0);
            glBufferData(GL_ARRAY_BUFFER, m_normals_data.length * Float.BYTES, m_normals_buffer, GL_STATIC_DRAW);
            glBindBuffer(GL_ARRAY_BUFFER, 0);
        }

        if(!m_uvs.isEmpty())
        {
            glGenBuffers(1, m_uvs_vbo, 0);
            glBindBuffer(GL_ARRAY_BUFFER, m_uvs_vbo[0]);
            m_uvs_buffer.position(0);
            glBufferData(GL_ARRAY_BUFFER, m_uvs_data.length * Float.BYTES, m_uvs_buffer, GL_STATIC_DRAW);
            glBindBuffer(GL_ARRAY_BUFFER, 0);
        }

        glGenBuffers ( 1, m_indices_vbo, 0 );
        glBindBuffer ( GL_ELEMENT_ARRAY_BUFFER, m_indices_vbo[0] );
        m_indices_buffer.position ( 0 );
        glBufferData ( GL_ELEMENT_ARRAY_BUFFER, m_indices_data.length * Short.BYTES, m_indices_buffer, GL_STATIC_DRAW );
        glBindBuffer ( GL_ELEMENT_ARRAY_BUFFER, 0 );
    }

    private void createVAO()
    {
        // Generate VAO Id
        glGenVertexArrays ( 1, m_vao, 0 );

        // Bind the VAO and then setup the vertex attributes
        glBindVertexArray ( m_vao[0] );

        glBindBuffer ( GL_ARRAY_BUFFER, m_vertices_vbo[0] );
        glEnableVertexAttribArray ( 0 );
        glVertexAttribPointer ( 0, 3, GL_FLOAT, false, Float.BYTES * 3, 0 );

        if(!m_normals.isEmpty())
        {
            glBindBuffer(GL_ARRAY_BUFFER, m_normals_vbo[0]);
            glEnableVertexAttribArray(1);
            glVertexAttribPointer(1, 3, GL_FLOAT, false, Float.BYTES * 3, 0);
        }

        if(!m_uvs.isEmpty())
        {
            glBindBuffer(GL_ARRAY_BUFFER, m_uvs_vbo[0]);
            glEnableVertexAttribArray(2);
            glVertexAttribPointer(2, 2, GL_FLOAT, false, Float.BYTES * 2, 0);
        }

        glBindBuffer ( GL_ELEMENT_ARRAY_BUFFER  , m_indices_vbo[0] );

        // Reset to the default VAO
        glBindVertexArray ( 0 );
    }
}