package grabasilak.iti.www.myapplication;

import android.opengl.Matrix;

class Camera {

            float    m_world_rot_angle;
    private float[]  m_world_rot_axis = new float[3];

    float[] m_world_matrix       = new float[16];
    float[] m_view_matrix        = new float[16];
    float[] m_projection_matrix  = new float[16];

            float[] m_eye        = new float[3];
            float[] m_target     = new float[3];
    private float[] m_up         = new float[3];

            float   m_fov;
            float   m_near_field;
            float   m_far_field;

    Camera ()
    {
        m_world_rot_angle   = 0.0f;
        m_world_rot_axis[0] = 0.0f;
        m_world_rot_axis[1] = 1.0f;
        m_world_rot_axis[2] = 0.0f;

        m_eye[0] =  0.0f;
        m_eye[1] =  0.0f;
        m_eye[2] = -4.0f;

        m_target[0] = m_target[1] = m_target[2] = 0.0f;

        m_up[0] = 0.0f;
        m_up[1] = 1.0f;
        m_up[2] = 0.0f;

        m_near_field = 1f;
        m_far_field  = 100.0f;
        m_fov        = 30.0f;
    }

    void computeWorldMatrix()
    {
        Matrix.setRotateM(m_world_matrix, 0, m_world_rot_angle, m_world_rot_axis[0], m_world_rot_axis[1], m_world_rot_axis[2]);
    }

    void computeViewMatrix()
    {
        Matrix.setLookAtM(m_view_matrix, 0, m_eye[0], m_eye[1], m_eye[2], m_target[0], m_target[1], m_target[2],  m_up[0],  m_up[1],  m_up[2]);
    }

    void computeProjectionMatrix(float aspect_ratio)
    {
        Matrix.perspectiveM(m_projection_matrix, 0, m_fov, aspect_ratio, m_near_field, m_far_field);
    }
    void computeProjectionMatrix(int width, int height, int depth)
    {
        Matrix.orthoM(m_projection_matrix, 0, 0f, width, 0.0f, height, 0.0f, depth);// need fixing
    }
}
