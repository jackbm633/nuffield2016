package com.example.computing.stillfacedetection;

/**
 * Created by Computing on 11/08/2016.
 */
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.MediaStore;

import java.io.File;
import java.io.IOException;
import java.lang.Runnable;

public class BatchRunnable implements Runnable {

    Thread processThread;
    String workingUriString;
    Context context;
    MobileVisionDetector mvd;
    OpenCVFaceDetector opd;

    BatchRunnable(String uriString, Context c) {
        processThread = new Thread(this, "batch thread");
        System.out.println("Batch thread created.");
        workingUriString = uriString;
        context = c;
        if ( PreferenceManager.getDefaultSharedPreferences(context)
                .getString("face_detect_engine", "Google")
                .equals("Google")) {
            mvd = new MobileVisionDetector();
            mvd.initialise(context);
        } else {
            opd = new OpenCVFaceDetector();
            opd.initialise(context);
        }
        processThread.start();


    }

    public void run() {
        Uri uri = Uri.fromFile(new File(workingUriString));
        Boolean result = false;
        System.out.println("Loading " + workingUriString);
        try {
            Bitmap bmp = MediaStore.Images.Media.getBitmap(
                    context.getContentResolver(), uri);
            if ( PreferenceManager.getDefaultSharedPreferences(context)
                    .getString("face_detect_engine", "Google")
                    .equals("Google")) {
                result = mvd.detectFace(bmp);
            } else {
                result = opd.detectFace(bmp);
            }
        } catch (IOException ioe) {
            System.err.println(ioe);
        }

            /** try {
                processImageCall call = BatchTestActivity.processImageCalls.take();
                String res = uri.toString() + " " + result.toString();
                call.setResult(res);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } */

    }

}
