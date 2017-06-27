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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import static android.opengl.GLES31.GL_ARRAY_BUFFER;
import static android.opengl.GLES31.GL_ELEMENT_ARRAY_BUFFER;
import static android.opengl.GLES31.GL_FLOAT;
import static android.opengl.GLES31.GL_STATIC_DRAW;
import static android.opengl.GLES31.glBindBuffer;
import static android.opengl.GLES31.glBufferData;
import static android.opengl.GLES31.glEnableVertexAttribArray;
import static android.opengl.GLES31.glGenBuffers;
import static android.opengl.GLES31.glVertexAttribPointer;
import static android.opengl.GLES31.glBindVertexArray;
import static android.opengl.GLES31.glGenVertexArrays;
import static android.opengl.GLES31.GL_TRIANGLES;
import static android.opengl.GLES31.GL_UNSIGNED_SHORT;
import static android.opengl.GLES31.glDrawElements;
import static android.opengl.GLES31.glGetUniformLocation;
import static android.opengl.GLES31.glUniform4fv;
import static android.opengl.GLES31.glUniformMatrix4fv;
import static android.opengl.GLES31.glUseProgram;

/**
 * A two-dimensional square for use as a drawn object in OpenGL ES 3.1.
 */
public class Mesh {

    private final int []        m_vao           = new int[1];
    private final int []        m_vertices_vbo  = new int[1];
    private final int []        m_indices_vbo   = new int[1];

    private final FloatBuffer   m_vertex_buffer;
    private final ShortBuffer   m_indices_buffer;

    private final float m_vertices_data[] = {
            -0.2f,  0.2f, 0.0f,   // top left
            -0.2f, -0.2f, 0.0f,   // bottom left
            0.2f, -0.2f, 0.0f,   // bottom right
            0.2f,  0.2f, 0.0f }; // top right

    private final short m_indices_data[] = {
            0, 1, 2, 0, 2, 3 }; // order to draw vertices

    private final float diffuse_color[] = { 0.2f, 0.709803922f, 0.898039216f, 1.0f };

    /**
     * Sets up the drawing object data for use in an OpenGL ES context.
     */
    public Mesh() {

        // initialize vertex byte buffer for shape coordinates
        m_vertex_buffer = ByteBuffer.allocateDirect ( m_vertices_data.length * 4 ).order ( ByteOrder.nativeOrder() ).asFloatBuffer();
        m_vertex_buffer.put (m_vertices_data);
        m_vertex_buffer.position ( 0 );

        // initialize indices byte buffer for shape coordinates
        m_indices_buffer = ByteBuffer.allocateDirect ( m_indices_data.length * 2 ).order ( ByteOrder.nativeOrder() ).asShortBuffer();
        m_indices_buffer.put (m_indices_data);
        m_indices_buffer.position ( 0 );

        // Generate VBO Ids and load the VBOs with data
        glGenBuffers ( 1, m_vertices_vbo, 0 );

        glBindBuffer ( GL_ARRAY_BUFFER, m_vertices_vbo[0] );
        m_vertex_buffer.position ( 0 );
        glBufferData( GL_ARRAY_BUFFER, m_vertices_data.length * 4, m_vertex_buffer, GL_STATIC_DRAW );
        glBindBuffer ( GL_ARRAY_BUFFER, 0 );

        glGenBuffers ( 1, m_indices_vbo, 0 );
        glBindBuffer ( GL_ELEMENT_ARRAY_BUFFER, m_indices_vbo[0] );
        m_indices_buffer.position ( 0 );
        glBufferData ( GL_ELEMENT_ARRAY_BUFFER, 2 * m_indices_data.length, m_indices_buffer, GL_STATIC_DRAW );
        glBindBuffer ( GL_ELEMENT_ARRAY_BUFFER, 0 );

        // Generate VAO Id
        glGenVertexArrays ( 1, m_vao, 0 );

        // Bind the VAO and then setup the vertex attributes
        glBindVertexArray ( m_vao[0] );

        glBindBuffer ( GL_ARRAY_BUFFER          , m_vertices_vbo[0] );
        glEnableVertexAttribArray ( 0 );
        glVertexAttribPointer ( 0, 3, GL_FLOAT, false, 4 * 3, 0 );

        glBindBuffer ( GL_ELEMENT_ARRAY_BUFFER  , m_indices_vbo[0] );

        // Reset to the default VAO
        glBindVertexArray ( 0 );
    }

    /**
     * Encapsulates the OpenGL ES instructions for drawing this shape.
     *
     * @param mvp_matrix - The Model View Project matrix in which to draw
     * this shape.
     */
    public void draw(int program, float[] mvp_matrix)
    {
        // Add program to OpenGL environment
        glUseProgram(program);
        {
            // 2. SET UNIFORMS

            // Set color for drawing the triangle
            glUniform4fv(glGetUniformLocation(program, "diffuse_color"), 1, diffuse_color, 0);
            // Apply the projection and view transformation
            glUniformMatrix4fv(glGetUniformLocation(program, "uMVPMatrix"), 1, false, mvp_matrix, 0);

            // 3. DRAW
            glBindVertexArray ( m_vao[0] );
            {
                glDrawElements(GL_TRIANGLES, m_indices_data.length, GL_UNSIGNED_SHORT, 0);
            }
            glBindVertexArray ( 0 );
        }
        glUseProgram(0);
    }
}