package grabasilak.iti.www.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES31;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES10.glCullFace;
import static android.opengl.GLES20.GL_BACK;
import static android.opengl.GLES20.GL_CCW;
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
import static android.opengl.GLES20.glDepthMask;
import static android.opengl.GLES20.glFramebufferTexture2D;
import static android.opengl.GLES20.glFrontFace;
import static android.opengl.GLES20.glGenFramebuffers;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glTexImage2D;
import static android.opengl.GLES20.glTexParameteri;
import static android.opengl.GLES30.GL_DEPTH_COMPONENT32F;
import static android.opengl.GLES30.glDrawBuffers;
import static android.opengl.GLES31.GL_DEPTH_TEST;
import static android.opengl.GLES31.GL_DYNAMIC_DRAW;
import static android.opengl.GLES31.GL_LEQUAL;
import static android.opengl.GLES31.GL_UNIFORM_BUFFER;
import static android.opengl.GLES31.glBindBuffer;
import static android.opengl.GLES31.glBindBufferBase;
import static android.opengl.GLES31.glBufferData;
import static android.opengl.GLES31.glClearColor;
import static android.opengl.GLES31.glClearDepthf;
import static android.opengl.GLES31.glDepthFunc;
import static android.opengl.GLES31.glEnable;
import static android.opengl.GLES31.glGenBuffers;
import static grabasilak.iti.www.myapplication.Util.m_theta;

class MyGLRenderer implements GLSurfaceView.Renderer {

            AABB            m_aabb;
            Camera          m_camera;
    private Light           m_light;

    private Context         m_context;

    private Mesh            m_sphere;
    private ArrayList<Mesh> m_meshes;

    private Shader          m_simple_rendering;
    private Shader          m_forward_rendering;
    private Shader          m_shadow_rendering;
    private Shader          m_text_rendering;
    private Shader          m_texture_color_rendering;

    // Forward Rendering
    private int []		m_forward_fbo           = new int[1];
    private int []		m_forward_texture_depth = new int[1];
    private int []		m_forward_texture_color = new int[1];

    private TextManager     m_text_manager;

    private ScreenQuad		m_screen_quad_output;

    private int []          m_ubo_matrices = new int[1];

    private RenderingSettings m_rendering_settings;

    MyGLRenderer(Context context)
    {
        m_context = context;
        m_rendering_settings = new RenderingSettings();
    }

    // INIT FUNCTION
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {

        m_aabb      = new AABB();
        m_light     = new Light();

        m_camera    = new Camera();
        m_meshes    = new ArrayList<>();

        // Load Meshes
        m_sphere    = new Mesh(m_context, "sphere.obj");
        addMesh(m_context.getString(R.string.MESH_NAME), true);

        createFBO();
        createUBO();

        // Load Shaders
        m_simple_rendering  = new Shader(m_context,  m_context.getString(R.string.SHADER_SIMPLE_RENDERING_NAME));
        m_forward_rendering = new Shader(m_context,  m_context.getString(R.string.SHADER_FORWARD_RENDERING_NAME));
        m_shadow_rendering  = new Shader(m_context,  m_context.getString(R.string.SHADER_SHADOW_RENDERING_NAME));
        m_text_rendering    = new Shader(m_context,  m_context.getString(R.string.SHADER_TEXT_RENDERING_NAME));
        m_texture_color_rendering = new Shader(m_context, m_context.getString(R.string.SHADER_TEXTURE_COLOR_RENDERING_NAME));

        m_screen_quad_output = new ScreenQuad(1);
        m_screen_quad_output.setViewport    (m_rendering_settings.m_viewport.m_width, m_rendering_settings.m_viewport.m_height);
        m_screen_quad_output.addShader      (m_texture_color_rendering);
        m_screen_quad_output.addTextureList (new ArrayList<Integer>(Arrays.asList(m_forward_texture_color[0])),
                                             new ArrayList<String>(Arrays.asList("Final Image")));

        // Set the background frame color
        glClearColor(   m_rendering_settings.m_background_color[0],
                        m_rendering_settings.m_background_color[1],
                        m_rendering_settings.m_background_color[2],
                        m_rendering_settings.m_background_color[3]);

        glClearDepthf(m_rendering_settings.m_depth);

        // Set Depth Test
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);

