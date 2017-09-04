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
import java.util.Collections;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.GL_BACK;
import static android.opengl.GLES20.GL_CCW;
import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_CULL_FACE;
import static android.opengl.GLES20.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glCullFace;
import static android.opengl.GLES20.glFrontFace;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glTexParameteri;
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

            AABB                m_aabb;
            Camera              m_camera;
    private Light               m_light;
    private ArrayList<Mesh>     m_meshes;

    private Context             m_context;
    private TextManager         m_text_manager;

    private int                  m_current_rendering_method;
    private ArrayList<Rendering> m_rendering_methods;

    private RenderingForward    m_rendering_forward;
    private PeelingF2B          m_peeling_f2b;

    private Shader              m_shader_color_render;
    private Shader              m_shader_depth_render;

    private ScreenQuad		    m_screen_quad_output;
    private ScreenQuad		    m_screen_quad_debug;

    private RenderingSettings   m_rendering_settings;

    private int []              m_ubo_matrices = new int[1];

    MyGLRenderer(Context context, int width, int height)
    {
        m_context = context;
        m_rendering_settings = new RenderingSettings(width, height);
    }

    // INIT FUNCTION
    public void     onSurfaceCreated(GL10 unused, EGLConfig config) {

        m_aabb      = new AABB();
        m_camera    = new Camera();
        m_meshes    = new ArrayList<>();
        m_light     = new Light(m_context);

        m_rendering_methods = new ArrayList<>();

        addMesh(m_context.getString(R.string.MESH_NAME), true);

        m_rendering_forward   = new RenderingForward(m_context, m_rendering_settings.m_viewport);
        m_peeling_f2b         = new PeelingF2B      (m_context, m_rendering_settings.m_viewport);

        m_rendering_methods.add(m_rendering_forward);
        m_rendering_methods.add(m_peeling_f2b);
        m_current_rendering_method = 0;

        m_shader_color_render = new Shader(m_context, m_context.getString(R.string.SHADER_TEXTURE_COLOR_RENDERING_NAME));
        m_shader_depth_render = new Shader(m_context, m_context.getString(R.string.SHADER_TEXTURE_DEPTH_RENDERING_NAME));

        m_screen_quad_output = new ScreenQuad(1);
        {
            m_screen_quad_output.setViewport        (m_rendering_settings.m_viewport.m_width, m_rendering_settings.m_viewport.m_height);
            m_screen_quad_output.addShader          (m_shader_color_render);
            m_screen_quad_output.addUniformTextures (   new ArrayList<>(Collections.singletonList("uniform_texture_color")));
        }
        m_screen_quad_debug = new ScreenQuad(4);
        {
            m_screen_quad_debug.setViewport         (m_rendering_settings.m_viewport.m_width, m_rendering_settings.m_viewport.m_height);
            m_screen_quad_debug.addShader           (m_shader_depth_render);
            m_screen_quad_debug.addUniformTextures  (   new ArrayList<>(Collections.singletonList("uniform_texture_depth")));
            m_screen_quad_debug.addUniformFloats    (   new ArrayList<>(Arrays.asList(m_light.m_camera.m_near_field, m_light.m_camera.m_far_field)),
                                                        new ArrayList<>(Arrays.asList("uniform_z_near", "uniform_z_far")));
        }

        setupText();
        createUBO();

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

        RenderingSettings.checkGlError("onSurfaceCreated");
    }

    // DRAW FUNCTION
    public void     onDrawFrame(GL10 unused)
    {
        m_text_manager.clear();
        {
            m_light.animation();
            m_light.draw(m_rendering_settings, m_text_manager, m_meshes, null, null, 0);
        }
        {
            m_camera.computeWorldMatrix();
            m_camera.computeViewMatrix();
        }
        {
            m_rendering_methods.get(m_current_rendering_method).draw(m_rendering_settings, m_text_manager, m_meshes, m_light, m_camera, m_ubo_matrices[0]);
        }
        {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            m_screen_quad_output.setTextureList (new ArrayList<>(Collections.singletonList(m_rendering_methods.get(m_current_rendering_method).m_texture_color[0])));
            m_screen_quad_output.draw();
            m_screen_quad_debug.setTextureList  (new ArrayList<>(Collections.singletonList(m_light.m_shadow_mapping.getTextureDepth())));
            m_screen_quad_debug.draw();
        }
        m_text_manager.draw(m_rendering_settings.m_viewport);

        RenderingSettings.checkGlError("onDrawFrame");
    }

    // RESIZE FUNCTION
    public void     onSurfaceChanged(GL10 unused, int width, int height) {
        m_rendering_settings.m_viewport.setViewport(width, height);
        m_rendering_settings.m_viewport.setAspectRatio();

        m_screen_quad_output.setViewport(width, height);
        m_screen_quad_debug.setViewport(width, height);

        m_camera.computeProjectionMatrix(m_rendering_settings.m_viewport.getAspectRatio());
        m_light.m_camera.computeProjectionMatrix(m_light.m_viewport.getAspectRatio());

        RenderingSettings.checkGlError("onSurfaceChanged");
    }

    private void    addMesh(String fileName, boolean align_to_aabb)
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
            float padding = 1.1f;
            // Update Camera
            m_camera.m_eye[0]   = m_aabb.m_center[0] + dis*padding;
            m_camera.m_eye[1]   = m_aabb.m_center[1] + dis*padding;
            m_camera.m_eye[2]   = m_aabb.m_center[2] + dis*padding;

            m_camera.m_target[0]= m_aabb.m_center[0];
            m_camera.m_target[1]= m_aabb.m_center[1];
            m_camera.m_target[2]= m_aabb.m_center[2];

            // Update Light
            m_light.m_radius = m_aabb.m_radius/50f;

            m_light.m_camera.m_eye[0]     = m_aabb.m_center[0] + dis/4;
            m_light.m_camera.m_eye[1]     = m_aabb.m_center[1] + dis/2;
            m_light.m_camera.m_eye[2]     = m_aabb.m_center[2] + dis/4;

            m_light.m_camera.m_target[0]  = m_aabb.m_center[0];
            m_light.m_camera.m_target[1]  = m_aabb.m_center[1];
            m_light.m_camera.m_target[2]  = m_aabb.m_center[2];

            m_light.m_initial_position[0] = m_light.m_camera.m_eye[0];
            m_light.m_initial_position[1] = m_light.m_camera.m_eye[1];
            m_light.m_initial_position[2] = m_light.m_camera.m_eye[2];
            m_light.createUBO();
        }
    }

    private void    createUBO()
    {
        glGenBuffers(1, m_ubo_matrices, 0);
        glBindBuffer(GL_UNIFORM_BUFFER, m_ubo_matrices[0]);
        {
            glBufferData(GL_UNIFORM_BUFFER, 5 * 16 * Float.BYTES, null, GL_DYNAMIC_DRAW); // 5 Mat4x4 are included in this UBO
        }
        glBindBuffer(GL_UNIFORM_BUFFER, 0);
        glBindBufferBase(GL_UNIFORM_BUFFER, 0, m_ubo_matrices[0]);
    }

    private void    setupText()
    {
        // Create our text manager
        m_text_manager = new TextManager(m_context);

        InputStream istr;
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
        {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GLES31.GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GLES31.GL_LINEAR);
            GLUtils.texImage2D(GL_TEXTURE_2D, 0, bmp, 0);
            glBindTexture(GL_TEXTURE_2D, 0);
        }
        assert bmp != null;
        bmp.recycle();

        // Tell our text manager to use index 1 of textures loaded
        m_text_manager.setTextureID(texturenames[0]);

        // Pass the uniform scale
        m_text_manager.setUniformscale(1);
    }
}