package grabasilak.iti.www.myapplication;

import static java.lang.System.nanoTime;

/**
 * Created by Andreas on 26-Jun-17.
 */

public class FPS {

    private final float     FPS_FACTOR = 1000000000.0f;

    public       float      m_fps;
    public       long       m_end_time;
    public       long       m_start_time;

    public FPS ()
    {
        start();
    }

    private void start()
    {
        m_start_time = nanoTime();
    }

    public void reset()
    {
        m_start_time = m_end_time;
    }

    public void end()
    {
        m_end_time  = nanoTime();
    }

    public void compute()
    {
        m_fps = FPS_FACTOR / (m_end_time - m_start_time);
        //Log.i("onDrawFrame", "FPS: " + m_fps);
    }
}
