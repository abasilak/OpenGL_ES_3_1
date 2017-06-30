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
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static android.opengl.GLES31.*;

/**
 * A two-dimensional square for use as a drawn object in OpenGL ES 3.1.
 */
class Shader {

    private final String    m_name;
    private int             m_program;

    /**
     * Sets up the drawing object data for use in an OpenGL ES context.
     */
    Shader(Context context, String name) {

        m_name = name;
        loadProgram(context);
    }

    /**
     *
     * @param context Application context
     * @param fileName Name of shader file
     * @return A String object containing shader source, otherwise null
     */
    private static String readShader (Context context, String fileName )
    {
        String shaderSource = null;

        if ( fileName == null )
        {
            return null;
        }

        // Read the shader file from assets
        InputStream is;
        byte [] buffer;

        try
        {
            is =  context.getAssets().open ( "Shaders/" + fileName );

            // Create a buffer that has the same size as the InputStream
            buffer = new byte[is.available()];

            // Read the text file as a stream, into the buffer
            int k = is.read ( buffer );

            ByteArrayOutputStream os = new ByteArrayOutputStream();

            // Write this buffer to the output stream
            os.write ( buffer );

            // Close input and output streams
            os.close();
            is.close();

            shaderSource = os.toString();
        }
        catch ( IOException e)
        {
            e.printStackTrace();
        }

        return shaderSource;
    }

    private int loadProgram(Context context) {

        // Read vertex shader from assets
        String vertShaderSrc = readShader (context, m_name + ".vert" );
        if ( vertShaderSrc == null )
            return 0;
        int vertexShader    = loadShader(GL_VERTEX_SHADER  ,vertShaderSrc);
        if ( vertexShader == 0 )
            return 0;

        String fragShaderSrc = readShader (context, m_name + ".frag" );
        if ( fragShaderSrc == null )
            return 0;
        int fragmentShader  = loadShader(GL_FRAGMENT_SHADER,fragShaderSrc);
        if ( fragmentShader == 0 )
        {
            glDeleteShader ( vertexShader );
            return 0;
        }

        m_program = glCreateProgram();                  // create empty OpenGL Program
        if ( m_program == 0 )
        {
            glDeleteShader ( vertexShader );
            glDeleteShader ( fragmentShader );
            return 0;
        }

        glAttachShader  (m_program, vertexShader);      // add shader to program
        glAttachShader  (m_program, fragmentShader);    // add the fragment shader to program

        final int[] linkStatus = new int[1];
        glLinkProgram   (m_program);                    // create OpenGL program executables
        glGetProgramiv  (m_program, GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {

            Log.e(m_name, "Error linking shader: " + glGetProgramInfoLog ( m_program ));

            glDetachShader  (m_program, vertexShader);      // add the fragment shader to program
            glDetachShader  (m_program, fragmentShader);    // add the fragment shader to program
            glDeleteProgram ( m_program );

            throw new RuntimeException("Error linking shaders @" + m_name);
        }

        glDeleteShader  (vertexShader);
        glDeleteShader  (fragmentShader);

        return m_program;
    }

    /**
     * Utility method for compiling a OpenGL shader.
     *
     * <p><strong>Note:</strong> When developing shaders, use the checkGlError()
     * method to debug shader coding errors.</p>
     *
     * @param type - Vertex or fragment shader type.
     * @param shaderCode - String containing the shader code.
     * @return - Returns an id for the shader.
     */
    private int loadShader(int type, String shaderCode){

        // read shader...
        final int[] compileStatus = new int[1];

        int shader = glCreateShader(type);
        if ( shader == 0 )
        {
            return 0;
        }

        // add the source code to the shader and compile it
        glShaderSource  (shader, shaderCode);
        glCompileShader (shader);
        glGetShaderiv   (shader, GL_COMPILE_STATUS, compileStatus, 0);

        // If the compilation failed, delete the shader.
        if (compileStatus[0] == 0) {
            Log.e(m_name, "Error compiling shader: " + glGetShaderInfoLog(shader));
            glDeleteShader(shader);

            String s_type = (type == 0) ? ".vert" : ".frag";

            throw new RuntimeException("Error creating shader: @" + m_name + s_type);
        }

        return shader;
    }

    int getProgram(){return m_program;}
}