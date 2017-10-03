package grabasilak.iti.www.myapplication;

import android.content.Context;

import java.util.ArrayList;

import static android.opengl.GLES10.GL_DEPTH_TEST;
import static android.opengl.GLES10.glDepthMask;
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
import static android.opengl.GLES20.glColorMask;
import static android.opengl.GLES20.glDisable;
import static android.opengl.GLES20.glEnable;
import static android.opengl.GLES20.glFramebufferTexture2D;
import static android.opengl.GLES20.glGenFramebuffers;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glTexImage2D;
import static android.opengl.GLES20.glTexParameteri;
import static android.opengl.GLES30.GL_R32F;
import static android.opengl.GLES30.GL_R32UI;
import static android.opengl.GLES30.GL_RGBA8;
import static android.opengl.GLES30.GL_SRGB8_ALPHA8;
import static android.opengl.GLES30.GL_TEXTURE_2D_ARRAY;
import static android.opengl.GLES30.glDrawBuffers;
import static android.opengl.GLES30.glTexStorage2D;
import static android.opengl.GLES30.glTexStorage3D;
import static android.opengl.GLES31.GL_READ_WRITE;
import static android.opengl.GLES31.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT;
import static android.opengl.GLES31.glBindImageTexture;
import static android.opengl.GLES31.glMemoryBarrierByRegion;

class RenderingAB_Array extends Rendering
{
    private int    m_max_layers;
    private Shader m_shader_init;
    private Shader m_shader_peel;
    private Shader m_shader_resolve;

    private ScreenQuad m_screen_quad_init;
    private ScreenQuad m_screen_quad_resolve;

    private int [] m_fbo                = new int[1];
    private int [] m_texture_peel_depth = new int[1];
    private int [] m_texture_peel_color = new int[1];
    private int [] m_texture_depth      = new int[1];
    private int [] m_texture_counter    = new int[1];

    RenderingAB_Array(Context context, Viewport viewport, int max_layers)
    {
        super("AB_ARRAY Rendering", viewport);

        m_total_passes      = 1;
        m_max_layers        = max_layers;
        m_shader_init       = new Shader(context, context.getString(R.string.SHADER_AB_ARRAY_INIT_NAME));
        m_shader_peel       = new Shader(context, context.getString(R.string.SHADER_AB_ARRAY_PEEL_NAME));
        m_shader_resolve    = new Shader(context, context.getString(R.string.SHADER_AB_ARRAY_RESOLVE_NAME));

        m_screen_quad_init = new ScreenQuad(1);
        {
            m_screen_quad_init.setViewport        (viewport.m_width, viewport.m_height);
            m_screen_quad_init.addShader          (m_shader_init);
        }

        m_screen_quad_resolve = new ScreenQuad(1);
        {
            m_screen_quad_resolve.setViewport        (viewport.m_width, viewport.m_height);
            m_screen_quad_resolve.addShader          (m_shader_resolve);
        }

        createFBO();
    }

    boolean  createFBO()
    {
        // Texture Color
        glGenTextures(1, m_texture_color, 0);
        glBindTexture(GL_TEXTURE_2D, m_texture_color[0]);
        {
            glTexImage2D(GL_TEXTURE_2D, 0, GL_SRGB8_ALPHA8, m_viewport.m_width, m_viewport.m_height, 0, GL_RGBA, GL_UNSIGNED_BYTE, null);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        }
        glBindTexture(GL_TEXTURE_2D, 0);

        // Texture Depth
        glGenTextures(1, m_texture_depth, 0);
        glBindTexture(GL_TEXTURE_2D, m_texture_depth[0]);
        {
            glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT16, m_viewport.m_width, m_viewport.m_height, 0, GL_DEPTH_COMPONENT, GL_UNSIGNED_INT, null);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        }
        glBindTexture(GL_TEXTURE_2D, 0);

