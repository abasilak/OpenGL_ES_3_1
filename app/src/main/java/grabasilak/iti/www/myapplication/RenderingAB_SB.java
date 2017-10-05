package grabasilak.iti.www.myapplication;

import android.content.Context;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;

import static android.opengl.GLES10.GL_DEPTH_TEST;
import static android.opengl.GLES20.GL_CLAMP_TO_EDGE;
import static android.opengl.GLES20.GL_COLOR_ATTACHMENT0;
import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_CULL_FACE;
import static android.opengl.GLES20.GL_DEPTH_ATTACHMENT;
import static android.opengl.GLES20.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_COMPONENT;
import static android.opengl.GLES20.GL_DEPTH_COMPONENT16;
import static android.opengl.GLES20.GL_DYNAMIC_DRAW;
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
import static android.opengl.GLES20.glBindBuffer;
import static android.opengl.GLES20.glBindFramebuffer;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glBufferData;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glColorMask;
import static android.opengl.GLES20.glDepthMask;
import static android.opengl.GLES20.glDisable;
import static android.opengl.GLES20.glEnable;
import static android.opengl.GLES20.glFramebufferTexture2D;
import static android.opengl.GLES20.glGenBuffers;
import static android.opengl.GLES20.glGenFramebuffers;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glTexImage2D;
import static android.opengl.GLES20.glTexParameteri;
import static android.opengl.GLES30.GL_MAP_INVALIDATE_BUFFER_BIT;
import static android.opengl.GLES30.GL_MAP_READ_BIT;
import static android.opengl.GLES30.GL_MAP_UNSYNCHRONIZED_BIT;
import static android.opengl.GLES30.GL_MAP_WRITE_BIT;
import static android.opengl.GLES30.GL_R32UI;
import static android.opengl.GLES30.GL_SRGB8_ALPHA8;
import static android.opengl.GLES30.glBindBufferBase;
import static android.opengl.GLES30.glDrawBuffers;
import static android.opengl.GLES30.glMapBufferRange;
import static android.opengl.GLES30.glTexStorage2D;
import static android.opengl.GLES30.glUnmapBuffer;
import static android.opengl.GLES31.GL_READ_WRITE;
import static android.opengl.GLES31.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT;
import static android.opengl.GLES31.GL_SHADER_STORAGE_BARRIER_BIT;
import static android.opengl.GLES31.GL_SHADER_STORAGE_BUFFER;
import static android.opengl.GLES31.glBindImageTexture;
import static android.opengl.GLES31.glMemoryBarrier;

class RenderingAB_SB extends Rendering
{
    private int    m_total_fragments;
    private int    m_generated_fragments;

    private Shader m_shader_init;
    private Shader m_shader_blend;
    private Shader m_shader_head;
    private Shader m_shader_peel;
    private Shader m_shader_resolve;

    private ScreenQuad m_screen_quad_init;
    private ScreenQuad m_screen_quad_head;
    private ScreenQuad m_screen_quad_resolve;

    private boolean m_realloc_memory_enabled;

    private int [] m_fbo                = new int[1];
    private int [] m_texture_depth      = new int[1];
    private int [] m_texture_head       = new int[1];
    private int [] m_texture_counter    = new int[1];
    private int [] m_ssbo_frag_counter = new int[1];
    private int [] m_ssbo_peel          = new int[1];

    private int [] m_atomic_counter_data = new int[1];

    RenderingAB_SB(Context context, Viewport viewport)
    {
        super("AB_SB Rendering", viewport);

        m_total_passes      = 2;
        m_struct_size       = (2*Float.BYTES + 2*Integer.BYTES);
        m_total_fragments   = 500000;

        m_shader_init       = new Shader(context, context.getString(R.string.SHADER_AB_SB_INIT_NAME));
        m_shader_blend      = new Shader(context, context.getString(R.string.SHADER_AB_SB_BLEND_NAME));
        m_shader_head       = new Shader(context, context.getString(R.string.SHADER_AB_SB_HEAD_NAME));
        m_shader_peel       = new Shader(context, context.getString(R.string.SHADER_AB_SB_PEEL_NAME));
        m_shader_resolve    = new Shader(context, context.getString(R.string.SHADER_AB_SB_RESOLVE_NAME));

        m_screen_quad_init = new ScreenQuad(1);
        {
            m_screen_quad_init.setViewport          (viewport.m_width, viewport.m_height);
            m_screen_quad_init.addShader            (m_shader_init);
        }

        m_screen_quad_head = new ScreenQuad(1);
        {
            m_screen_quad_head.setViewport          (viewport.m_width, viewport.m_height);
            m_screen_quad_head.addShader            (m_shader_head);
        }

        m_screen_quad_resolve = new ScreenQuad(1);
        {
            m_screen_quad_resolve.setViewport       (viewport.m_width, viewport.m_height);
            m_screen_quad_resolve.addShader         (m_shader_resolve);
        }

        m_atomic_counter_data[0] = 0;
        m_realloc_memory_enabled = false;

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

        // Texture Head
        glGenTextures(1, m_texture_head, 0);
        glBindTexture(GL_TEXTURE_2D, m_texture_head[0]);
        {
            glTexStorage2D(GL_TEXTURE_2D, 1, GL_R32UI, m_viewport.m_width, m_viewport.m_height);
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

        // Atomic Counter
        glGenBuffers(1, m_ssbo_frag_counter, 0);
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, m_ssbo_frag_counter[0]);
        {
            glBufferData(GL_SHADER_STORAGE_BUFFER, Integer.BYTES, null, GL_DYNAMIC_DRAW);
        }
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);

