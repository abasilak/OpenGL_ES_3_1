package grabasilak.iti.www.myapplication;

import android.opengl.Matrix;
import android.renderscript.Float3;

class Camera {

    float   m_world_rot_angle;
    private Float3  m_world_rot_axis;

    float[] m_world_matrix       = new float[16];
    float[] m_view_matrix        = new float[16];
    float[] m_projection_matrix  = new float[16];

    Float3 m_eye;
    Float3 m_target;
    private Float3 m_up;

    float   m_fov;
    private float   m_near_field;
    private float   m_far_field;


    Camera ()
    {
        m_world_rot_angle = 0.0f;
        m_world_rot_axis = new Float3(0.0f,1.0f,0.0f);

        m_eye    = new Float3(0.0f, 0.0f, -4.0f);

        m_target = new Float3(0.0f, 0.0f, 0.0f);

        m_up     = new Float3(0.0f, 1.0f, 0.0f);

        m_near_field = 1.0f;
        m_far_field  = 1000.0f;
        m_fov        = 30.0f;
    }

    void computeWorldMatrix()
    {
        Matrix.setRotateM(m_world_matrix, 0, m_world_rot_angle, m_world_rot_axis.x, m_world_rot_axis.y, m_world_rot_axis.z);
    }

    void computeViewMatrix()
    {
        Matrix.setLookAtM(m_view_matrix, 0, m_eye.x, m_eye.y, m_eye.z, m_target.x, m_target.y, m_target.z,  m_up.x,  m_up.y,  m_up.z);
    }

    void computeProjectionMatrix(float aspect_ratio)
    {
        Matrix.perspectiveM(m_projection_matrix, 0, m_fov, aspect_ratio, m_near_field, m_far_field);
    }
}
