package grabasilak.iti.www.myapplication;

import android.content.Context;

import java.util.ArrayList;

import static android.opengl.GLES20.GL_CLAMP_TO_EDGE;
import static android.opengl.GLES20.GL_COLOR_ATTACHMENT0;
import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_ATTACHMENT;
import static android.opengl.GLES20.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_COMPONENT;
import static android.opengl.GLES20.GL_DEPTH_COMPONENT16;
import static android.opengl.GLES20.GL_FRAMEBUFFER;
import static android.opengl.GLES20.GL_NEAREST;
import static android.opengl.GLES20.GL_RGBA;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_S;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_T;
import static android.opengl.GLES20.GL_UNSIGNED_BYTE;
import static android.opengl.GLES20.GL_UNSIGNED_INT;
import static android.opengl.GLES20.glBindFramebuffer;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glFramebufferTexture2D;
import static android.opengl.GLES20.glGenFramebuffers;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glTexImage2D;
import static android.opengl.GLES20.glTexParameteri;
import static android.opengl.GLES30.GL_SRGB8_ALPHA8;
import static android.opengl.GLES30.glDrawBuffers;
import static android.opengl.GLES30.glInvalidateFramebuffer;

class ForwardRendering {

    private Shader  m_render_shader;

    private int []	m_fbo           = new int[1];
    private int []	m_texture_depth = new int[1];
            int []	m_texture_color = new int[1];

    ForwardRendering(Context context, RenderingSettings rendering_settings)
    {
        m_render_shader = new Shader(context, context.getString(R.string.SHADER_FORWARD_RENDERING_NAME));

        createFBO(rendering_settings);
    }

    private boolean createFBO(RenderingSettings rendering_settings)
    {
        // Texture Depth
        glGenTextures(1, m_texture_depth, 0);
        glBindTexture(GL_TEXTURE_2D, m_texture_depth[0]);
        {
            glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT16, rendering_settings.m_viewport.m_width, rendering_settings.m_viewport.m_height, 0, GL_DEPTH_COMPONENT, GL_UNSIGNED_INT, null);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        }
        glBindTexture(GL_TEXTURE_2D, 0);

        // Texture Color
        glGenTextures(1, m_texture_color, 0);
        glBindTexture(GL_TEXTURE_2D, m_texture_color[0]);
        {
            glTexImage2D(GL_TEXTURE_2D, 0, GL_SRGB8_ALPHA8, rendering_settings.m_viewport.m_width, rendering_settings.m_viewport.m_height, 0, GL_RGBA, GL_UNSIGNED_BYTE, null);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        }
        glBindTexture(GL_TEXTURE_2D, 0);

        // Framebuffer Object
        glGenFramebuffers(1, m_fbo, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, m_fbo[0]);
        {
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, m_texture_depth[0], 0);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, m_texture_color[0], 0);
        }
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        RenderingSettings.checkFramebufferStatus();

        return true;
    }

    void draw(RenderingSettings rendering_settings, TextManager text_manager, ArrayList<Mesh> meshes, Light light, Camera camera, int ubo_matrices)
    {
        rendering_settings.m_viewport.setViewport();

        glBindFramebuffer(GL_FRAMEBUFFER, m_fbo[0]);
        {
            glDrawBuffers(1, new int[]{GL_COLOR_ATTACHMENT0}, 0);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            rendering_settings.m_fps.start();
            {
                for (int i = 0; i < meshes.size(); i++)
                    meshes.get(i).draw(m_render_shader.getProgram(), camera, light, ubo_matrices);
            }
            rendering_settings.m_fps.end();

            light.render(camera);

            text_manager.addText(new TextObject("Forward: " + String.format("%.2f", rendering_settings.m_fps.getTime()), 50, rendering_settings.m_viewport.m_height - 100));
            text_manager.Draw(rendering_settings.m_viewport.m_width, rendering_settings.m_viewport.m_height);
        }
        glInvalidateFramebuffer(GL_FRAMEBUFFER, 1, new int[]{GL_DEPTH_ATTACHMENT}, 0 );
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

}
