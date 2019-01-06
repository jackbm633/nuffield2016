package com.example.computing.stillfacedetection;

/**
 * Created by Computing on 12/08/2016.
 */

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

public class MobileVisionFaceDetectCallable implements Callable {
    String uriString;
    Context context;
    MobileVisionDetector mvd;
    public MobileVisionFaceDetectCallable(String uriString, Context c, MobileVisionDetector mvd){
        this.uriString = uriString;
        this.context = c;
        this.mvd = mvd;
    }

    @Override
    public String call() throws Exception {
        return uriString + " detected: " + detectFacesfromUri(uriString).toString();
    }

    private Boolean detectFacesfromUri(String UriString){
        Uri uri = Uri.fromFile(new File(UriString));
        Boolean result = false;
        try {
            Bitmap bmp = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);

            result = mvd.detectFace(bmp);

        } catch (IOException ioe) {
            System.err.println(ioe);
            result = false;
        }
        return result;
    }
}
