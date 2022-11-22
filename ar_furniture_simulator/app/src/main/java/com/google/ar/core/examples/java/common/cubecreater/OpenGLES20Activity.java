package com.google.ar.core.examples.java.common.cubecreater;

import android.content.Intent;
import android.os.Bundle;

import android.app.Activity;
import android.opengl.GLSurfaceView;

public class OpenGLES20Activity extends Activity {
    private GLSurfaceView mGLView;
    static int wid,dep,hei;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        String width = bundle.getString("width");
        String depth = bundle.getString("depth");
        String height = bundle.getString("height");
        try {
            wid = Integer.parseInt(width);
            dep = Integer.parseInt(depth);
            hei = Integer.parseInt(height);
        } catch (NumberFormatException ex) {
            ex.printStackTrace();
        }
        // Create a GLSurfaceView instance and set it
        // as the ContentView for this Activity
        mGLView = new MyGLSurfaceView(this);
        setContentView(mGLView);
    }
    @Override
    protected void onPause() {
        super.onPause();
        // The following call pauses the rendering thread.
        // If your OpenGL application is memory intensive,
        // you should consider de-allocating objects that
        // consume significant memory here.
        mGLView.onPause();
    }
    @Override
    protected void onResume() {
        super.onResume();
        // The following call resumes a paused rendering thread.
        // If you de-allocated graphic objects for onPause()
        // this is a good place to re-allocate them.
        mGLView.onResume();
    }
}