package grabasilak.iti.www.myapplication;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.renderscript.Float3;

import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES31.*;
import static grabasilak.iti.www.myapplication.Util.m_theta;

class MyGLRenderer implements GLSurfaceView.Renderer {

            AABB            m_aabb;
            Camera          m_camera;
    private Light           m_light;

    private Context         m_context;

    private ArrayList<Mesh> m_meshes;

    private Shader          m_shader;

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
        m_shader = new Shader(m_context,  m_context.getString(R.string.SHADING_NAME));

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

        m_rendering_settings.checkGlError("onSurfaceCreated");
    }

    // DRAW FUNCTION
    public void onDrawFrame(GL10 unused)
    {
        // Redraw background color
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // Animate Lights
        m_light.animation();

        // Shadow Mapping
        if (m_light.m_is_animated)
        {
            m_light.m_is_animated = false;
            //m_light.drawSceneToShadowFBO();
        }

        if(m_meshes.size()>0)
        {
            m_camera.computeWorldMatrix();
            m_camera.computeViewMatrix();

            // Render Meshes
            for (int i = 0; i < m_meshes.size(); i++)
                m_meshes.get(i).draw(m_shader.getProgram(), m_camera, m_light, m_ubo_matrices[0]);
        }

        // Get the amount of time the last frame took.
        m_rendering_settings.m_fps.end();
        m_rendering_settings.m_fps.compute();
        m_rendering_settings.m_fps.reset();
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
            m_aabb.m_min.x = Math.min(m_aabb.m_min.x, new_mesh.m_aabb.m_min.x );
            m_aabb.m_min.y = Math.min(m_aabb.m_min.y, new_mesh.m_aabb.m_min.y );
            m_aabb.m_min.z = Math.min(m_aabb.m_min.z, new_mesh.m_aabb.m_min.z );

            m_aabb.m_max.x = Math.max(m_aabb.m_max.x, new_mesh.m_aabb.m_max.x );
            m_aabb.m_max.y = Math.max(m_aabb.m_max.y, new_mesh.m_aabb.m_max.y );
            m_aabb.m_max.z = Math.max(m_aabb.m_max.z, new_mesh.m_aabb.m_max.z );
        }
        m_aabb.computeCenter();
        m_aabb.computeRadius();

        float dis = ((Math.max(Math.abs(m_aabb.m_max.x - m_aabb.m_min.x), Math.abs(m_aabb.m_max.z - m_aabb.m_min.z))) / m_theta) + 0.001f;

        if(align_to_aabb)
        {
            // Update Camera
            m_camera.m_eye.x    = m_aabb.m_center.x + dis;
            m_camera.m_eye.y    = m_aabb.m_center.y + dis;
            m_camera.m_eye.z    = m_aabb.m_center.z + dis;
            m_camera.m_target   = new Float3(m_aabb.m_center.x, m_aabb.m_center.y, m_aabb.m_center.z);

            // Update Light
            m_light.m_camera.m_eye.x   = m_aabb.m_center.x + dis;
            m_light.m_camera.m_eye.y   = m_aabb.m_center.y + dis;
            m_light.m_camera.m_eye.z   = m_aabb.m_center.z + dis;
            m_light.m_camera.m_target  = m_aabb.m_center;

            m_light.m_initial_position = new Float3(m_light.m_camera.m_eye.x, m_light.m_camera.m_eye.y, m_light.m_camera.m_eye.z);
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
}