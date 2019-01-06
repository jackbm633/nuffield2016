package uk.co.jafico.facecamera2;

import android.graphics.Bitmap;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class TongueSegmentationActivity extends AppCompatActivity {
    private Mat rgbaFrame;
    private ImageView edgesView;
    private ImageView originalView;
    private Mat rgbaFrameAlt;
    private Mat edges;
    private Boolean tongueInFocus = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tongue_segmentation);
        Bundle extras  = getIntent().getExtras();
        String imagePath = extras.getString("imagePath");
        rgbaFrame = Imgcodecs.imread(imagePath);
        Bitmap originalBitmap = Bitmap.createBitmap(rgbaFrame.width(),
                rgbaFrame.height(), Bitmap.Config.ARGB_8888);
        originalView = (ImageView) findViewById(R.id.original_view);
        edgesView = (ImageView) findViewById(R.id.edges_view);
        final SeekBar thresholdBar = (SeekBar) findViewById(
                R.id.canny_threshold_bar);
        final TextView thresholdText = (TextView) findViewById(
                R.id.canny_threshold_text);
        thresholdBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i,
                                                  boolean b) {
                        thresholdText.setText(getString(
                                R.string.canny_threshold,
                                thresholdBar.getProgress()));
                        edges = edgeDetect(rgbaFrame, thresholdBar.getProgress());
                        Bitmap edgesBitmap = Bitmap.createBitmap(edges.width(),
                                edges.height(), Bitmap.Config.ARGB_8888);
                        Utils.matToBitmap(edges, edgesBitmap);
                        edgesView.setImageBitmap(edgesBitmap);


                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });
                Utils.matToBitmap(rgbaFrame, originalBitmap);
        originalView.setImageBitmap(originalBitmap);




        edges = edgeDetect(rgbaFrame, 1);
        Bitmap edgesBitmap = Bitmap.createBitmap(edges.width(),
                edges.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(edges, edgesBitmap);
        edgesView.setImageBitmap(edgesBitmap);

    }

    protected Mat edgeDetect(Mat orig, int threshold){
        // We will first convert the RGB orig to grayscale, as this is what
        // the Canny algorithm is designed to work with.
        Mat gray = new Mat();
        Imgproc.cvtColor(orig, gray, Imgproc.COLOR_BGR2GRAY);
        edges = new Mat();
        Imgproc.Canny(gray, edges, threshold, threshold*3, 5, true);
        return edges;

    }

    @Override
    public boolean onCreateOptionsMenu(Menu m) {
        super.onCreateOptionsMenu(m);
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_tongue_segmentation, m);
        return true;
    }

    public void changeFocus(MenuItem menuItem) {
        if (!tongueInFocus) {
            // In this method, we will detect the face in the bitmap, and only
            // focus on the tongue.
            // We will first initialise the CascadeClassifier for the face.
            File rootDirectory = getExternalFilesDir(null);
            File faceClassifierFile = new File(rootDirectory,
                    "faceClassifier.xml");
            if (!faceClassifierFile.exists()) {
                InputStream i = getResources().openRawResource(
                        R.raw.face_classifier);
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
            CascadeClassifier faceClassifier = new CascadeClassifier(
                    faceClassifierFile.getAbsolutePath());
            MatOfRect faces = new MatOfRect();
            faceClassifier.detectMultiScale(rgbaFrame, faces);
            if (faces.toArray().length > 0){
                for (Rect face : faces.toArray()) {
                    Mat tongueMat = rgbaFrame.submat( face.y + (face.width/3) * 2,
                            face.y + face.height, face.x + face.width/4,
                            face.x + (face.width / 4) * 3);
                    rgbaFrameAlt = rgbaFrame;
                    rgbaFrame = tongueMat;
                    edges = edgeDetect(rgbaFrame, 1);
                    Bitmap edgesBitmap = Bitmap.createBitmap(edges.width(),
                            edges.height(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(edges, edgesBitmap);
                    edgesView.setImageBitmap(edgesBitmap);

                    Bitmap originalBitmap = Bitmap.createBitmap(rgbaFrame.width(),
                            rgbaFrame.height(), Bitmap.Config.ARGB_8888);

                    Utils.matToBitmap(rgbaFrame, originalBitmap);
                    originalView.setImageBitmap(originalBitmap);
                    tongueInFocus = !tongueInFocus;
                }
            } else {
                // We couldn't find a face in the image.
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.face_error);
                builder.setIcon(R.drawable.ic_error_white_24dp);
                builder.show();
            }
        }else {
            Mat tmp = rgbaFrame;
            rgbaFrame = rgbaFrameAlt;
            rgbaFrameAlt = tmp;
            edges = edgeDetect(rgbaFrame, 1);
            Bitmap edgesBitmap = Bitmap.createBitmap(edges.width(),
                    edges.height(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(edges, edgesBitmap);
            edgesView.setImageBitmap(edgesBitmap);

            Bitmap originalBitmap = Bitmap.createBitmap(rgbaFrame.width(),
                    rgbaFrame.height(), Bitmap.Config.ARGB_8888);

            Utils.matToBitmap(rgbaFrame, originalBitmap);
            originalView.setImageBitmap(originalBitmap);
            tongueInFocus = !tongueInFocus;
        }
    }


    protected void segmentTongue(MenuItem i) {
        List<MatOfPoint> contours = new ArrayList<>();
        Mat tmp = edges;
        Imgproc.findContours(edges, contours, new Mat(), Imgproc.RETR_CCOMP,
                Imgproc.CHAIN_APPROX_TC89_L1);
        Imgproc.drawContours(edges, contours, 0, new Scalar(255,0,255), 3);
        Bitmap edgesBitmap = Bitmap.createBitmap(edges.width(),
                edges.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(edges, edgesBitmap);
        edgesView.setImageBitmap(edgesBitmap);
    }

}
