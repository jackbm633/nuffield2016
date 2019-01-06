package uk.co.jafico.facecamera2;

import org.opencv.core.Mat;
import org.opencv.objdetect.CascadeClassifier;

import java.util.concurrent.Callable;

/**
 * Created by Computing on 19/08/2016.
 */

public class FaceDetectCallable implements Callable {

        private long waitTime;
        private Mat rgba;
        private Mat gray;
        private CascadeClassifier faceClassifier;

        public FaceDetectCallable(int timeInMillis, Mat rgba, Mat gray,
                                  CascadeClassifier faceClassifier) {
            this.waitTime = timeInMillis;
            this.rgba = rgba;
            this.gray = gray;
            this.faceClassifier = faceClassifier;
        }

        @Override
        public Mat call() throws Exception {
            return rgba;
        }
}
