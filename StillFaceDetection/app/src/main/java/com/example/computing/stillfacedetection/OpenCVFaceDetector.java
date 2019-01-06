package com.example.computing.stillfacedetection;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import static android.os.Environment.getExternalStorageDirectory;

public class OpenCVFaceDetector {
    // These are variables for all of the Haar cascades we will be using to
    // detect the face with.
    CascadeClassifier faceClassifier;
    CascadeClassifier eyeClassifier;
    CascadeClassifier smileClassifier;
    Mat img;
    public boolean initialise(Context context) {
        if (!OpenCVLoader.initDebug()){
            return false;
        }
        // The Haar classifiers will now be initialised.
        System.out.println(getExternalStorageDirectory());
        faceClassifier = new CascadeClassifier(
                getExternalStorageDirectory().
                        getAbsolutePath() +"/faceClassifier.xml");
        if (faceClassifier.empty()){
            System.err.println("Empty");
        }
        eyeClassifier = new CascadeClassifier(getExternalStorageDirectory().
                getAbsolutePath() +"/eyeClassifier.xml");
        smileClassifier = new CascadeClassifier(
                getExternalStorageDirectory().
                        getAbsolutePath() +"/smileClassifier.xml");
        return true;
    }

    public boolean detectFace(Bitmap bmp){
        // We will load the bitmap in both grayscale and color modes, for
        // processing and output respectively.
        img = new Mat(bmp.getHeight(), bmp.getWidth(), CvType.CV_8UC3);
        Utils.bitmapToMat(bmp, img);
        Mat gray = new Mat(bmp.getHeight(), bmp.getWidth(), CvType.CV_8UC3);
        Imgproc.cvtColor(img, gray, Imgproc.COLOR_BGR2GRAY);

        // We will now detect the eyes on the face.
        MatOfRect eyes = new MatOfRect();
        eyeClassifier.detectMultiScale(gray, eyes);
        if (eyes.toArray().length > 1) {
            for (Rect rect : eyes.toArray()) {
                Imgproc.rectangle(img,new Point(rect.x, rect.y), new Point(rect.x + rect.width,
                        rect.y + rect.height), new Scalar(0, 0, 255), 2);
            }
        }

        // We will now detect the smiles on the face.
        MatOfRect smiles = new MatOfRect();
        smileClassifier.detectMultiScale(gray, smiles);
        if (smiles.toArray().length > 1) {
            for (Rect rect : smiles.toArray()) {
                Imgproc.rectangle(img,new Point(rect.x, rect.y), new Point(rect.x + rect.width,
                        rect.y + rect.height), new Scalar(0, 255, 0), 2);
            }
        }

        // We will now detect the faces and draw rectangles around them.
        MatOfRect faces = new MatOfRect();
        faceClassifier.detectMultiScale(gray, faces);
        if (faces.toArray().length < 1) {
            return false;
        } else {
            for (Rect rect : faces.toArray()) {
                Imgproc.rectangle(img,new Point(rect.x, rect.y), new Point(rect.x + rect.width,
                        rect.y + rect.height), new Scalar(255, 0, 0), 10);
            }
            return true;
        }



    }
    public Bitmap getBitmap(){
        System.out.println(img.height());
        System.out.println(img.width());
        Bitmap bmp = Bitmap.createBitmap(img.width(), img.height(), Bitmap.Config.ARGB_8888);
        System.out.println(bmp.getHeight());
        System.out.println(bmp.getWidth());
        Utils.matToBitmap(img, bmp);
        return bmp;
    }
}