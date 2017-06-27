package grabasilak.iti.www.myapplication;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {

    private MyGLSurfaceView myGLSurfaceView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create a GLSurfaceView instance and set it
        // as the ContentView for this Activity.
        myGLSurfaceView = new MyGLSurfaceView(this);
        setContentView(myGLSurfaceView);

        // Set our view.
        //setContentView(R.layout.activity_main);

        // Retrieve our ConstraintLayout layout from our main layout we just set to our view.
        //LinearLayout my_layout = (LinearLayout) findViewById(R.id.game_layout);

        // Attach our surfaceview to our relative layout from our main layout.
        //LinearLayout.LayoutParams glParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 0.9f);
        //my_layout.addView(myGLSurfaceView, glParams);
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
