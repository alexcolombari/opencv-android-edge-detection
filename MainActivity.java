package com.opencvcannyedge;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.renderscript.Sampler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AbsSeekBar;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.FpsMeter;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.lang.reflect.Field;

import static org.opencv.core.CvType.CV_8UC1;
import static org.opencv.imgproc.Imgproc.Canny;
import static org.opencv.imgproc.Imgproc.threshold;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    // DETERMINE THE MIN AND MAX VALUES OF SEEKBAR
    SeekBar seekBar;
    int min = 150, max = 230, current = 115;

    TextView seekBarValue;

    // DETERMINE THE INITIAL VALUES OF CANNY THRESHOLD AND BLUR SIGMA
    private int thresholdCanny = 170;
    private int thresholdCanny2 = 190;
    private int blurSigmaX = 90;
    private int blurSigmaY = 80;

    // ANDROID PERMISSIONS
    public final int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 1;
    private static String READ_EXTERNAL_STORAGE_PERMISSION = android.Manifest.permission.READ_EXTERNAL_STORAGE;
    private static String WRITE_EXTERNAL_STORAGE_PERMISSION = android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

    private static final String TAG = "OCVSample::Activity";

    private CameraBridgeViewBase mOpenCvCameraView;
    int w, h;

        // LOAD LIBRARY
    static {
        System.loadLibrary("opencv_java3");
    }

    public void OpenCVInitLoad() {
        if (!OpenCVLoader.initDebug()) {
            Log.d("", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
        } else {
            Log.d("", "OpenCV library found inside package. Using it!");
        }
    }

    // LOOKING FOR OpenCV MANAGER
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // CREATE JavaCameraView AND APPLY SPECIFIC FLAGS FOR FULLSCREEN, ETC...
        OpenCVInitLoad();
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.java_surface_view_1);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getSupportActionBar().hide(); //hide the title bar
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkMultiplePermissions(REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS, MainActivity.this);
        }

        // CREATE SEEKBAR
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        // SET "MAX" FOR THE MAXIMUM VALUE OF SEEK BAR - SET THE "CURRENT" VARIABLE VALUE TO GET THE CURRENT PROGRESS
        seekBar.setMax(max); // seekBar.setMax(max - min);
        seekBar.setProgress(current); // seekBar.setProgress(current - min);
        // CREATE AN VARIABLE THAT RECEIVE THE CURRENT PROGRESS BAR VALUE
        seekBarValue = (TextView)findViewById(R.id.textView);
    }

    //CHECK FOR CAMERA AND STORAGE ACCESS PERMISSIONS
    @TargetApi(Build.VERSION_CODES.M)
    private void checkMultiplePermissions(int permissionCode, Context context) {

        String[] PERMISSIONS = {android.Manifest.permission.CAMERA, READ_EXTERNAL_STORAGE_PERMISSION, WRITE_EXTERNAL_STORAGE_PERMISSION,
                android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.READ_PHONE_STATE};
        if (!hasPermissions(context, PERMISSIONS)) {
            ActivityCompat.requestPermissions((Activity) context, PERMISSIONS, permissionCode);
        }
    }

    private boolean hasPermissions(Context context, String... permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                    // CALL CAMERA HERE
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        //  stopPreview();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //stopPreview();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        w = width;
        h = height;
    }

    public void onCameraViewStopped() {
    }

    @Override
    public Mat onCameraFrame(final CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        // DEFINE THE ORIGINAL InputFrame AS RGBA VALUES
        Mat rgba = inputFrame.rgba();
        Mat edges = new Mat(rgba.size(), CV_8UC1);

        // CURRENTLY SEEKBAR CHANGE VALUE
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override                                                 // boolean fromUser
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(progress <= 170){
                    blurSigmaX = progress + 90;
                    blurSigmaY = progress + 110;
                } else{
                    blurSigmaX = progress - 10;  //-20
                    blurSigmaY = progress - 15;  // -15
                }

                thresholdCanny = progress + 40;
                thresholdCanny2 = progress + 120;

                // TEXT VIEW RETURN SEEK BAR CURRENT VALUE
                seekBarValue.setText(String.valueOf(progress));

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        // IMAGE PROCESSING
        // Input -> Gray Scale -> Apply Gaussian Blur -> Apply Canny Edge Detector -> Return
        try {
            Imgproc.cvtColor(rgba, edges, Imgproc.COLOR_RGB2GRAY, 4);
            Imgproc.GaussianBlur(edges, edges, new Size(3, 3), blurSigmaX, blurSigmaY);
            Canny(edges, edges, thresholdCanny, thresholdCanny2);
        } catch (Exception e) {
            Log.i(TAG, e.toString());
            e.printStackTrace();
        }
    
        return edges;
    }
}