package grabasilak.iti.www.myapplication;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * A view container where OpenGL ES graphics can be drawn on screen.
 * This view can also be used to capture touch events, such as a user
 * interacting with drawn objects.
 */
class MyGLSurfaceView extends GLSurfaceView
{
    private final  int   m_renderer_mode = GLSurfaceView.RENDERMODE_CONTINUOUSLY; //or use 'RENDERMODE_WHEN_DIRTY'
    private MyGLRenderer m_renderer;

    public MyGLSurfaceView(Context context)
    {
        super(context);
    }

    public MyGLSurfaceView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public void init(Context context)
    {
        // Create an OpenGL ES 3.0 context
        setEGLContextClientVersion(3);

        m_renderer = new MyGLRenderer(context);

        // Set the Renderer for drawing on the GLSurfaceView
        setRenderer(m_renderer);

        // Render the view
        setRenderMode(m_renderer_mode);
    }

    // HANDLING EVENTS
    private final float TOUCH_SCALE_FACTOR_X    = 0.001f;
    private final float TOUCH_SCALE_FACTOR_Y    = 0.001f;
    private final float TOUCH_SCALE_FACTOR_Z    = 0.05f;
    private final float TOUCH_SCALE_FACTOR_ROT  = 0.1f;

    private float   mPreviousX;
    private float   mPreviousY;
    private float   mPreviousZ;

    @Override
    public boolean onTouchEvent(MotionEvent e)
    {
        // MotionEvent reports input details from the touch screen
        // and other input controls. In this case, you are only
        // interested in events where the touch position changed.

        float x=e.getX(), dx, y=e.getY(), dy, z=1.0f, dz;

        switch (e.getAction()) {

            case MotionEvent.ACTION_MOVE:

                // One finger detected
                if (e.getPointerCount() == 1)
                {
                    x = e.getX();
                    y = e.getY();

                    dx = x - mPreviousX;
                    dy = y - mPreviousY;

                    // reverse direction of rotation above the mid-line
                    if (y > getHeight() / 2) dx *= -1 ;
                    // reverse direction of rotation to left of the mid-line
                    if (x < getWidth () / 2) dy *= -1 ;

                    m_renderer.m_camera.m_world_rot_angle += ((dx + dy) * TOUCH_SCALE_FACTOR_ROT);
                 //   m_renderer.m_camera.m_world_rot_axis   = (dy > dx) ? new Float3(0.0f,0.0f,1.0f) : new Float3(0.0f,1.0f,0.0f);
                }
                // Two fingers detected
                else // if (e.getPointerCount() == 2)
                {
                    z  = spacing(e);
                    dz = z - mPreviousZ;
                    if(z > 200.0f)
                    {
                        if(dz > 0)
                            m_renderer.m_camera.m_eye[2] += TOUCH_SCALE_FACTOR_Z;
                        else
                            m_renderer.m_camera.m_eye[2] -= TOUCH_SCALE_FACTOR_Z;
                    }
                    else
                    {
                        x = e.getX(0);
                        y = e.getY(0);

                        dx = x - mPreviousX;
                        dy = y - mPreviousY;

                        m_renderer.m_camera.m_target[0] += dx * TOUCH_SCALE_FACTOR_X;
                        m_renderer.m_camera.m_target[1] += dy * TOUCH_SCALE_FACTOR_Y;
                    }
                }
                requestRender();
                break;
        }

        mPreviousX = x;
        mPreviousY = y;
        mPreviousZ = z;

        return true;
    }

    /** Determine the space between the first two fingers */
    private float spacing(MotionEvent event)
    {
        float dx = event.getX(0) - event.getX(1);
        float dy = event.getY(0) - event.getY(1);

        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    void addMesh(String fileName)
    {
        m_renderer.addMesh(fileName);
    }
}