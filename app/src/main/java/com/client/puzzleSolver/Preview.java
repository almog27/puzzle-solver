package com.client.puzzleSolver;

//********************************************************************************************
//Image processing on mobile
//15 Puzzle solver
//Author: Almog Ben David
//********************************************************************************************/

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

public class Preview extends SurfaceView implements SurfaceHolder.Callback {
    SurfaceHolder mHolder;
    public Camera camera;

    Preview(Context context) {
        super(context);

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
//        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        //TODO: Consider removing
    }

    // Called once the holder is ready
    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, acquire the camera and tell it where
        // to draw.

        camera = Camera.open();

        try {
            camera.setPreviewDisplay(holder);
            //TODO: Check if this helps in something
//            Parameters parameters = camera.getParameters();
//            parameters.setFlashMode(Parameters.FLASH_MODE_ON);
//            camera.setParameters(parameters);
            camera.setPreviewCallback(new PreviewCallback() {
                // Called for each frame previewed
                public void onPreviewFrame(byte[] data, Camera camera) {
                    Preview.this.invalidate();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Called when the holder is destroyed
    public void surfaceDestroyed(SurfaceHolder holder) {
        camera.stopPreview();
        camera.setPreviewCallback(null);
        camera.release();
        camera = null;
    }

    // Called when holder has changed
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        camera.startPreview();
    }
}
