package grabasilak.iti.www.myapplication;

import android.opengl.Matrix;
import android.renderscript.Float3;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static android.opengl.GLES31.*;
import static grabasilak.iti.www.myapplication.Util.m_sizeofV4;

class Light {

    private int []	    m_ubo = new int[1];

    private Float3      m_color;

    boolean	            m_is_rendered;
    boolean	            m_is_animated;
    private boolean	    m_is_spotlight;
    private boolean	    m_casts_shadows;
    private float	    m_spotlight_cutoff;

    private float	    m_att_constant;
    private float	    m_att_linear;
    private float	    m_att_quadratic;


    private final int	m_shadow_size           = 1024;
    int	[]		        m_shadow_map_fbo        = new int[1];
    int	[]		        m_shadow_map_texture    = new int[1];
    final Viewport      m_shadow_map_viewport;

    Float3              m_initial_position;

    Camera              m_camera;
    private FloatBuffer m_light_buffer;

    Light()
    {
        m_att_constant		    = 1.0f;
        m_att_linear		    = 0.01f;
        m_att_quadratic		    = 0.0032f;
        m_spotlight_cutoff	    = 30.0f;

        m_is_spotlight          = true;
        m_casts_shadows         = false;

        m_camera                = new Camera();
        m_camera.m_fov          = 90.0f;

        m_initial_position      = new Float3(0.0f,0.0f,0.0f);

        m_shadow_map_viewport   = new Viewport(0, 0, m_shadow_size, m_shadow_size);

        m_color                 = new Float3(1.0f,1.0f,1.0f);

        createShadowFBO();
    }

    void createUBO()
    {
        float [] light_data = new float[24];

        m_light_buffer = ByteBuffer.allocateDirect (6 * m_sizeofV4).order (ByteOrder.nativeOrder() ).asFloatBuffer();
        //the light position
        light_data[0] = m_camera.m_eye.x;
        light_data[1] = m_camera.m_eye.y;
        light_data[2] = m_camera.m_eye.z;
        // spotlight or directional
        light_data[3] = m_is_spotlight ? 1.0f : 0.0f;

        float[]	light_direction_wcs = new float[3];
        light_direction_wcs[0]  = m_camera.m_target.x - m_camera.m_eye.x;
        light_direction_wcs[1]  = m_camera.m_target.y - m_camera.m_eye.y;
        light_direction_wcs[2]  = m_camera.m_target.z - m_camera.m_eye.z;
        light_direction_wcs     = Util.normalize(light_direction_wcs);

        //the light target
        light_data[4] = light_direction_wcs[0];
        light_data[5] = light_direction_wcs[1];
        light_data[6] = light_direction_wcs[2];
        // spotlight or directional
        light_data[7] = m_casts_shadows ? 1.0f : 0.0f;

        //the light colors
        // ambient
        light_data[8 ] = m_color.x * 0.05f;
        light_data[9 ] = m_color.y * 0.05f;
        light_data[10] = m_color.z * 0.05f;
        light_data[11] = 1.0f;
        // diffuse
        light_data[12] = m_color.x;
        light_data[13] = m_color.y;
        light_data[14] = m_color.z;
        light_data[15] = 1.0f;
        // specular
        light_data[16] = m_color.x;
        light_data[17] = m_color.y;
        light_data[18] = m_color.z;
        light_data[19] = 1.0f;

        //the light attenuation & cutoff factors
        light_data[20] = m_att_constant;
        light_data[21] = m_att_linear;
        light_data[22] = m_att_quadratic;
        light_data[23] = (float) Math.cos(Math.toRadians(m_spotlight_cutoff));

        glGenBuffers(1, m_ubo, 0);
        glBindBuffer(GL_UNIFORM_BUFFER, m_ubo[0]);
        {
            m_light_buffer.put (light_data);
            m_light_buffer.position ( 0 );
            glBufferData   (GL_UNIFORM_BUFFER, 6 * m_sizeofV4, m_light_buffer, GL_DYNAMIC_DRAW);
        }
        glBindBuffer(GL_UNIFORM_BUFFER, 0);
        glBindBufferBase(GL_UNIFORM_BUFFER, 2, m_ubo[0]);
    }

    void updateUBO()
    {
        float [] light_data = new float[8];

        m_light_buffer = ByteBuffer.allocateDirect (2 * m_sizeofV4).order (ByteOrder.nativeOrder() ).asFloatBuffer();
        //the light position
        light_data[0] = m_camera.m_eye.x;
        light_data[1] = m_camera.m_eye.y;
        light_data[2] = m_camera.m_eye.z;
        // spotlight or directional
        light_data[3] = m_is_spotlight ? 1.0f : 0.0f;

        float[]	light_direction_wcs = new float[3];
        light_direction_wcs[0]  = m_camera.m_target.x - m_camera.m_eye.x;
        light_direction_wcs[1]  = m_camera.m_target.y - m_camera.m_eye.y;
        light_direction_wcs[2]  = m_camera.m_target.z - m_camera.m_eye.z;
        light_direction_wcs     = Util.normalize(light_direction_wcs);

        //the light target
        light_data[4] = light_direction_wcs[0];
        light_data[5] = light_direction_wcs[1];
        light_data[6] = light_direction_wcs[2];
        // spotlight or directional
        light_data[7] = m_casts_shadows ? 1.0f : 0.0f;

        glBindBuffer(GL_UNIFORM_BUFFER, m_ubo[0]);
        {
            m_light_buffer.put (light_data);
            m_light_buffer.position ( 0 );
            glBufferSubData(GL_UNIFORM_BUFFER, 0, 2 * m_sizeofV4, m_light_buffer);
        }
        glBindBuffer(GL_UNIFORM_BUFFER, 0);
    }

    void animation()
    {
        m_is_animated = true;

        m_camera.m_world_rot_angle += 0.3f;
        if (m_camera.m_world_rot_angle > 360.0f)
            m_camera.m_world_rot_angle -= 360.0f;
        m_camera.computeWorldMatrix();

        float [] old_pos = new float [4];
        old_pos[0] = m_initial_position.x;
        old_pos[1] = m_initial_position.y;
        old_pos[2] = m_initial_position.z;
        old_pos[3] = 1.0f;

        float [] new_pos = new float [4];
        Matrix.multiplyMV(new_pos, 0, m_camera.m_world_matrix, 0, old_pos, 0);

        m_camera.m_eye.x = new_pos[0];
        m_camera.m_eye.y = new_pos[1];
        m_camera.m_eye.z = new_pos[2];

        updateUBO();
    }

    void createShadowFBO()
    {
        // Create Shadow Texture
        glGenTextures(1, m_shadow_map_texture, 0);
        glBindTexture(GL_TEXTURE_2D, m_shadow_map_texture[0]);
        {
            glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT32F, m_shadow_map_viewport.m_width, m_shadow_map_viewport.m_height, 0, GL_DEPTH_COMPONENT, GL_FLOAT, null);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        }
        glBindTexture(GL_TEXTURE_2D, 0);

        // Create Shadow Framebuffer
        glGenFramebuffers(1, m_shadow_map_fbo, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, m_shadow_map_fbo[0]);
        {
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, m_shadow_map_texture[0], 0);
        }
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        RenderingSettings.checkFramebufferStatus();
    }
}
