package com.example.computing.stillfacedetection;

/**
 * Created by Computing on 12/08/2016.
 */

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.MediaStore;

import java.io.File;
import java.io.IOException;
import java.lang.Exception;
import java.lang.String;
import java.util.concurrent.Callable;

public class OpenCVFaceDetectCallable implements Callable {
    String uriString;
    Context context;
    OpenCVFaceDetector opd;
    public OpenCVFaceDetectCallable(String uriString, Context c, OpenCVFaceDetector opd){
        this.uriString = uriString;
        this.context = c;
        this.opd = opd;
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

            result = opd.detectFace(bmp);

        } catch (IOException ioe) {
            System.err.println(ioe);
            result = false;
        }
        return result;
    }
}
