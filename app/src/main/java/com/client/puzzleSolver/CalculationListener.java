package com.client.puzzleSolver;

import android.graphics.Bitmap;

public interface CalculationListener {
    public void onCameraReadyStateChange(boolean state);

    public void onImageReady(Bitmap bitmap);
}
