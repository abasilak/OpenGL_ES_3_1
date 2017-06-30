package grabasilak.iti.www.myapplication;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES31.*;

class MyGLRenderer implements GLSurfaceView.Renderer {

            Camera          m_camera;
    private Context         m_context;

    private ArrayList<Mesh> m_meshes;

    private Shader          m_shader;

    private float[]         m_mvp_matrix = new float[16];

    private RenderingSettings m_rendering_settings;

    MyGLRenderer(Context context)
    {
        m_context = context;
    }

    // INIT FUNCTION
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {

        m_meshes  = new ArrayList<>();
        m_meshes.add(new Mesh(m_context, m_context.getString(R.string.MESH_NAME)));

        // Load Shaders
        m_shader = new Shader(m_context,  m_context.getString(R.string.SHADING_NAME));

        // Load Camera
        m_camera	= new Camera();

        m_rendering_settings = new RenderingSettings();

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

            // Calculate the projection and view transformation
            Matrix.multiplyMM(m_mvp_matrix, 0, m_camera.m_projection_matrix, 0, m_camera.m_view_matrix, 0);
            Matrix.multiplyMM(m_mvp_matrix, 0, m_mvp_matrix, 0, m_camera.m_world_matrix, 0);

            // Render Meshes
            for (int i = 0; i < m_meshes.size(); i++)
                m_meshes.get(i).draw(m_shader.getProgram(), m_mvp_matrix);
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
        // Load Models
        m_meshes.add(new Mesh(m_context, m_context.getString(R.string.MESH_NAME)));

        // WORKING!!
        Mesh m = m_meshes.get(0);
        m.m_diffuse_color[0] = 1;
        m.m_diffuse_color[1] = 0;
        m.m_diffuse_color[2] = 0;
        m.m_diffuse_color[3] = 1;
    }
}