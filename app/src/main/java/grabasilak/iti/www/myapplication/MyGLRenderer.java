package grabasilak.iti.www.myapplication;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES31.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES31.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES31.GL_DEPTH_TEST;
import static android.opengl.GLES31.GL_LEQUAL;
import static android.opengl.GLES31.glClear;
import static android.opengl.GLES31.glClearColor;
import static android.opengl.GLES31.glClearDepthf;
import static android.opengl.GLES31.glDepthFunc;
import static android.opengl.GLES31.glEnable;

public class MyGLRenderer implements GLSurfaceView.Renderer {

    public  Camera        m_camera;
    private Context       m_context;
    private Mesh          m_mesh;
    private Shader        m_shader_rendering;

    private float[]       m_mvp_matrix    = new float[16];

    private RenderingSettings m_rendering_settings;

    public MyGLRenderer(Context context)
    {
        m_context = context;
    }

    // INIT FUNCTION
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {

        m_rendering_settings = new RenderingSettings();

        // Load Camera
        m_camera	= new Camera();

        // Load Models
        m_mesh      = new Mesh();

        // Load Shaders
        m_shader_rendering = new Shader(m_context, "simple_rendering");

        // Set Depth Test
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);

        // Set Culling Test
        //glEnable(GL_CULL_FACE);
//        glCullFace(GL_BACK);
//        glFrontFace(GL_CCW);

        // Set the background frame color
        glClearColor(m_rendering_settings.m_background_color[0],
                m_rendering_settings.m_background_color[1],
                m_rendering_settings.m_background_color[2],
                m_rendering_settings.m_background_color[3]);

        glClearDepthf(m_rendering_settings.m_depth);

        m_rendering_settings.checkGlError("onSurfaceCreated");
    }

    // DRAW FUNCTION
    public void onDrawFrame(GL10 unused)
    {
        // Redraw background color
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        m_camera.computeWorldMatrix();
        m_camera.computeViewMatrix();

        // Calculate the projection and view transformation
        Matrix.multiplyMM(m_mvp_matrix, 0, m_camera.m_projection_matrix, 0, m_camera.m_view_matrix, 0);
        Matrix.multiplyMM(m_mvp_matrix, 0, m_mvp_matrix, 0,  m_camera.m_world_matrix, 0);

        // -- RENDER BEGIN --
        {
            m_mesh.draw(m_shader_rendering.getProgram(), m_mvp_matrix);
        }
        // -- RENDER  END  --

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
}