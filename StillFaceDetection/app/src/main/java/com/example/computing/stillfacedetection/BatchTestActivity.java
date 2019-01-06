package com.example.computing.stillfacedetection;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import net.bgreco.DirectoryPicker;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;

public class BatchTestActivity extends AppCompatActivity {
    private String imageDirectoryPath;
    private TextView imageDirectoryView;
    private String outputDirectoryPath;
    private TextView outputDirectoryView;
    private ProgressBar progressBar;
    MobileVisionDetector mvd;
    OpenCVFaceDetector opd;
    private final int PICK_IMAGE_DIRECTORY = 2;
    private final int PICK_OUTPUT_DIRECTORY = 3;
    public static SynchronousQueue<processImageCall> processImageCalls;
    ExecutorService es;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_batch_test);
        imageDirectoryView = (TextView) findViewById(R.id.imageDirectory);
        outputDirectoryView = (TextView) findViewById(R.id.outputDirectory);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        es = Executors.newSingleThreadExecutor();
    }


    protected void browseImageDirectory(View v){
        Intent intent = new Intent(this, DirectoryPicker.class);
        intent.putExtra(DirectoryPicker.START_DIR, Environment.getRootDirectory());
        startActivityForResult(intent, PICK_IMAGE_DIRECTORY);
    }
    protected void browseOutputDirectory(View v){
        Intent intent = new Intent(this, DirectoryPicker.class);
        intent.putExtra(DirectoryPicker.START_DIR, Environment.getRootDirectory());
        startActivityForResult(intent, PICK_OUTPUT_DIRECTORY);
    }

    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        System.out.println("Directory picked");
        if (requestCode == PICK_IMAGE_DIRECTORY && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            imageDirectoryPath = (String) extras.get(DirectoryPicker.CHOSEN_DIRECTORY);
            imageDirectoryView.setText(imageDirectoryPath);
        } else if (requestCode == PICK_OUTPUT_DIRECTORY && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            outputDirectoryPath = (String) extras.get(DirectoryPicker.CHOSEN_DIRECTORY);
            outputDirectoryView.setText(outputDirectoryPath);
        }
    }

    void startBatchProcess(View v) throws InterruptedException, ExecutionException, IOException {
        // We will first ask for permission (API 23+)
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1){
            String[] perms = {"android.permission.WRITE_EXTERNAL_STORAGE"};
            int permsRequestCode = 200;
            requestPermissions(perms, permsRequestCode);
        }

        if ( PreferenceManager.getDefaultSharedPreferences(this)
                .getString("face_detect_engine", "Google")
                .equals("Google")) {
            mvd = new MobileVisionDetector();
            mvd.initialise(this);
        } else {
            opd = new OpenCVFaceDetector();
            opd.initialise(this);
        }
        // We will also define the queue to pass data around, visible by all
        // threads.
         processImageCalls = new SynchronousQueue<>();

        // We will first validate both of the file paths chosen, checking if
        // they are not empty.
        if (imageDirectoryPath == null || outputDirectoryPath == null){
            // Display a dialog box, saying that file paths invalid.
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.invalid_path)
                    .setTitle(R.string.invalid_path_title);
            AlertDialog dialog = builder.create();
            dialog.show();
        }

        File[] filesInImageDirectory = new File(imageDirectoryPath)
                .listFiles();

        int count = 1;
        progressBar.setMax(filesInImageDirectory.length);

        // We create a log file, which we will append the results to.
        File outLog = new File(outputDirectoryPath + "/log.txt");
        outLog.createNewFile();
        FileWriter outLogWriter = new FileWriter(outLog);

        for (File f : filesInImageDirectory){
            Future result;
            if ( PreferenceManager.getDefaultSharedPreferences(this)
                    .getString("face_detect_engine", "Google")
                    .equals("Google")) {
                result = es.submit(new MobileVisionFaceDetectCallable(f.getAbsolutePath(), this, mvd));
            } else {
                result = es.submit(new OpenCVFaceDetectCallable(f.getAbsolutePath(), this, opd));
            }
            String res = (String) result.get();
            System.out.println(res);
            outLogWriter.append(res + "\n");
            count++;
            progressBar.setProgress(count);

        }
        outLogWriter.close();

    }
}
