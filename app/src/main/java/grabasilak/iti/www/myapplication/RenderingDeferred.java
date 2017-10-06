package grabasilak.iti.www.myapplication;

import android.content.Context;

import java.util.ArrayList;
import java.util.Arrays;

import static android.opengl.GLES20.GL_CLAMP_TO_EDGE;
import static android.opengl.GLES20.GL_COLOR_ATTACHMENT0;
import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_ATTACHMENT;
import static android.opengl.GLES20.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_COMPONENT;
import static android.opengl.GLES20.GL_DEPTH_COMPONENT16;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_FRAMEBUFFER;
import static android.opengl.GLES20.GL_NEAREST;
import static android.opengl.GLES20.GL_RGB;
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
import static android.opengl.GLES20.glDepthMask;
import static android.opengl.GLES20.glFramebufferTexture2D;
import static android.opengl.GLES20.glGenFramebuffers;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glTexImage2D;
import static android.opengl.GLES20.glTexParameteri;
import static android.opengl.GLES30.GL_COLOR_ATTACHMENT1;
import static android.opengl.GLES30.GL_COLOR_ATTACHMENT2;
import static android.opengl.GLES30.GL_COLOR_ATTACHMENT3;
import static android.opengl.GLES30.GL_RGB16F;
import static android.opengl.GLES30.GL_RGBA16F;
import static android.opengl.GLES30.GL_RGBA32UI;
import static android.opengl.GLES30.GL_RGBA_INTEGER;
import static android.opengl.GLES30.GL_SRGB8_ALPHA8;
import static android.opengl.GLES30.glDrawBuffers;
import static android.opengl.GLES30.glInvalidateFramebuffer;

class RenderingDeferred extends Rendering
{
    private Shader  m_shader_gbuffer;
    private Shader  m_shader_lighting;

    private ScreenQuad m_screen_quad_lighting;

    private int []	m_fbo           = new int[1];
    private int []	m_fbo_deferred  = new int[1];
    private int []	m_texture_depth = new int[1];

    private int []	m_texture_gb_depth   = new int[1];
    private int []	m_texture_gb_pos_wcs = new int[1];
    private int []	m_texture_gb_pos_lcs = new int[1];
    private int []	m_texture_gb_normal  = new int[1];
    private int [] m_texture_gb_colors = new int[1];
    private int []	m_texture_gb_specular= new int[1];
    private int []	m_texture_gb_emission= new int[1];

    RenderingDeferred(Context context, Viewport viewport, ArrayList<Light> lights, Camera camera)
    {
        super("Deferred Rendering", viewport);

        m_shader_gbuffer  = new Shader(context, context.getString(R.string.SHADER_DEFERRED_GBUFFER_NAME));
        m_shader_lighting = new Shader(context, context.getString(R.string.SHADER_DEFERRED_LIGHTING_NAME));

        m_screen_quad_lighting = new ScreenQuad(1);
        {
            m_screen_quad_lighting.setViewport          (viewport.m_width, viewport.m_height);
            m_screen_quad_lighting.addShader            (m_shader_lighting);
        }

        createFBO();

        m_screen_quad_lighting.setTextureList       (new ArrayList<>(Arrays.asList(
                m_texture_gb_pos_wcs[0],
                m_texture_gb_pos_lcs[0],
                m_texture_gb_normal[0],
                m_texture_gb_colors[0],
                lights.get(0).m_shadow_mapping.getTextureDepth()
        )));

        ArrayList uniforms_deferred = new ArrayList< float[] >();
        uniforms_deferred.add(new float[] {camera.m_eye[0], camera.m_eye[1], camera.m_eye[2]});
        m_screen_quad_lighting.addUniform( uniforms_deferred, new ArrayList<>(Arrays.asList("uniform_camera_position_wcs")));

        //m_screen_quad_lighting.addUniform1f(   new ArrayList<>(Arrays.asList(m_camera.m_near_field)),
          //      new ArrayList<>(Arrays.asList("uniform_camera_position_wcs")));
        //glUniform3f(glGetUniformLocation(program, "uniform_camera_position_wcs"), camera.m_eye[0], camera.m_eye[1], camera.m_eye[2]);
    }

