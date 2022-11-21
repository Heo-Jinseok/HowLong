package com.example.project123;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;
/**
 * A view container where OpenGL ES graphics can be drawn on screen.
 * This view can also be used to capture touch events, such as a user
 * interacting with drawn objects.
 */
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

public class MyGLSurfaceView extends GLSurfaceView {
    private final MyGLRenderer mRenderer;
    final static float DRAGSPEED = 5;

    public MyGLSurfaceView(Context context) {
        super(context);

        setEGLContextClientVersion(2);

        mRenderer = new MyGLRenderer();
        setRenderer(mRenderer);

        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    private final float TOUCH_SCALE_FACTOR = 180.0f / 320;
    private float mPreviousX;
    private float mPreviousY;

    @Override
    public boolean onTouchEvent(MotionEvent e) {

        float x = e.getX();
        float y = e.getY();
        switch (e.getAction()) {
            case MotionEvent.ACTION_MOVE:
                float dx = (x - mPreviousX)/DRAGSPEED;
                float dy = (y - mPreviousY)/DRAGSPEED;

                if (y > getHeight() / 2) {
                    dx = dx * -1;
                }

                if (x < getWidth() / 2) {
                    dy = dy * -1;
                }
                mRenderer.setAngle(
                        mRenderer.getAngle() +
                                ((dx + dy) * TOUCH_SCALE_FACTOR));
                requestRender();
        }
        mPreviousX = x;
        mPreviousY = y;
        return true;
    }
}