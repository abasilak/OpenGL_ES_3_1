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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static android.opengl.GLES31.GL_COMPILE_STATUS;
import static android.opengl.GLES31.GL_FRAGMENT_SHADER;
import static android.opengl.GLES31.GL_LINK_STATUS;
import static android.opengl.GLES31.GL_VERTEX_SHADER;
import static android.opengl.GLES31.glAttachShader;
import static android.opengl.GLES31.glCompileShader;
import static android.opengl.GLES31.glCreateProgram;
import static android.opengl.GLES31.glCreateShader;
import static android.opengl.GLES31.glDeleteProgram;
import static android.opengl.GLES31.glDeleteShader;
import static android.opengl.GLES31.glDetachShader;
import static android.opengl.GLES31.glGetProgramInfoLog;
import static android.opengl.GLES31.glGetProgramiv;
import static android.opengl.GLES31.glGetShaderInfoLog;
import static android.opengl.GLES31.glGetShaderiv;
import static android.opengl.GLES31.glLinkProgram;
import static android.opengl.GLES31.glShaderSource;

class Shader {

    private final String    m_name;
    private int             m_program;

    Shader(Context context, String name)
    {
        m_name = name;
        loadProgram(context);
    }

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

            String newLine = System.getProperty("line.separator");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder result = new StringBuilder();
            String line; boolean flag = false;
            while ((line = reader.readLine()) != null)
            {
                String[] arr = line.split("\\s+");
                if(arr.length == 0 )
                    continue;

                if(arr[0].equals("#include"))
                {
                    result.append(flag ? newLine: "").append(readShader(context, arr[1].substring(1,arr[1].length()-1)));
                }
                else
                    result.append(flag ? newLine: "").append(line);
                flag = true;
            }
            return result.toString();
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