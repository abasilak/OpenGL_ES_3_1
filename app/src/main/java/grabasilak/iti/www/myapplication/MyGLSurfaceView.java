package grabasilak.iti.www.myapplication;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

/**
 * A view container where OpenGL ES graphics can be drawn on screen.
 * This view can also be used to capture touch events, such as a user
 * interacting with drawn objects.
 */
class MyGLSurfaceView extends GLSurfaceView
{
    private final MyGLRenderer m_renderer;

    public MyGLSurfaceView(Context context)
    {
        super(context);

        // Create an OpenGL ES 3.0 context
        setEGLContextClientVersion(3);

        m_renderer = new MyGLRenderer(context);

        // Set the Renderer for drawing on the GLSurfaceView
        setRenderer(m_renderer);

        // Render the view only when there is a change in the drawing data
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        // Render the view always
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    // HANDLING EVENTS
    private final float TOUCH_SCALE_FACTOR      = 0.001f;
    private final float TOUCH_SCALE_FACTOR_ROT  = 0.1f;

    private float   mPreviousX;
    private float   mPreviousY;

    @Override
    public boolean onTouchEvent(MotionEvent e)
    {
        // MotionEvent reports input details from the touch screen
        // and other input controls. In this case, you are only
        // interested in events where the touch position changed.

        float x = e.getX();
        float y = e.getY();

        switch (e.getAction()) {
            case MotionEvent.ACTION_MOVE:

                float dx = x - mPreviousX;
                float dy = y - mPreviousY;

                // One finger detected
                if (e.getPointerCount() == 1)
                {
                    m_renderer.m_camera.m_target[0] += dx * TOUCH_SCALE_FACTOR;
                    m_renderer.m_camera.m_target[1] += dy * TOUCH_SCALE_FACTOR;
                }
                // More than one fingers detected
                else
                {
                    // reverse direction of rotation above the mid-line
                    if (y > getHeight() / 2) dx *= -1 ;
                    // reverse direction of rotation to left of the mid-line
                    if (x < getWidth () / 2) dy *= -1 ;

                    m_renderer.m_camera.m_world_rot_angle += ((dx + dy) * TOUCH_SCALE_FACTOR_ROT);
                }
                requestRender();
        }

        mPreviousX = x;
        mPreviousY = y;

        return true;
    }
}