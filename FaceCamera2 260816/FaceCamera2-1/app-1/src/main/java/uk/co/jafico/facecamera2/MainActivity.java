package uk.co.jafico.facecamera2;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.media.ToneGenerator;
import android.os.CountDownTimer;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class MainActivity extends AppCompatActivity
        implements CameraBridgeViewBase.CvCameraViewListener2{
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private JavaCameraView mJavaCameraView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mJavaCameraView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private int mCameraID = 0;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    private final int MY_PERMISSIONS_REQUEST_CAMERA = 568;
    private final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 56;

    private CascadeClassifier faceClassifier;
    private CascadeClassifier eyeClassifier;
    private CascadeClassifier mouthClassifier;
    private CascadeClassifier noseClassifier;

    Mat mRgba;

    // This is a flag to tell if face detection is on or not.
    private Boolean detectOn = false;


    private BlockingQueue<CameraBridgeViewBase.CvCameraViewFrame> framesIn
            = new LinkedBlockingQueue<>(5);
    private BlockingQueue<Mat> framesOut = new LinkedBlockingQueue<>(5);

    // This is the ORB feature detector that we will be using to find corners of mouths.
    FeatureDetector orb;
    DescriptorExtractor desEx;

    // This is the text view that will be used to count doen for tongue
    // segmentation
    TextView countDown;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mJavaCameraView = (JavaCameraView) findViewById(R.id.camera_view);


        // Set up the user interaction to manually show or hide the system UI.
        mJavaCameraView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        mJavaCameraView.enableFpsMeter();

        countDown = (TextView) findViewById(R.id.text_count_down);

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);

        // We will check if camera permission has been granted, and if not, ask
        // the user for permission first.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    MY_PERMISSIONS_REQUEST_CAMERA);
        }

        // We will also check if write permission is granted, and if not, ask
        // for permission.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
        }

        if (!OpenCVLoader.initDebug()){
            Log.e("MainActivity-OpenCV", "OpenCV has not loaded.");
            finish();

        } else {
            // OpenCV has loaded correctly
            Log.i("MainActivity-OpenCV", "OpenCV has loaded successfully!");
        }

        // We will check media availability, to see if we can use the face
        // classifiers.
        if (checkMediaAvailability()){
            // Check external storage, write classifiers if necessary.
            File rootDirectory = getExternalFilesDir(null);
            File faceClassifierFile = new File(rootDirectory, "faceClassifier.xml");
            if (!faceClassifierFile.exists()) {
                InputStream i = getResources().openRawResource(R.raw.face_classifier);
                OutputStream out = null;
                try {
                    out = new FileOutputStream(faceClassifierFile);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                try {

                    byte[] buffer = new byte[10 * 1024];
                    int read;

                    while ((read = i.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                    out.flush();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
            File eyeClassifierFile = new File(rootDirectory, "eyeClassifier.xml");
            if (!eyeClassifierFile.exists()) {
                InputStream i = getResources().openRawResource(R.raw.eye_classifier);
                OutputStream out = null;
                try {
                    out = new FileOutputStream(eyeClassifierFile);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                try {

                    byte[] buffer = new byte[10 * 1024];
                    int read;

                    while ((read = i.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                    out.flush();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            File mouthClassifierFile = new File(rootDirectory, "mouthClassifier.xml");
            if (!mouthClassifierFile.exists()) {
                InputStream i = getResources().openRawResource(R.raw.mouth_classifier);
                OutputStream out = null;
                try {
                    out = new FileOutputStream(mouthClassifierFile);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                try {

                    byte[] buffer = new byte[10 * 1024];
                    int read;

                    while ((read = i.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                    out.flush();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            File noseClassifierFile = new File(rootDirectory, "noseClassifier.xml");
            if (!noseClassifierFile.exists()) {
                InputStream i = getResources().openRawResource(R.raw.nose_classifier);
                OutputStream out = null;
                try {
                    out = new FileOutputStream(noseClassifierFile);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                try {

                    byte[] buffer = new byte[10 * 1024];
                    int read;

                    while ((read = i.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                    out.flush();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            faceClassifier = new CascadeClassifier(faceClassifierFile.
                    getAbsolutePath());
            eyeClassifier = new CascadeClassifier(eyeClassifierFile.
                    getAbsolutePath());
            mouthClassifier = new CascadeClassifier(mouthClassifierFile.
                    getAbsolutePath());
            noseClassifier = new CascadeClassifier(noseClassifierFile.
                getAbsolutePath());
        } else {
            AlertDialog.Builder b = new AlertDialog.Builder(this);
            b.setMessage(R.string.external_storage_error).setNeutralButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                }
            });
            b.create().show();
        }



        // We will now check the number of cameras, and if there is only one,
        // we will disable second camera.


        // We will try to initialise OpenCV, and if not, handle the error.


        mJavaCameraView.setCvCameraViewListener(this);
        mJavaCameraView.enableView();

        faceDetectWorker.start();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mJavaCameraView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        if (detectOn) {
            Mat mRgba = inputFrame.rgba();
            try {
                framesIn.put(inputFrame);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Mat outputFrame = null;
            try {
                outputFrame = framesOut.poll(1000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (outputFrame == null) {
                return mRgba;
            }
            return outputFrame;
        } else {return inputFrame.rgba();}

    }

    public void onCameraViewStopped(){
        // Do something here.
    }

    public void onCameraViewStarted(int width, int height) {
        // Do something here.
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        if (item.getItemId() == R.id.action_switch_camera) {
            // Switches the camera.
            mCameraID = mCameraID ^ 1;
            mJavaCameraView.disableView();
            mJavaCameraView.setCameraIndex(mCameraID);
            mJavaCameraView.enableView();
            return true;
        } else if (item.getItemId() == R.id.action_change_resolution) {
                mJavaCameraView.disableView();
                Camera cam = Camera.open(mCameraID);
                final List<Camera.Size> resolutions = cam.getParameters()
                        .getSupportedPreviewSizes();
                cam.release();

                // Creates an ArrayList - used to create strings of resolutions
                // for the dialog box.
                ArrayList<String> resolutionsStr = new ArrayList<String>();

                for (Camera.Size s : resolutions) {
                    resolutionsStr.add(s.width + "x" + s.height);
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.action_change_resolution)
                        .setItems(resolutionsStr.toArray(
                                new CharSequence[resolutions.size()]),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface d, int i) {
                                        mJavaCameraView.setMaxFrameSize(
                                                resolutions.get(i).width,
                                                resolutions.get(i).height);
                                        mJavaCameraView.enableView();
                                    }
                                });

                AlertDialog d = builder.create();
                d.show();

        } else if (item.getItemId() == R.id.action_toggle_face_detection) {
            detectOn = !detectOn;
            return true;
        }

        return false;
    }


    // This method checks if external storage is available for read and write.
    private boolean checkMediaAvailability() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        } else {return false;}
    }

    Thread faceDetectWorker = new Thread() {
        @Override
        public void run() {
            while (true) {
                try {
                    CameraBridgeViewBase.CvCameraViewFrame inputFrame =
                            framesIn.poll(1000, TimeUnit.MILLISECONDS);
                    if (inputFrame == null) {
                        // timeout
                        continue;
                    }

                    Mat rgba = inputFrame.rgba();
                    Mat gray = inputFrame.gray();
                    Mat out = new Mat();

                    MatOfRect faces = new MatOfRect();
                    faceClassifier.detectMultiScale(gray, faces);
                    if (!(faces.toArray().length < 1)) {
                        for (Rect rect : faces.toArray()) {
                            Rect mouthRect = new Rect(rect.x + rect.width / 4, rect.y + rect.height / 3  * 2,
                                    rect.width / 2, rect.height / 3);
                            Imgproc.rectangle(rgba, new Point(mouthRect.x, mouthRect.y), new Point(mouthRect.x + mouthRect.width,
                                    mouthRect.y + mouthRect.height), new Scalar(0,0,155));

                            Mat mouthMat = rgba.submat(mouthRect);
                            orb = FeatureDetector.create(FeatureDetector.ORB);
                            desEx = DescriptorExtractor.create(DescriptorExtractor.ORB);
                            System.out.println(mouthMat.size());
                            MatOfKeyPoint kp = new MatOfKeyPoint();

                            orb.detect(mouthMat, kp);
                            // We will now retrieve the most extreme left
                            // keypoint and draw it on the face.
                            KeyPoint[] keyPoints = kp.toArray();

                            Point leftPoint = new Point(1000000, 1000000);
                            for (KeyPoint keyPoint : keyPoints){
                                if (keyPoint.pt.x < leftPoint.x) {
                                    leftPoint = keyPoint.pt;
                                }
                                Imgproc.circle(rgba, keyPoint.pt, 2,
                                        new Scalar(255,255,255));
                            }

                            // We will add the offset so it is centered on the mouth.
                            leftPoint.x += rect.x + rect.width / 4;
                            leftPoint.y += rect.y + rect.height / 3 * 2;

                            Imgproc.circle(rgba, leftPoint, 3,
                                    new Scalar(255,0,255));

                            Point rightPoint = new Point(0, 0);
                            for (KeyPoint keyPoint : keyPoints){
                                if (keyPoint.pt.x > rightPoint.x) {
                                    rightPoint = keyPoint.pt;
                                }
                            }

                            // We will add the offset so it is centered on the mouth.
                            rightPoint.x += rect.x + rect.width / 4;
                            rightPoint.y += rect.y + rect.height / 3 * 2;
                            System.out.println(rightPoint.x+ ","+ rightPoint.y);
                            Imgproc.circle(rgba, rightPoint, 10,
                                    new Scalar(0,255,255));

                        }

                    }

                    framesOut.put(rgba);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    public void segmentTongue(View v) throws InterruptedException{
        // We will ask user to wait 5 seconds, and ask them to stick out
        // tongue.
        detectOn = true;
        new CountDownTimer(5000, 1000) {
            public void onTick(long millisUntilFinished){
                countDown.setText(getString(R.string.count_down, millisUntilFinished/1000));
            }
            public void onFinish() {
                CameraBridgeViewBase.CvCameraViewFrame frame = null;
                try {
                    faceDetectWorker.interrupt();
                    frame = framesIn.poll(2000, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                assert frame != null;
                Mat rgbaFrame = frame.rgba();
                String imagePath = getExternalFilesDir(null) + "/image.bmp";
                Imgcodecs.imwrite(imagePath, rgbaFrame);
                Intent intent = new Intent(getApplicationContext(),
                        TongueSegmentationActivity.class);
                intent.putExtra("imagePath", imagePath);
                startActivity(intent);
            }
        }.start();


    }
}

