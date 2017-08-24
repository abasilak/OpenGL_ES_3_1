package grabasilak.iti.www.myapplication;

import static java.lang.System.nanoTime;

class FPS {

    private final float     FPS_FACTOR = 1000000000.0f;

    private float      m_fps;
    private long       m_end_time;
    private long       m_start_time;

    FPS ()
    {
        start();
    }

    private void start()
    {
        m_start_time = nanoTime();
    }

    void reset()
    {
        m_start_time = m_end_time;
    }

    void end()
    {
        m_end_time  = nanoTime();
    }

    void compute()
    {
        m_fps = FPS_FACTOR / (m_end_time - m_start_time);
        //Log.i("onDrawFrame", "FPS: " + m_fps);
    }

    float get()
    {
        return m_fps;
    }

}
