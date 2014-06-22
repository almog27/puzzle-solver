package com.client.puzzleSolver;

//********************************************************************************************
//Image processing on mobile
//15 Puzzle solver
//Author: Almog Ben David
//********************************************************************************************/

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends Activity implements CalculationListener {

    private static final String TAG = "15PuzzleSolver";

    private Preview preview;
    private ResultView resultView;
    private Context mContext = this;
    private Menu menu;
    private CalculationState calculationState;

    /**
     * This variable determine if the application is in production mode or development mode
     * By using this variable you can determine if to see the deployment steps buttons
     */
    private boolean isProductionMode = true;

    private MenuItem menuItemProductionMode,
            menuItemOrigImage, menuItemCanny,
            menuItemHoughAllLines,
            menuItemPerspective,
            menuItemFull;

    private final static String INPUT_IMG_FILENAME = "/temp.jpg"; //name for storing image captured by camera view

    //flag to check if camera is ready for capture
    private boolean cameraReadyFlag = true;

    // Called when the activity is first created.
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //make the screen full screen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //remove the title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        resultView = new ResultView(this);
        preview = new Preview(this);
        calculationState = CalculationState.FULL_CALCULATION;

        //set Content View as the preview
        setContentView(preview);

        //add result view  to the content View
        addContentView(resultView, new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT));

        //set the orientation as landscape
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_8, this, openCVCallBack);
    }

    // Called when shutter is opened
    ShutterCallback shutterCallback = new ShutterCallback() {
        public void onShutter() {
        }
    };

    // Handles data for raw picture
    PictureCallback rawCallback = new PictureCallback() {
        @Override
        public void onPictureTaken(byte[] arg0, android.hardware.Camera arg1) {
        }
    };

    //store the image as a jpeg image
    public boolean compressByteImage(byte[] imageData,
                                     int quality) {
        File sdCard = Environment.getExternalStorageDirectory();
        FileOutputStream fileOutputStream = null;

        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 1;    //no downsampling
            Bitmap myImage = BitmapFactory.decodeByteArray(imageData, 0,
                    imageData.length, options);
            fileOutputStream = new FileOutputStream(
                    sdCard.toString() + INPUT_IMG_FILENAME);

            BufferedOutputStream bos = new BufferedOutputStream(
                    fileOutputStream);

            //compress image to jpeg
            myImage.compress(CompressFormat.JPEG, quality, bos);

            bos.flush();
            bos.close();
            fileOutputStream.close();

        } catch (FileNotFoundException e) {
            Log.e(TAG, "FileNotFoundException");
            e.printStackTrace();
        } catch (IOException e) {
            Log.e(TAG, "IOException");
            e.printStackTrace();
        }
        return true;
    }

    // Handles data for jpeg picture
    PictureCallback jpegCallback = new PictureCallback() {
        @Override
        public void onPictureTaken(byte[] imageData, android.hardware.Camera camera) {
            if (imageData != null) {
                Intent mIntent = new Intent();
                // compress image
                compressByteImage(imageData, 75);
                setResult(0, mIntent);

                //** Send image and offload image processing task to calculations async task **
                CalculationState processingCalcState = isProductionMode ?
                        CalculationState.FULL_CALCULATION:
                        calculationState;
                CalculationsTask task = new CalculationsTask(mContext, MainActivity.this, processingCalcState);
                task.execute(Environment.getExternalStorageDirectory().toString() + INPUT_IMG_FILENAME);

                //start the camera view again
                camera.startPreview();
            }
        }
    };

    //*******************************************************************************
    //UI
    //*******************************************************************************
    //onKeyDown is used to monitor button pressed and facilitate the switching of views
    @Override
    public boolean onKeyDown(int keycode, KeyEvent event) {
        //check if the camera button is pressed
        if (keycode == KeyEvent.KEYCODE_CAMERA) {
            //if result
            if (resultView.IsShowingResult) {
                resultView.IsShowingResult = false;
            } else if (cameraReadyFlag) {//switch to camera view
                cameraReadyFlag = false;
                preview.camera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {
                        preview.camera.takePicture(shutterCallback, rawCallback, jpegCallback);
                    }
                });
            }
            return true;
        }
        return super.onKeyDown(keycode, event);
    }

    @Override
    public void onBackPressed() {
        if (resultView.IsShowingResult) {
            resultView.IsShowingResult = false;
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {  // finger touches the screen
            if (resultView.IsShowingResult) {
                resultView.IsShowingResult = false;
            } else if (cameraReadyFlag) {//switch to camera view
                cameraReadyFlag = false;
                preview.camera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {
                        preview.camera.takePicture(shutterCallback, rawCallback, jpegCallback);
                    }
                });
            }
        }
        return true;
    }

    @Override
    public void onCameraReadyStateChange(boolean state) {
        cameraReadyFlag = state;
    }

    @Override
    public void onImageReady(Bitmap bitmap) {
        resultView.resultImage = bitmap;
        resultView.IsShowingResult = true;
    }

    //*******************************************************************************
    // Open CV handling and callbacks
    //*******************************************************************************
    private BaseLoaderCallback openCVCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    break;
                }
                default: {
                    super.onManagerConnected(status);
                    break;
                }
            }
        }
    };

    //*******************************************************************************
    // Menu and menu items handling
    //*******************************************************************************
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        createMenu();
        return true;
    }

    private void createMenu() {
        menu.clear();
        if (isProductionMode) {
            menuItemProductionMode = menu.add("Debug mode");
        } else {
            menuItemProductionMode = menu.add("Production mode");
            menuItemOrigImage = menu.add(CalculationState.ORIGINAL_IMAGE.toString());
            menuItemCanny = menu.add(CalculationState.CANNY_EDGE.toString());
            menuItemHoughAllLines = menu.add(CalculationState.HOUGH_TRANSFORM_DRAW.toString());
            menuItemPerspective = menu.add(CalculationState.PERSPECTIVE_TRANSFORM.toString());
            menuItemFull = menu.add(CalculationState.FULL_CALCULATION.toString());
        }

        new Handler().postDelayed(new Runnable() {
            public void run() {
                openOptionsMenu();
            }
        }, 100);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.equals(menuItemProductionMode)) {
            isProductionMode = !isProductionMode;
            createMenu();
        } else if (item.equals(menuItemOrigImage))
            calculationState = CalculationState.ORIGINAL_IMAGE;
        else if (item.equals(menuItemCanny))
            calculationState = CalculationState.CANNY_EDGE;
        else if (item.equals(menuItemHoughAllLines))
            calculationState = CalculationState.HOUGH_TRANSFORM_DRAW;
        else if (item.equals(menuItemPerspective))
            calculationState = CalculationState.PERSPECTIVE_TRANSFORM;
        else if (item.equals(menuItemFull))
            calculationState = CalculationState.FULL_CALCULATION;
        return true;
    }
}