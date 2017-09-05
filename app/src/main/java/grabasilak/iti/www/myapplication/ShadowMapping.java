package grabasilak.iti.www.myapplication;

import android.content.Context;

import java.util.ArrayList;

import static android.opengl.GLES20.GL_CLAMP_TO_EDGE;
import static android.opengl.GLES20.GL_DEPTH_ATTACHMENT;
import static android.opengl.GLES20.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_COMPONENT;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_FRAMEBUFFER;
import static android.opengl.GLES20.GL_NEAREST;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_S;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_T;
import static android.opengl.GLES20.glBindFramebuffer;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glColorMask;
import static android.opengl.GLES20.glFramebufferTexture2D;
import static android.opengl.GLES20.glGenFramebuffers;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glTexImage2D;
import static android.opengl.GLES20.glTexParameteri;
import static android.opengl.GLES30.GL_DEPTH_COMPONENT32F;

class ShadowMapping extends Rendering
{
    private Shader  m_shader_shadow_mapping;

    private int []	m_fbo           = new int[1];
    private int []	m_texture_depth = new int[1];

    ShadowMapping(Context context, Viewport viewport)
    {
        super("Shadow Mapping");

        m_shader_shadow_mapping = new Shader(context, context.getString(R.string.SHADER_SHADOW_MAPPING_NAME));

        createFBO(viewport);
    }

    boolean createFBO(Viewport viewport)
    {
        // Create Shadow Texture
        glGenTextures(1, m_texture_depth, 0);
        glBindTexture(GL_TEXTURE_2D, m_texture_depth[0]);
        {
            glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT32F, viewport.m_width, viewport.m_height, 0, GL_DEPTH_COMPONENT, GL_FLOAT, null);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        }
        glBindTexture(GL_TEXTURE_2D, 0);

        // Create Shadow Framebuffer
        glGenFramebuffers(1, m_fbo, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, m_fbo[0]);
        {
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, m_texture_depth[0], 0);
        }
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        RenderingSettings.checkFramebufferStatus();

        return true;
    }

    void draw(RenderingSettings rendering_settings, TextManager text_manager, ArrayList<Mesh> meshes, Light light, Camera camera, int ubo_matrices)
    {
        rendering_settings.m_fps.start();
        {
            glBindFramebuffer(GL_FRAMEBUFFER, m_fbo[0]);
            {
                glClear(GL_DEPTH_BUFFER_BIT);
                glColorMask(false, false, false, false);
                {
                    for (int i = 0; i < meshes.size(); i++)
                        meshes.get(i).drawShadowMapping(m_shader_shadow_mapping.getProgram(), camera, light);
                }
                glColorMask(true, true, true, true);
            }
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
        }
        rendering_settings.m_fps.end();

        text_manager.addText(new TextObject(m_name + ": " + String.format("%.2f", rendering_settings.m_fps.getTime()), 50, rendering_settings.m_viewport.m_height - 50));
    }

    int getTextureDepth()
    {
        return m_texture_depth[0];
    }
}
