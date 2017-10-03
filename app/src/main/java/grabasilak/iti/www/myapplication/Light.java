package grabasilak.iti.www.myapplication;

import android.content.Context;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;

import static android.opengl.GLES31.GL_DYNAMIC_DRAW;
import static android.opengl.GLES31.GL_UNIFORM_BUFFER;
import static android.opengl.GLES31.glBindBuffer;
import static android.opengl.GLES31.glBindBufferBase;
import static android.opengl.GLES31.glBufferData;
import static android.opengl.GLES31.glBufferSubData;
import static android.opengl.GLES31.glDepthMask;
import static android.opengl.GLES31.glGenBuffers;
import static grabasilak.iti.www.myapplication.Util.m_sizeofV4;

class Light
{
    ShadowMapping m_shadow_mapping;

    private boolean	    m_is_rendered;
    private boolean	    m_is_animated;
    public  boolean	    m_is_spotlight;
    private boolean	    m_casts_shadows;
    private float	    m_spotlight_cutoff;

    private float	    m_att_constant;
    private float	    m_att_linear;
    private float	    m_att_quadratic;
    private float []    m_color                     = new float[3];
    private float []    m_initial_position          = new float[3];

    private Shader      m_shader_simple_rendering;

    private FloatBuffer m_buffer;
    private int []	    m_ubo = new int[1];

    Camera              m_camera;
    Viewport            m_viewport;
    private Mesh        m_sphere;
    private float       m_radius;

    Light(Context context)
    {
        int m_texture_size      = context.getResources().getInteger(R.integer.SHADOW_MAP_SIZE);

        m_att_constant		    = 1.0f;
        m_att_linear		    = 0.01f;
        m_att_quadratic		    = 0.0032f;
        m_spotlight_cutoff	    = 30.0f;

        m_is_rendered           = true;
        m_is_spotlight          = true;
        m_casts_shadows         = true;

        m_camera                = new Camera();
        m_camera.m_fov          = 90.0f;

        m_color[0]              = m_color[1]            = m_color[2]            = 1.0f;
        m_initial_position[0]   = m_initial_position[1] = m_initial_position[2] = 0.0f;

        m_viewport = new Viewport(0, 0, m_texture_size, m_texture_size);
        m_viewport.setAspectRatio();

        m_shadow_mapping            = new ShadowMapping(context, m_viewport);

        m_shader_simple_rendering   = new Shader(context, context.getString(R.string.SHADER_SIMPLE_RENDERING_NAME));
        m_sphere                    = new Mesh  (context, "sphere.obj");

        m_sphere.m_mtl_materials.get(0).m_diffuse[0] = 0.9f;
        m_sphere.m_mtl_materials.get(0).m_diffuse[1] = 0.9f;
        m_sphere.m_mtl_materials.get(0).m_diffuse[2] = 0.f;
    }

    void init(AABB aabb, float dis)
    {
        m_radius = aabb.m_radius/50f;

        m_camera.m_eye[0]     = aabb.m_center[0] + dis;
        m_camera.m_eye[1]     = aabb.m_center[1] + dis;
        m_camera.m_eye[2]     = aabb.m_center[2] + dis;

        m_camera.m_target[0]  = aabb.m_center[0];
        m_camera.m_target[1]  = aabb.m_center[1];
        m_camera.m_target[2]  = aabb.m_center[2];

        m_camera.computeNearFarFields(aabb.m_min, aabb.m_max);

        m_initial_position[0] = m_camera.m_eye[0];
        m_initial_position[1] = m_camera.m_eye[1];
        m_initial_position[2] = m_camera.m_eye[2];

        createUBO();
    }

    void animation()
    {
        m_is_animated = true;

        m_camera.m_world_rot_angle += 0.2f;
        if (m_camera.m_world_rot_angle > 360.0f)
            m_camera.m_world_rot_angle -= 360.0f;
        m_camera.computeWorldMatrix();

        float [] old_pos = new float [4];
        old_pos[0] = m_initial_position[0];
        old_pos[1] = m_initial_position[1];
        old_pos[2] = m_initial_position[2];
        old_pos[3] = 1.0f;

        float [] new_pos = new float [4];
        Matrix.multiplyMV(new_pos, 0, m_camera.m_world_matrix, 0, old_pos, 0);

        m_camera.m_eye[0] = new_pos[0];
        m_camera.m_eye[1] = new_pos[1];
        m_camera.m_eye[2] = new_pos[2];

        updateUBO();
    }

