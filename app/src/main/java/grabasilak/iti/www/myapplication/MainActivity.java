package grabasilak.iti.www.myapplication;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {

    private MyGLSurfaceView myGLSurfaceView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create a GLSurfaceView instance and set it as the ContentView for this Activity.
        //myGLSurfaceView = new MyGLSurfaceView(this);
        //myGLSurfaceView.init(this);
        //setContentView(myGLSurfaceView);

        // Set our view.
        setContentView(R.layout.activity_main);

        myGLSurfaceView = (MyGLSurfaceView) findViewById(R.id.my_opengl_es_view);
        myGLSurfaceView.getHolder().setFixedSize(getResources().getInteger(R.integer.SCREEN_WIDTH),getResources().getInteger(R.integer.SCREEN_HEIGHT));
        myGLSurfaceView.getHolder().setFormat(1);
        myGLSurfaceView.init(this, getResources().getInteger(R.integer.SCREEN_WIDTH), getResources().getInteger(R.integer.SCREEN_HEIGHT));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // The following call resumes a paused rendering thread.
        // If you de-allocated graphic objects for onPause()
        // this is a good place to re-allocate them.
        myGLSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // The following call pauses the rendering thread.
        // If your OpenGL application is memory intensive,
        // you should consider de-allocating objects that
        // consume significant memory here.
        myGLSurfaceView.onPause();
    }
}
