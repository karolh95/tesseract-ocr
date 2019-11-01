package com.ocr.tesseract4;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.googlecode.leptonica.android.Pix;
import com.googlecode.leptonica.android.Pixa;
import com.googlecode.leptonica.android.ReadFile;
import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.function.Function;

public class Ocr extends AsyncTask<Void, Void, Void> {

    private static final String LANG = "eng";
    private static final String DIRECTORY_NAME = "/Tesseract/";
    private static final String DATA_PATH = Environment.getExternalStorageDirectory().toString() + DIRECTORY_NAME;
    private static final String TESSDATA = "tessdata";

    private static final String TAG = Ocr.class.getSimpleName();

    private WeakReference<MainActivity> activity;
    private Bitmap bitmap;
    private Function<TessBaseAPI, Pixa> action;

    Ocr(MainActivity activity, Bitmap bitmap, Function<TessBaseAPI, Pixa> action) {
        this.activity = new WeakReference<>(activity);
        this.bitmap = bitmap;
        this.action = action;
    }

    @Override
    protected Void doInBackground(Void... voids) {

        long time = System.currentTimeMillis();

//        Prepare Tesseract working directory
        prepareDirectory();
//        Prepare Tesseract data files
        copyTessDataFiles();
//        Prepare Tesseract API
        TessBaseAPI api = new TessBaseAPI();
        api.init(DATA_PATH, LANG);

//        Read and apply bitmap
        Pix image = ReadFile.readBitmap(bitmap);
        api.setImage(image);

//        Recognize text from an image...
        Pixa pixa = null;
        String outText = null;

        if (action != null) {
            pixa = action.apply(api);
            if (pixa == null) {
                outText = api.getHOCRText(0);
            }
        } else {
            outText = api.getUTF8Text();
        }
//        Operation complete!
        api.clear();
        api.end();

        long executionTime = System.currentTimeMillis() - time;
        Pixa arg1 = pixa;
        String arg2 = outText;

        activity.get().runOnUiThread(() -> activity.get().finish(arg1, arg2, executionTime));
        return null;
    }


    /**
     * Prepare directory on external storage
     */
    private void prepareDirectory() {

        File dir = new File(DATA_PATH + TESSDATA);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Log.e(TAG, "ERROR: Creation of directory " + DATA_PATH + TESSDATA + " failed, check does Android Manifest have permission to write to external storage.");
            } else {
                Log.i(TAG, "Created directory " + DATA_PATH + TESSDATA);
            }
        }
    }

    /**
     * Copy tessdata files (located on assets/tessdata) to destination directory
     **/
    private void copyTessDataFiles() {
        try {
            String[] fileList = activity.get().getAssets().list(TESSDATA);

            assert fileList != null;
            for (String fileName : fileList) {

                // open file within the assets folder
                // if it is not already there copy it to the sdcard
                String pathToDataFile = DATA_PATH + TESSDATA + "/" + fileName;
                if (!(new File(pathToDataFile)).exists()) {

                    InputStream in = activity.get().getAssets().open(TESSDATA + "/" + fileName);

                    OutputStream out = new FileOutputStream(pathToDataFile);

                    // Transfer bytes from in to out
                    byte[] buf = new byte[1024];
                    int len;

                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                    in.close();
                    out.close();

                    Log.i(TAG, "Copied " + fileName + " to " + TESSDATA);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Unable to copy files to " + TESSDATA + ": " + e.toString());
        }
    }

}