        // Texture Counter
        glGenTextures(1, m_texture_counter, 0);
        glBindTexture(GL_TEXTURE_2D, m_texture_counter[0]);
        {
            glTexStorage2D(GL_TEXTURE_2D, 1, GL_R32UI, m_viewport.m_width, m_viewport.m_height);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        }
        glBindTexture(GL_TEXTURE_2D, 0);

        // Texture Peel
        glGenTextures(1, m_texture_peel_depth, 0);
        glBindTexture(GL_TEXTURE_2D_ARRAY, m_texture_peel_depth[0]);
        {
            glTexStorage3D(GL_TEXTURE_2D_ARRAY, 1, GL_R32F, m_viewport.m_width, m_viewport.m_height, m_max_layers);
            glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        }
        glBindTexture(GL_TEXTURE_2D_ARRAY, 0);

        glGenTextures(1, m_texture_peel_color, 0);
        glBindTexture(GL_TEXTURE_2D_ARRAY, m_texture_peel_color[0]);
        {
            glTexStorage3D(GL_TEXTURE_2D_ARRAY, 1, GL_RGBA8, m_viewport.m_width, m_viewport.m_height, m_max_layers);
            glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        }
        glBindTexture(GL_TEXTURE_2D_ARRAY, 0);

        // Framebuffer Object
        glGenFramebuffers(1, m_fbo, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, m_fbo[0]);
        {
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT , GL_TEXTURE_2D, m_texture_depth[0], 0);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, m_texture_color[0], 0);
        }
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        RenderingSettings.checkFramebufferStatus();
        RenderingSettings.checkGlError(m_name + " - [createFBO]");

        return true;
    }

    void     draw(RenderingSettings rendering_settings, TextManager text_manager, ArrayList<Mesh> meshes, ArrayList<Light> lights, Camera camera, int ubo_matrices)
    {
        rendering_settings.m_fps.start();
        {
            m_viewport.setViewport();
            glBindFramebuffer(GL_FRAMEBUFFER, m_fbo[0]);
            {
                glBindImageTexture(0, m_texture_counter     [0], 0, false, 0, GL_READ_WRITE, GL_R32UI);
                glBindImageTexture(1, m_texture_peel_depth  [0], 0, true , 0, GL_READ_WRITE, GL_R32F);
                glBindImageTexture(2, m_texture_peel_color  [0], 0, true , 0, GL_READ_WRITE, GL_RGBA8);

                glColorMask(false, false, false, false);
                glDisable(GL_DEPTH_TEST);
                glDepthMask(false);

                // 0. INIT
                {
                    m_screen_quad_init.draw();
                }
                glMemoryBarrierByRegion(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);

                // 1. PEEL
                {
                    for (Mesh mesh: meshes)
                        mesh.draw(m_shader_peel.getProgram(), camera, lights, ubo_matrices);
                }
                glMemoryBarrierByRegion(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);

                glColorMask(true, true, true, true);
                glEnable(GL_DEPTH_TEST);
                glDepthMask(true);

                // 2. RESOLVE
                glDrawBuffers(1, new int[]{GL_COLOR_ATTACHMENT0}, 0);
                glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
                {
                    m_screen_quad_resolve.draw();
                }

                glBindImageTexture(0, 0, 0, false, 0, GL_READ_WRITE, GL_R32UI);
                glBindImageTexture(1, 0, 0, true , 0, GL_READ_WRITE, GL_R32F);
                glBindImageTexture(2, 0, 0, true , 0, GL_READ_WRITE, GL_RGBA8);
            }
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
        }
        rendering_settings.m_fps.end();

        text_manager.addText(new TextObject(m_name + ": " + String.format("%.2f", rendering_settings.m_fps.getTime()),  text_manager.m_x,
                rendering_settings.m_viewport.m_height - text_manager.m_y*(text_manager.txtcollection.size()+1)
        ));

        RenderingSettings.checkGlError(m_name + " - [draw]");
    }

    int      getTextureDepth()
    {
        return m_texture_peel_depth[0];
    }
    int      getFBO         ()
    {
        return m_fbo[0];
    }
    Viewport getViewport    () { return m_viewport;}
}
