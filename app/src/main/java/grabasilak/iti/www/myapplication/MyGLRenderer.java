package grabasilak.iti.www.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLES31;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES31.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES31.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES31.GL_DYNAMIC_DRAW;
import static android.opengl.GLES31.GL_UNIFORM_BUFFER;
import static android.opengl.GLES31.glBindBuffer;
import static android.opengl.GLES31.glBindBufferBase;
import static android.opengl.GLES31.glBufferData;
import static android.opengl.GLES31.glClear;
import static android.opengl.GLES31.glClearColor;
import static android.opengl.GLES31.glEnable;
import static android.opengl.GLES31.glGenBuffers;
import static grabasilak.iti.www.myapplication.Util.m_theta;

class MyGLRenderer implements GLSurfaceView.Renderer {

            AABB            m_aabb;
            Camera          m_camera;
    private Light           m_light;

    private Context         m_context;

    private ArrayList<Mesh> m_meshes;

    private Shader          m_forward_rendering;
    private Shader          m_shadow_rendering;
    private Shader          m_text_rendering;

    private TextManager     m_text_manager;

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
        addMesh(m_context.getString(R.string.MESH_NAME), true);

        // Load Shaders
        m_forward_rendering = new Shader(m_context,  m_context.getString(R.string.SHADER_FORWARD_NAME));
        m_shadow_rendering  = new Shader(m_context,  m_context.getString(R.string.SHADER_SHADOW_NAME));
        m_text_rendering    = new Shader(m_context,  m_context.getString(R.string.SHADER_TEXT_RENDERING_NAME));

        createUBO();

        // Set the background frame color
        glClearColor(   m_rendering_settings.m_background_color[0],
                        m_rendering_settings.m_background_color[1],
                        m_rendering_settings.m_background_color[2],
                        m_rendering_settings.m_background_color[3]);

        GLES31.glClearDepthf(m_rendering_settings.m_depth);

        // Set Depth Test
        GLES31.glEnable(GLES31.GL_DEPTH_TEST);
        GLES31.glDepthFunc(GLES31.GL_LEQUAL);

        // Set Culling Test
        glEnable(GLES31.GL_CULL_FACE);
        GLES31.glCullFace(GLES31.GL_BACK);
        GLES31.glFrontFace(GLES31.GL_CCW);

        SetupText();

        RenderingSettings.checkGlError("onSurfaceCreated");
    }

    // DRAW FUNCTION
    public void onDrawFrame(GL10 unused)
    {
        // Animate Lights
       // m_light.animation();

        // Shadow Mapping
        if (m_light.m_is_animated)
        {
            m_light.m_is_animated = false;
            for (int i = 0; i < m_meshes.size(); i++)
                m_meshes.get(i).drawSceneToShadowFBO(m_shadow_rendering.getProgram(), m_light);
        }

        // Redraw background color
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        m_rendering_settings.m_viewport.setViewport();

        if(m_meshes.size()>0)
        {
            m_camera.computeWorldMatrix();
            m_camera.computeViewMatrix();

            // Render Meshes
            for (int i = 0; i < m_meshes.size(); i++)
                m_meshes.get(i).draw(m_forward_rendering.getProgram(), m_camera, m_light, m_ubo_matrices[0]);
        }

        // Get the amount of time the last frame took.
        m_rendering_settings.m_fps.end();
        m_rendering_settings.m_fps.compute();
        m_rendering_settings.m_fps.reset();

        // Add it to our manager
        m_text_manager.addText(new TextObject("FPS: " + String.format("%.2f", m_rendering_settings.m_fps.get()), 50, m_rendering_settings.m_viewport.m_height-50));
        m_text_manager.PrepareDraw();
        m_text_manager.Draw(m_text_rendering.getProgram(), m_rendering_settings.m_viewport.m_width, m_rendering_settings.m_viewport.m_height);
    }

    // RESIZE FUNCTION
    public void onSurfaceChanged(GL10 unused, int width, int height) {

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
            m_camera.m_eye[0]   = m_aabb.m_center[0] + dis;
            m_camera.m_eye[1]   = m_aabb.m_center[1] + dis;
            m_camera.m_eye[2]   = m_aabb.m_center[2] + dis;

            m_camera.m_target[0]= m_aabb.m_center[0];
            m_camera.m_target[1]= m_aabb.m_center[1];
            m_camera.m_target[2]= m_aabb.m_center[2];

            // Update Light
            m_light.m_camera.m_eye[0]     = m_aabb.m_center[0] + dis;
            m_light.m_camera.m_eye[1]     = m_aabb.m_center[1] + dis;
            m_light.m_camera.m_eye[2]     = m_aabb.m_center[2] + dis;

            m_light.m_camera.m_target[0]  = m_aabb.m_center[0];
            m_light.m_camera.m_target[1]  = m_aabb.m_center[1];
            m_light.m_camera.m_target[2]  = m_aabb.m_center[2];

            m_light.m_initial_position[0] = m_light.m_camera.m_eye[0];
            m_light.m_initial_position[1] = m_light.m_camera.m_eye[1];
            m_light.m_initial_position[2] = m_light.m_camera.m_eye[2];
            m_light.createUBO();
        }
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
        GLES20.glGenTextures(1, texturenames, 0);
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, texturenames[0]);
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MIN_FILTER, GLES31.GL_LINEAR);
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MAG_FILTER, GLES31.GL_LINEAR);
        GLUtils.texImage2D(GLES31.GL_TEXTURE_2D, 0, bmp, 0);
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, 0);
        bmp.recycle();

        // Tell our text manager to use index 1 of textures loaded
        m_text_manager.setTextureID(texturenames[0]);

        // Pass the uniform scale
        m_text_manager.setUniformscale(1);
    }
}