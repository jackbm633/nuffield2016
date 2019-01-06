package uk.co.jafico.facecamera2;

import org.opencv.core.Mat;
import org.opencv.objdetect.CascadeClassifier;

/**
 * Created by Computing on 19/08/2016.
 */

class FaceDetectParams {
    Mat rgba;
    Mat gray;
    CascadeClassifier faceClassifier;
    CascadeClassifier eyeClassifier;

    FaceDetectParams(Mat rgba, Mat gray, CascadeClassifier faceClassifier) {
        this.rgba = rgba;
        this.gray = gray;
        this.faceClassifier = faceClassifier;
        this.eyeClassifier = eyeClassifier;
    }

    public Mat getRgba() {
        return rgba;
    }

    public Mat getGray() {
        return gray;
    }

    public CascadeClassifier getFaceClassifier() {
        return faceClassifier;
    }

    public CascadeClassifier getEyeClassifier() {
        return eyeClassifier;
    }
}
