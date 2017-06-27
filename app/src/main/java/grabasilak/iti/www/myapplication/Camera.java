package grabasilak.iti.www.myapplication;

import android.opengl.Matrix;

/**
 * Created by Andreas on 27-Jun-17.
 */

public class Camera {

    public float   m_world_angle;
    public float[] m_world_rot_axis     = new float[3];

    public float[] m_world_matrix       = new float[16];
    public float[] m_view_matrix        = new float[16];
    public float[] m_projection_matrix  = new float[16];

    public float[] m_eye        = new float[3];
    public float[] m_target     = new float[3];
    public float[] m_up         = new float[3];

    public float   m_near_field;
    public float   m_far_field;
    public float   m_fov;

    public Camera ()
    {
        m_world_angle = 0.0f;

        m_world_rot_axis[0] = 0.0f;
        m_world_rot_axis[1] = 0.0f;
        m_world_rot_axis[2] = 1.0f;

        m_eye[0] =  0.0f;
        m_eye[1] =  0.0f;
        m_eye[2] = -3.0f;

        m_target[0] = 0.0f;
        m_target[1] = 0.0f;
        m_target[2] = 0.0f;

        m_up[0] = 0.0f;
        m_up[1] = 1.0f;
        m_up[2] = 0.0f;

        m_near_field = 1.0f;
        m_far_field  = 10.0f;
        m_fov        = 45.0f;
    }

    void computeWorldMatrix()
    {
        Matrix.setRotateM(m_world_matrix, 0, m_world_angle, m_world_rot_axis[0], m_world_rot_axis[1], m_world_rot_axis[2]);
    }

    void computeViewMatrix()
    {
        Matrix.setLookAtM(m_view_matrix, 0, m_eye[0], m_eye[1], m_eye[2], m_target[0], m_target[1], m_target[2],  m_up[0],  m_up[1],  m_up[2]);
    }

    void computeProjectionMatrix(float aspect_ratio)
    {
        Matrix.perspectiveM(m_projection_matrix, 0, m_fov, aspect_ratio, m_near_field, m_far_field);
        //Matrix.frustumM(m_projection_matrix, 0, -aspect_ratio, aspect_ratio, -1, 1, 3, 7);
    }
}
