package com.example.computing.stillfacedetection; /**
 * Created by Computing on 09/08/2016.
 */

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.SparseArray;

import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Landmark;


public class MobileVisionDetector {
    Canvas c;
    FaceDetector detector;
    Bitmap mutableBmp;
    public boolean initialise(Context context){
        detector = new FaceDetector.Builder(context)
                .setTrackingEnabled(false)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS).build();
        return true;

    }
    public boolean detectFace(Bitmap bmp){
        // We have to convert the working bitmap, bmp, into a mutable one.
        mutableBmp = bmp.copy(Bitmap.Config.ARGB_8888, true);
        Frame frame = new Frame.Builder().setBitmap(mutableBmp).build();
        SparseArray<Face> faces = detector.detect(frame);
        c = new Canvas(mutableBmp);
        if (faces.size() < 1) {
            return false;
        } else {
            for (int i = 0; i < faces.size(); i++){
                Face face = faces.valueAt(i);
                Paint paint = new Paint();
                paint.setStyle(Paint.Style.STROKE);
                paint.setColor(Color.WHITE);
                paint.setStrokeWidth(20);

                // Draws rectangles around each face.
                c.drawRect((face.getPosition().x),
                        (face.getPosition().y),
                        (face.getPosition().x + face.getWidth()),
                        (face.getPosition().y + face.getHeight()), paint);
                for (Landmark landmark : face.getLandmarks()){

                    int cx = (int) ((landmark.getPosition().x));
                    int cy = (int) ((landmark.getPosition().y));
                    c.drawCircle(cx, cy, 2, paint);
                    System.out.println(landmark.getPosition().x);
                }
            }
            return true;
        }

    }
    public Bitmap getBitmap(){
        return mutableBmp;
    }

}