        glGenBuffers(1, m_ssbo_peel, 0);

        // Framebuffer Object
        glGenFramebuffers(1, m_fbo, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, m_fbo[0]);
        {
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT , GL_TEXTURE_2D, m_texture_depth[0], 0);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, m_texture_color[0], 0);
        }
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        initSharedPool();

        RenderingSettings.checkFramebufferStatus();
        RenderingSettings.checkGlError(m_name + " - [createFBO]");

        return true;
    }

    private void     initSharedPool()
    {
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, m_ssbo_peel[0]);
        {
            glBufferData(GL_SHADER_STORAGE_BUFFER, m_total_fragments * m_struct_size, null, GL_DYNAMIC_DRAW);
        }
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
    }

    void     draw(RenderingSettings rendering_settings, TextManager text_manager, ArrayList<Mesh> meshes, ArrayList<Light> lights, Camera camera, int ubo_matrices)
    {
        rendering_settings.m_fps.start();
        {
            m_viewport.setViewport();
            glBindFramebuffer(GL_FRAMEBUFFER, m_fbo[0]);
            {
                glBindImageTexture(0, m_texture_counter[0], 0, false, 0, GL_READ_WRITE, GL_R32UI);
                glBindImageTexture(1, m_texture_head   [0], 0, false, 0, GL_READ_WRITE, GL_R32UI);
                glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 3, m_ssbo_frag_counter[0]);
                glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 4, m_ssbo_peel[0]);

                glColorMask(false, false, false, false);
                glDisable(GL_CULL_FACE);
                glDisable(GL_DEPTH_TEST);
                glDepthMask(false);

                while(true)
                {
                    // 0.a INIT Atomic Counter
                    {
                        glBindBuffer(GL_SHADER_STORAGE_BUFFER, m_ssbo_frag_counter[0]);
                        {
                            IntBuffer mappedBuffer = ((ByteBuffer) glMapBufferRange(
                                    GL_SHADER_STORAGE_BUFFER, 0, Integer.BYTES,
                                    GL_MAP_WRITE_BIT | GL_MAP_INVALIDATE_BUFFER_BIT | GL_MAP_UNSYNCHRONIZED_BIT)).order(ByteOrder.nativeOrder()).asIntBuffer();
                            mappedBuffer.put(m_atomic_counter_data).position(0);
                            glUnmapBuffer(GL_SHADER_STORAGE_BUFFER);
                        }
                        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
                    }
                    glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);

                    // 0.b INIT Counter & Head
                    {
                        m_screen_quad_init.draw();
                    }
                    glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);

                    // 1.a BLEND
                    {
                        // glBeginQuery(GL_SAMPLES_PASSED, m_occlusion_query[0]);
                        for (Mesh mesh : meshes)
                            mesh.drawSimple(m_shader_blend.getProgram(), camera);
                        //glEndQuery(GL_SAMPLES_PASSED);
                    }
                    glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT | GL_SHADER_STORAGE_BARRIER_BIT);

                    // 1.c Compute Head
                    {
                        m_screen_quad_head.draw();
                    }
                    glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT | GL_SHADER_STORAGE_BARRIER_BIT);

                    // 1.b COMPUTE TOTAL FRAGMENTS
                    if (m_realloc_memory_enabled)
                    {
                        glBindBuffer(GL_SHADER_STORAGE_BUFFER, m_ssbo_frag_counter[0]);
                        {
                            IntBuffer mappedBuffer = ((ByteBuffer) glMapBufferRange(
                                    GL_SHADER_STORAGE_BUFFER, 0, Integer.BYTES,
                                    GL_MAP_READ_BIT)).order(ByteOrder.nativeOrder()).asIntBuffer();

                            m_generated_fragments = mappedBuffer.get();
                            glUnmapBuffer(GL_SHADER_STORAGE_BUFFER);
                        }
                        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);

                        //glGetQueryObjectiv(m_occlusion_query[0], GL_QUERY_RESULT, m_occlusion_query_result,0);
                        //m_total_fragments = m_occlusion_query_result[0];

                        if (m_generated_fragments > m_total_fragments)
                        {
                            m_total_fragments = m_generated_fragments;
                            initSharedPool();
                        }
                        else
                            break;
                    }
                    else
                        break;
                }

                // 2.a PEEL
                {
                    for (Mesh mesh : meshes)
                        mesh.draw(m_shader_peel.getProgram(), camera, lights, ubo_matrices);
                }
                glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT | GL_SHADER_STORAGE_BARRIER_BIT);

                // 3. RESOLVE
                glColorMask(true, true, true, true);
                glEnable(GL_CULL_FACE);
                glEnable(GL_DEPTH_TEST);
                glDepthMask(true);

                glDrawBuffers(1, new int[]{GL_COLOR_ATTACHMENT0}, 0);
                glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
                {
                    m_screen_quad_resolve.draw();
                }

                glBindImageTexture(0, 0, 0, false, 0, GL_READ_WRITE, GL_R32UI);
                glBindImageTexture(1, 0, 0, false, 0, GL_READ_WRITE, GL_R32UI);
                glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 3, 0);
                glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 4, 0);
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
        return 0;
    }
    int      getFBO         ()
    {
        return m_fbo[0];
    }
    Viewport getViewport    () { return m_viewport;}
}
