package grabasilak.iti.www.myapplication;

import android.content.Context;

import java.util.ArrayList;

import static android.opengl.GLES20.GL_CLAMP_TO_EDGE;
import static android.opengl.GLES20.GL_COLOR_ATTACHMENT0;
import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_CULL_FACE;
import static android.opengl.GLES20.GL_DEPTH_ATTACHMENT;
import static android.opengl.GLES20.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_COMPONENT;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_FRAMEBUFFER;
import static android.opengl.GLES20.GL_NEAREST;
import static android.opengl.GLES20.GL_RGBA;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_S;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_T;
import static android.opengl.GLES20.GL_UNSIGNED_BYTE;
import static android.opengl.GLES20.glBindFramebuffer;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glDisable;
import static android.opengl.GLES20.glEnable;
import static android.opengl.GLES20.glFramebufferTexture2D;
import static android.opengl.GLES20.glGenFramebuffers;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glTexImage2D;
import static android.opengl.GLES20.glTexParameteri;
import static android.opengl.GLES30.GL_ANY_SAMPLES_PASSED;
import static android.opengl.GLES30.GL_DEPTH_COMPONENT32F;
import static android.opengl.GLES30.GL_QUERY_RESULT;
import static android.opengl.GLES30.GL_SRGB8_ALPHA8;
import static android.opengl.GLES30.glBeginQuery;
import static android.opengl.GLES30.glDrawBuffers;
import static android.opengl.GLES30.glEndQuery;
import static android.opengl.GLES30.glGetQueryObjectuiv;

class RenderingPeelingF2B extends Rendering
{
    private Shader  m_shader_peel;

    private int     m_currID, m_prevID;
    private int     m_passes;

    private int []	m_fbo           = new int[2];
    private int []	m_texture_depth = new int[2];

    RenderingPeelingF2B(Context context, Viewport viewport)
    {
        super("F2B Peeling", viewport);

        m_total_passes  = 1;
        m_shader_peel   = new Shader(context, context.getString(R.string.SHADER_F2B_PEELING_NAME));

        createFBO();
    }

    boolean  createFBO()
    {
        glGenFramebuffers(2, m_fbo, 0);
        glGenTextures(2, m_texture_depth, 0);
        glGenTextures(1, m_texture_color, 0);

        // Texture Color
        glBindTexture(GL_TEXTURE_2D, m_texture_color[0]);
        {
            glTexImage2D(GL_TEXTURE_2D, 0, GL_SRGB8_ALPHA8, m_viewport.m_width, m_viewport.m_height, 0, GL_RGBA, GL_UNSIGNED_BYTE, null);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        }
        glBindTexture(GL_TEXTURE_2D, 0);

        for (int i = 0; i < 2; i++)
        {
            // Texture Depth
            glBindTexture(GL_TEXTURE_2D, m_texture_depth[i]);
            {
                glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT32F, m_viewport.m_width, m_viewport.m_height, 0, GL_DEPTH_COMPONENT, GL_FLOAT, null);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            }
            glBindTexture(GL_TEXTURE_2D, 0);

            // Framebuffer Object
            glBindFramebuffer(GL_FRAMEBUFFER, m_fbo[i]);
            {
                glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT , GL_TEXTURE_2D, m_texture_depth[i], 0);
                glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, m_texture_color[0], 0);
            }
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
            RenderingSettings.checkFramebufferStatus();
        }
        RenderingSettings.checkGlError(m_name + " - [createFBO]");

        return true;
    }

    void     draw(RenderingSettings rendering_settings, TextManager text_manager, ArrayList<Mesh> meshes, ArrayList<Light> lights, Camera camera, int ubo_matrices)
    {
        rendering_settings.m_fps.start();
        {
            m_viewport.setViewport();

            glDisable(GL_CULL_FACE);

            m_passes = 0;
            m_occlusion_query_result[0] = 1;
            while (m_passes < m_total_passes && m_occlusion_query_result[0] > 0)
            {
                m_currID = m_passes % 2;
                m_prevID = 1 - m_currID;

                glBindFramebuffer(GL_FRAMEBUFFER, m_fbo[m_currID]);
                {
                    glDrawBuffers(1, new int[]{GL_COLOR_ATTACHMENT0}, 0);
                    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

                    glBeginQuery(GL_ANY_SAMPLES_PASSED, m_occlusion_query[0]);
                    {
                        for (Mesh mesh : meshes)
                            mesh.peel(m_shader_peel.getProgram(), camera, lights, ubo_matrices, m_texture_depth[m_prevID], m_viewport);
                    }
                    glEndQuery(GL_ANY_SAMPLES_PASSED);
                    glGetQueryObjectuiv(m_occlusion_query[0], GL_QUERY_RESULT, m_occlusion_query_result, 0);
                }
                glBindFramebuffer(GL_FRAMEBUFFER, 0);

                m_passes++;
            }

            glEnable(GL_CULL_FACE);
        }
        rendering_settings.m_fps.end();

        text_manager.addText(new TextObject(m_name + "("+ m_total_passes + "): " + String.format("%.2f", rendering_settings.m_fps.getTime()),  text_manager.m_x,
                rendering_settings.m_viewport.m_height - text_manager.m_y*(text_manager.txtcollection.size()+1)
        ));
    }

    int      getTextureDepth()
    {
        return m_texture_depth[m_currID];
    }
    int      getFBO         ()
    {
        return m_fbo[m_currID];
    }
    Viewport getViewport    () { return m_viewport;}
}
