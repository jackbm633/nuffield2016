package com.example.computing.stillfacedetection;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;

public class MainActivity extends AppCompatActivity {
    Bitmap bmp;
    private final int RESULT_LOAD_IMAGE = 1;
    ImageView iv;
    Boolean permissionforStorageAccess = true;
    // Engine flag tells us what engine we should use - 0 = OpenCV, 1 = Google
    int engine = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        iv = (ImageView) findViewById(R.id.imageView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu, adding items to the action bar if present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_load_image) {
            // Load image that person can select from Android gallery.
            loadSingleImageFromGallery();
            return true;
        } else if (id == R.id.action_settings) {
            Intent i = new Intent(this, SettingsActivity.class);
            startActivity(i);
            return true;
        } else if (id == R.id.action_load_images_bulk){
            Intent i = new Intent(this, BatchTestActivity.class);
            startActivity(i);
        }

        return super.onOptionsItemSelected(item);
    }

    // This handles the opening of the single image, getting the path of it.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA};
            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();

            // picturePath contains the path of the selected image.

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            String engine = prefs.getString("face_detect_engine", "Google");
            System.out.println(engine);
            // If user has selected the Google engine, we will use it to detect faces.
            if (engine.equals("Google")) {
                bmp = mvdLoadBitmapandDetectFaces(picturePath);
            } else {
                bmp = openCVLoadBitmapandDetectFaces(picturePath);
            }
        }
    }

    void loadSingleImageFromGallery(){
        // If the user is running Android Marshmallow or greater, ask for
        // permission to open file.
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1){
            String[] perms = {"android.permission.WRITE_EXTERNAL_STORAGE"};
            int permsRequestCode = 200;
            requestPermissions(perms, permsRequestCode);
        }
        Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, RESULT_LOAD_IMAGE);
    }

    Bitmap mvdLoadBitmapandDetectFaces(String uriString){
        Uri uri = Uri.fromFile(new File(uriString));
        try {
            Bitmap bmp = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            iv.setImageBitmap(bmp);
            MobileVisionDetector mvd = new MobileVisionDetector();
            mvd.initialise(this);
            System.out.println(mvd.detectFace(bmp));

            iv.setImageBitmap(mvd.getBitmap());
            return bmp;

        } catch (IOException ioe) {
            System.err.println(ioe);
            return null;
        }
    }
    Bitmap openCVLoadBitmapandDetectFaces(String uriString){
        Uri uri = Uri.fromFile(new File(uriString));
        try {
            Bitmap bmp = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            iv.setImageBitmap(bmp);
            OpenCVFaceDetector opd = new OpenCVFaceDetector();
            opd.initialise(this);
            System.out.println(opd.detectFace(bmp));
            iv.setImageBitmap(opd.getBitmap());
            return bmp;

        } catch (IOException ioe) {
            System.err.println(ioe);
            return null;
        }
    }

    private boolean shouldAskPermission() {
        return (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1);
    }

    @Override
    public void onRequestPermissionsResult(int permsRequestCode, String[] permissions, int[] grantResults){

        switch(permsRequestCode){

            case 200:

                permissionforStorageAccess = grantResults[0]== PackageManager.PERMISSION_GRANTED;

                break;

        }

    }
}