package uk.co.jafico.facecamera2;

import android.os.AsyncTask;
import android.provider.Settings;

import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

/**
 * Created by Computing on 19/08/2016.
 */

public class FaceDetectTask extends AsyncTask<FaceDetectParams, Void, Mat> {
    @Override
    protected Mat doInBackground(FaceDetectParams... params) {
        FaceDetectParams params1 = params[0];
        Mat gray = params1.getGray();
        Mat rgba = params1.getRgba();
        CascadeClassifier faceClassifier = params1.getFaceClassifier();

        MatOfRect faces = new MatOfRect();
        faceClassifier.detectMultiScale(gray, faces);
        if (faces.toArray().length > 1) {
            for (Rect rect : faces.toArray()) {
                Imgproc.rectangle(rgba,new Point(rect.x, rect.y), new Point(rect.x + rect.width,
                        rect.y + rect.height), new Scalar(0, 0, 255), 2);
            }
        }

        return rgba;


    }

    @Override
    protected void onPostExecute(Mat result) {
        System.out.println("PostExec");

    }

    @Override
    protected void onPreExecute() {
        System.out.println("PreExec");

    }

}