    boolean createFBO()
    {
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

        // Texture Color
        glGenTextures(1, m_texture_color, 0);
        glBindTexture(GL_TEXTURE_2D, m_texture_color[0]);
        {
            glTexImage2D(GL_TEXTURE_2D, 0, GL_SRGB8_ALPHA8    , m_viewport.m_width, m_viewport.m_height, 0, GL_RGBA, GL_UNSIGNED_BYTE, null);
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
        RenderingSettings.checkGlError(m_name + " - [createFBO]");

        // Texture Depth
        glGenTextures(1, m_texture_gb_depth, 0);
        glBindTexture(GL_TEXTURE_2D, m_texture_gb_depth[0]);
        {
            glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT16, m_viewport.m_width, m_viewport.m_height, 0, GL_DEPTH_COMPONENT, GL_UNSIGNED_INT, null);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        }
        glBindTexture(GL_TEXTURE_2D, 0);

        // Texture Pos WCS
        glGenTextures(1, m_texture_gb_pos_wcs, 0);
        glBindTexture(GL_TEXTURE_2D, m_texture_gb_pos_wcs[0]);
        {
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB16F            ,  m_viewport.m_width, m_viewport.m_height, 0, GL_RGB, GL_FLOAT, null);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        }
        glBindTexture(GL_TEXTURE_2D, 0);

        // Texture Pos LCS
        glGenTextures(1, m_texture_gb_pos_lcs, 0);
        glBindTexture(GL_TEXTURE_2D, m_texture_gb_pos_lcs[0]);
        {
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA16F            ,  m_viewport.m_width, m_viewport.m_height, 0, GL_RGBA, GL_FLOAT, null);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        }
        glBindTexture(GL_TEXTURE_2D, 0);

        // Texture Normal
        glGenTextures(1, m_texture_gb_normal, 0);
        glBindTexture(GL_TEXTURE_2D, m_texture_gb_normal[0]);
        {
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB16F            ,  m_viewport.m_width, m_viewport.m_height, 0, GL_RGB, GL_FLOAT, null);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        }
        glBindTexture(GL_TEXTURE_2D, 0);

        // Texture Diffuse
        glGenTextures(1, m_texture_gb_colors, 0);
        glBindTexture(GL_TEXTURE_2D, m_texture_gb_colors[0]);
        {
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA32UI              ,  m_viewport.m_width, m_viewport.m_height, 0, GL_RGBA_INTEGER, GL_UNSIGNED_INT, null);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        }
        glBindTexture(GL_TEXTURE_2D, 0);

        // Framebuffer Deferred Object
        glGenFramebuffers(1, m_fbo_deferred, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, m_fbo_deferred[0]);
        {
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT , GL_TEXTURE_2D, m_texture_gb_depth   [0], 0);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, m_texture_gb_pos_wcs [0], 0);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT1, GL_TEXTURE_2D, m_texture_gb_pos_lcs [0], 0);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT2, GL_TEXTURE_2D, m_texture_gb_normal  [0], 0);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT3, GL_TEXTURE_2D, m_texture_gb_colors  [0], 0);
        }
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        RenderingSettings.checkFramebufferStatus();
        RenderingSettings.checkGlError(m_name + " - [createFBODeferred]");

        return true;
    }

    void draw(RenderingSettings rendering_settings, TextManager text_manager, ArrayList<Mesh> meshes, ArrayList<Light> lights, Camera camera, int ubo_matrices)
    {
        rendering_settings.m_fps.start();
        {
            m_viewport.setViewport();

            glBindFramebuffer(GL_FRAMEBUFFER, m_fbo_deferred[0]);
            {
                glDrawBuffers(4, new int[]{GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT1, GL_COLOR_ATTACHMENT2, GL_COLOR_ATTACHMENT3}, 0);
                glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
                glDepthMask(false);
                {
                    for (Mesh mesh: meshes)
                        mesh.draw(m_shader_gbuffer.getProgram(), camera, lights, ubo_matrices);
                }
                glDepthMask(true);
            }
            glInvalidateFramebuffer(GL_FRAMEBUFFER, 1, new int[]{GL_DEPTH_ATTACHMENT}, 0);
            glBindFramebuffer(GL_FRAMEBUFFER, 0);

            glBindFramebuffer(GL_FRAMEBUFFER, m_fbo[0]);
            {
                glDrawBuffers(1, new int[]{GL_COLOR_ATTACHMENT0}, 0);
                glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
                {
                    m_screen_quad_lighting.draw();
                }
            }
            glInvalidateFramebuffer(GL_FRAMEBUFFER, 1, new int[]{GL_DEPTH_ATTACHMENT}, 0);
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
        }
        rendering_settings.m_fps.end();

        text_manager.addText(new TextObject(m_name + ": " + String.format("%.2f", rendering_settings.m_fps.getTime()),  text_manager.m_x,
                rendering_settings.m_viewport.m_height - text_manager.m_y*(text_manager.txtcollection.size()+1)
        ));
    }

    int getTextureDepth ()
    {
        return m_texture_depth[0];
    }
    int getFBO          ()
    {
        return m_fbo[0];
    }
    Viewport getViewport()
    {
        return m_viewport;
    }
}
