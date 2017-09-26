package grabasilak.iti.www.myapplication;

import static java.lang.System.nanoTime;

class FPS {

    private final float     MS_FACTOR   = 1000000.0f;
    private final float     FPS_FACTOR  = 1000000000.0f;

    private float      m_fps;
    private float      m_time;
    private long       m_end_time;
    private long       m_start_time;
    private long       m_fence;

    FPS ()
    {
        start();
    }

    void start()
    {
        m_start_time = nanoTime();
    }

    void reset()
    {
        m_start_time = m_end_time;
    }

    void end()
    {
//        m_fence = glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
//        glClientWaitSync(m_fence, GL_SYNC_FLUSH_COMMANDS_BIT, GL_TIMEOUT_IGNORED);
//        glDeleteSync(m_fence);

        m_end_time  = nanoTime();
        compute();
    }

    private void compute()
    {
        m_time = m_end_time - m_start_time;
        m_fps = FPS_FACTOR / m_time;
    }

    float getFPS()
    {
        return m_fps;
    }
    float getTime()
    {
        return m_time/MS_FACTOR;
    }
}