        // Set Culling Test
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glFrontFace(GL_CCW);

        SetupText();

        RenderingSettings.checkGlError("onSurfaceCreated");
    }

    // DRAW FUNCTION
    public void onDrawFrame(GL10 unused)
    {
        // Animate Lights
        m_light.animation();

        // Shadow Mapping
        if (m_light.m_is_animated)
        {
            m_light.m_is_animated = false;
//            for (int i = 0; i < m_meshes.size(); i++)
  //              m_meshes.get(i).drawSceneToShadowFBO(m_shadow_rendering.getProgram(), m_light);
        }

        m_rendering_settings.m_viewport.setViewport();

        glBindFramebuffer(GL_FRAMEBUFFER, m_forward_fbo[0]);
        glDrawBuffers(1, new int[]{GL_COLOR_ATTACHMENT0}, 0);

        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        if(m_meshes.size()>0)
        {
            m_camera.computeWorldMatrix();
            m_camera.computeViewMatrix();

            // Render Meshes
            for (int i = 0; i < m_meshes.size(); i++)
                m_meshes.get(i).draw(m_forward_rendering.getProgram(), m_camera, m_light, m_ubo_matrices[0]);

            if (m_light.m_is_rendered)
            {
                glDepthMask(false);
                {
                    m_sphere.setIdentity();
                    m_sphere.translate(m_light.m_camera.m_eye[0],m_light.m_camera.m_eye[1],m_light.m_camera.m_eye[2]);
                    m_sphere.scale(m_aabb.m_radius/10f,m_aabb.m_radius/10f,m_aabb.m_radius/10f);
                    m_sphere.drawSimple(m_simple_rendering.getProgram(), m_camera, m_light, m_ubo_matrices[0]);
                }
                glDepthMask(true);
            }
        }

        // Get the amount of time the last frame took.
        m_rendering_settings.m_fps.end();
        m_rendering_settings.m_fps.compute();
        m_rendering_settings.m_fps.reset();

        GLES31.glEnable(GLES31.GL_BLEND);
        GLES31.glBlendFunc(GLES31.GL_ONE, GLES31.GL_ONE_MINUS_SRC_ALPHA);
        {
            m_text_manager.addText(new TextObject("FPS: " + String.format("%.2f", m_rendering_settings.m_fps.get()), 50, m_rendering_settings.m_viewport.m_height - 50));
            m_text_manager.PrepareDraw();
            m_text_manager.Draw(m_text_rendering.getProgram(), m_rendering_settings.m_viewport.m_width, m_rendering_settings.m_viewport.m_height);
        }
        GLES31.glDisable(GLES31.GL_BLEND);

        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        // Final Quad Rendering
        {
           m_screen_quad_output.draw();
        }
    }

    // RESIZE FUNCTION
    public void onSurfaceChanged(GL10 unused, int width, int height) {

        m_screen_quad_output.m_viewport.setViewport(width, height);
        m_rendering_settings.m_viewport.setViewport(width, height);
        m_rendering_settings.m_viewport.setAspectRatio();

        m_camera.computeProjectionMatrix(m_rendering_settings.m_viewport.getAspectRatio());
    }

    private void addMesh(String fileName, boolean align_to_aabb)
    {
        // Load Mesh
        Mesh new_mesh = new Mesh(m_context, fileName);
        m_meshes.add(new_mesh);

        // Update Axis-Aligned Bounding Box since a new mesh has been added
        {
            m_aabb.m_min[0] = Math.min(m_aabb.m_min[0], new_mesh.m_aabb.m_min[0]);
            m_aabb.m_min[1] = Math.min(m_aabb.m_min[1], new_mesh.m_aabb.m_min[1]);
            m_aabb.m_min[2] = Math.min(m_aabb.m_min[2], new_mesh.m_aabb.m_min[2]);

            m_aabb.m_max[0] = Math.max(m_aabb.m_max[0], new_mesh.m_aabb.m_max[0]);
            m_aabb.m_max[1] = Math.max(m_aabb.m_max[1], new_mesh.m_aabb.m_max[1]);
            m_aabb.m_max[2] = Math.max(m_aabb.m_max[2], new_mesh.m_aabb.m_max[2]);
        }
        m_aabb.computeCenter();
        m_aabb.computeRadius();

        float dis = ((Math.max(Math.abs(m_aabb.m_max[0] - m_aabb.m_min[0]), Math.abs(m_aabb.m_max[2] - m_aabb.m_min[2]))) / m_theta) + 0.001f;

        if(align_to_aabb)
        {
            // Update Camera
            m_camera.m_eye[0]   = m_aabb.m_center[0] + dis*2;
            m_camera.m_eye[1]   = m_aabb.m_center[1] + dis*2;
            m_camera.m_eye[2]   = m_aabb.m_center[2] + dis*2;

            m_camera.m_target[0]= m_aabb.m_center[0];
            m_camera.m_target[1]= m_aabb.m_center[1];
            m_camera.m_target[2]= m_aabb.m_center[2];

            // Update Light
            m_light.m_camera.m_eye[0]     = m_aabb.m_center[0] + dis/4f;
            m_light.m_camera.m_eye[1]     = m_aabb.m_center[1] + dis/4f;
            m_light.m_camera.m_eye[2]     = m_aabb.m_center[2] + dis/4f;

            m_light.m_camera.m_target[0]  = m_aabb.m_center[0];
            m_light.m_camera.m_target[1]  = m_aabb.m_center[1];
            m_light.m_camera.m_target[2]  = m_aabb.m_center[2];

            m_light.m_initial_position[0] = m_light.m_camera.m_eye[0];
            m_light.m_initial_position[1] = m_light.m_camera.m_eye[1];
            m_light.m_initial_position[2] = m_light.m_camera.m_eye[2];
            m_light.createUBO();
        }
    }

    boolean createFBO()
    {
        // Texture Depth
        glGenTextures(1, m_forward_texture_depth, 0);
        glBindTexture(GL_TEXTURE_2D, m_forward_texture_depth[0]);
        {
            glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT32F, m_rendering_settings.m_viewport.m_width, m_rendering_settings.m_viewport.m_height, 0, GL_DEPTH_COMPONENT, GL_FLOAT, null);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        }
        glBindTexture(GL_TEXTURE_2D, 0);

        glGenTextures(1, m_forward_texture_color, 0);
        glBindTexture(GL_TEXTURE_2D, m_forward_texture_color[0]);
        {
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, m_rendering_settings.m_viewport.m_width, m_rendering_settings.m_viewport.m_height, 0, GL_RGBA, GL_UNSIGNED_BYTE, null);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        }
        glBindTexture(GL_TEXTURE_2D, 0);

        // FBO
        glGenFramebuffers(1, m_forward_fbo, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, m_forward_fbo[0]);
        {
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, m_forward_texture_depth[0], 0);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, m_forward_texture_color[0], 0);
        }
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        RenderingSettings.checkFramebufferStatus();

        return true;
    }

    private void createUBO()
    {
        glGenBuffers(1, m_ubo_matrices, 0);
        glBindBuffer(GL_UNIFORM_BUFFER, m_ubo_matrices[0]);
        {
            glBufferData(GL_UNIFORM_BUFFER, 5 * 16 * Float.BYTES, null, GL_DYNAMIC_DRAW); // 5 Mat4x4 are included in this UBO
        }
        glBindBuffer(GL_UNIFORM_BUFFER, 0);
        glBindBufferBase(GL_UNIFORM_BUFFER, 0, m_ubo_matrices[0]);
    }

    public void SetupText()
    {
        // Create our text manager
        m_text_manager = new TextManager();

        InputStream istr = null;
        Bitmap bmp = null;
        try {
            istr = m_context.getAssets().open("Font/font.png");
            bmp = BitmapFactory.decodeStream(istr);
        }
        catch (IOException e) {
            // handle exception
            Log.d("LOADING FILE", "FILE LOADED UNSUCCESSFULLY !");
        }

        int[] texturenames = new int[1];
        glGenTextures(1, texturenames, 0);
        glBindTexture(GL_TEXTURE_2D, texturenames[0]);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GLES31.GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GLES31.GL_LINEAR);
        GLUtils.texImage2D(GL_TEXTURE_2D, 0, bmp, 0);
        glBindTexture(GL_TEXTURE_2D, 0);
        bmp.recycle();

        // Tell our text manager to use index 1 of textures loaded
        m_text_manager.setTextureID(texturenames[0]);

        // Pass the uniform scale
        m_text_manager.setUniformscale(1);
    }
}