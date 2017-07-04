package grabasilak.iti.www.myapplication;

import android.content.Context;
import android.opengl.GLSurfaceView;

import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES31.GL_BACK;
import static android.opengl.GLES31.GL_CCW;
import static android.opengl.GLES31.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES31.GL_CULL_FACE;
import static android.opengl.GLES31.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES31.GL_DEPTH_TEST;
import static android.opengl.GLES31.GL_DYNAMIC_DRAW;
import static android.opengl.GLES31.GL_LEQUAL;
import static android.opengl.GLES31.GL_UNIFORM_BUFFER;
import static android.opengl.GLES31.glBindBuffer;
import static android.opengl.GLES31.glBindBufferBase;
import static android.opengl.GLES31.glBufferData;
import static android.opengl.GLES31.glClear;
import static android.opengl.GLES31.glClearColor;
import static android.opengl.GLES31.glClearDepthf;
import static android.opengl.GLES31.glCullFace;
import static android.opengl.GLES31.glDepthFunc;
import static android.opengl.GLES31.glEnable;
import static android.opengl.GLES31.glFrontFace;
import static android.opengl.GLES31.glGenBuffers;

class MyGLRenderer implements GLSurfaceView.Renderer {

            AABB            m_aabb;
            Camera          m_camera;


    private Context         m_context;

    private ArrayList<Mesh> m_meshes;

    private Shader          m_shader;

    int [] m_ubo_matrices = new int[1];

    final float             m_theta = (float) Math.sin(Math.PI / 8.0f);

    private RenderingSettings m_rendering_settings;

    MyGLRenderer(Context context)
    {
        m_context = context;
        m_rendering_settings = new RenderingSettings();
    }

    // INIT FUNCTION
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {

        glGenBuffers(1, m_ubo_matrices, 0);
        glBindBuffer(GL_UNIFORM_BUFFER, m_ubo_matrices[0]);
        {
            glBufferData(GL_UNIFORM_BUFFER, 4 * 16 * Float.BYTES, null, GL_DYNAMIC_DRAW);
        }
        glBindBuffer(GL_UNIFORM_BUFFER, 0);
        glBindBufferBase(GL_UNIFORM_BUFFER, 0, m_ubo_matrices[0]);

        m_aabb      = new AABB();
        m_camera    = new Camera();
        m_meshes    = new ArrayList<>();

        // Load Meshes
        addMesh(m_context.getString(R.string.MESH_NAME));

        // Load Shaders
        m_shader = new Shader(m_context,  m_context.getString(R.string.SHADING_NAME));

        // Set the background frame color
        glClearColor(m_rendering_settings.m_background_color[0],
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

        if(m_meshes.size()>0)
        {
            m_camera.computeWorldMatrix();
            m_camera.computeViewMatrix();

            // Render Meshes
            for (int i = 0; i < m_meshes.size(); i++)
                m_meshes.get(i).draw(m_shader.getProgram(), m_camera, m_ubo_matrices[0]);
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

    void addMesh(String fileName)
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

        // Update Camera
        {
            float dis = ((Math.max(Math.abs(m_aabb.m_max.x - m_aabb.m_min.x), Math.abs(m_aabb.m_max.z - m_aabb.m_min.z))) / m_theta) + 0.001f;

            m_camera.m_eye.x    = m_aabb.m_center.x + dis;
            m_camera.m_eye.y    = m_aabb.m_center.y + dis;
            m_camera.m_eye.z    = m_aabb.m_center.z + dis;
            m_camera.m_target   = m_aabb.m_center;
        }

        // WORKING!!
//        Mesh m = m_meshes.get(0);
//        m.m_diffuse_color[0] = 1;
//        m.m_diffuse_color[1] = 0;
//        m.m_diffuse_color[2] = 0;
//        m.m_diffuse_color[3] = 1;
    }
}