    private void createUBO()
    {
        float [] light_data = new float[24];

        m_buffer = ByteBuffer.allocateDirect (6 * m_sizeofV4).order (ByteOrder.nativeOrder() ).asFloatBuffer();
        //the light position
        light_data[0] = m_camera.m_eye[0];
        light_data[1] = m_camera.m_eye[1];
        light_data[2] = m_camera.m_eye[2];
        // spotlight or directional
        light_data[3] = m_is_spotlight ? 1.0f : 0.0f;

        float[]	light_direction_wcs = new float[3];
        light_direction_wcs[0]  = m_camera.m_target[0] - m_camera.m_eye[0];
        light_direction_wcs[1]  = m_camera.m_target[1] - m_camera.m_eye[1];
        light_direction_wcs[2]  = m_camera.m_target[2] - m_camera.m_eye[2];
        light_direction_wcs     = Util.normalize(light_direction_wcs);

        //the light target
        light_data[4] = light_direction_wcs[0];
        light_data[5] = light_direction_wcs[1];
        light_data[6] = light_direction_wcs[2];
        // spotlight or directional
        light_data[7] = m_casts_shadows ? 1.0f : 0.0f;

        //the light colors
        // ambient
        light_data[8 ] = m_color[0] * 0.05f;
        light_data[9 ] = m_color[1] * 0.05f;
        light_data[10] = m_color[2] * 0.05f;
        light_data[11] = 1.0f;
        // diffuse
        light_data[12] = m_color[0];
        light_data[13] = m_color[1];
        light_data[14] = m_color[2];
        light_data[15] = 1.0f;
        // specular
        light_data[16] = m_color[0];
        light_data[17] = m_color[1];
        light_data[18] = m_color[2];
        light_data[19] = 1.0f;

        //the light attenuation & cutoff factors
        light_data[20] = m_att_constant;
        light_data[21] = m_att_linear;
        light_data[22] = m_att_quadratic;
        light_data[23] = (float) Math.cos(Math.toRadians(m_spotlight_cutoff));

        glGenBuffers(1, m_ubo, 0);
        glBindBuffer(GL_UNIFORM_BUFFER, m_ubo[0]);
        {
            m_buffer.put (light_data);
            m_buffer.position ( 0 );
            glBufferData   (GL_UNIFORM_BUFFER, 6 * m_sizeofV4, m_buffer, GL_DYNAMIC_DRAW);
        }
        glBindBuffer(GL_UNIFORM_BUFFER, 0);
        glBindBufferBase(GL_UNIFORM_BUFFER, 2, m_ubo[0]);
    }

    private void updateUBO()
    {
        float [] light_data = new float[8];

        m_buffer = ByteBuffer.allocateDirect (2 * m_sizeofV4).order (ByteOrder.nativeOrder() ).asFloatBuffer();
        //the light position
        light_data[0] = m_camera.m_eye[0];
        light_data[1] = m_camera.m_eye[1];
        light_data[2] = m_camera.m_eye[2];
        // spotlight or directional
        light_data[3] = m_is_spotlight ? 1.0f : 0.0f;

        float[]	light_direction_wcs = new float[3];
        light_direction_wcs[0]  = m_camera.m_target[0] - m_camera.m_eye[0];
        light_direction_wcs[1]  = m_camera.m_target[1] - m_camera.m_eye[1];
        light_direction_wcs[2]  = m_camera.m_target[2] - m_camera.m_eye[2];
        light_direction_wcs     = Util.normalize(light_direction_wcs);

        //the light target
        light_data[4] = light_direction_wcs[0];
        light_data[5] = light_direction_wcs[1];
        light_data[6] = light_direction_wcs[2];
        // spotlight or directional
        light_data[7] = m_casts_shadows ? 1.0f : 0.0f;

        glBindBuffer(GL_UNIFORM_BUFFER, m_ubo[0]);
        {
            m_buffer.put (light_data);
            m_buffer.position ( 0 );
            glBufferSubData(GL_UNIFORM_BUFFER, 0, 2 * m_sizeofV4, m_buffer);
        }
        glBindBuffer(GL_UNIFORM_BUFFER, 0);
    }

    void draw(RenderingSettings rendering_settings, TextManager text_manager, ArrayList<Mesh> meshes, Camera camera)
    {
        if (m_is_animated)
        {
            m_is_animated = false;

            if(m_casts_shadows)
            {
                m_camera.computeViewMatrix();
                //m_viewport.setViewport();

                m_shadow_mapping.draw(rendering_settings, text_manager, meshes, new ArrayList<>(Collections.singletonList(this)), camera, 0);
            }
        }
    }

    void render(Camera camera)
    {
        if (m_is_rendered)
        {
            glDepthMask(false);
            {
                m_sphere.setIdentity();
                m_sphere.translate(m_camera.m_eye[0], m_camera.m_eye[1], m_camera.m_eye[2]);
                m_sphere.scale(m_radius, m_radius, m_radius);
                m_sphere.drawSimple(m_shader_simple_rendering.getProgram(), camera);
            }
            glDepthMask(true);
        }
    }
}
