package com.ocr.tesseract4;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.googlecode.leptonica.android.Pixa;
import com.googlecode.tesseract.android.TessBaseAPI;
import com.ocr.tesseract4.tools.PermissionTool;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class MainActivity extends AppCompatActivity {

    private static final String[] PERMISSIONS = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private static final String CLOCK = "Execution time: ";
    private static final String DATE_FORMAT = "mm:ss.SSS";

    private static final String MODE_CHAR = "getConnectedComponents()";
    private static final String MODE_WORD = "getWords()";
    private static final String MODE_LINE = "getTextLines()";
    private static final String MODE_REGS = "getRegions()";
    private static final String MODE_UTF8 = "getUTF8()";
    private static final String MODE_HOCR = "getHOCR()";


    private int MIN_IMAGE_INDEX;
    private int MAX_IMAGE_INDEX;

    private PermissionTool tool;
    private Function<TessBaseAPI, Pixa> action;
    private BitmapDrawable[] bitmaps;
    private int bitmapIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tool = new PermissionTool();

        MIN_IMAGE_INDEX = 0;
        MAX_IMAGE_INDEX = 3;

        bitmaps = new BitmapDrawable[4];
        bitmaps[0] = (BitmapDrawable) getResources().getDrawable(R.drawable.test_01, getTheme());
        bitmaps[1] = (BitmapDrawable) getResources().getDrawable(R.drawable.test_02, getTheme());
        bitmaps[2] = (BitmapDrawable) getResources().getDrawable(R.drawable.test_03, getTheme());
        bitmaps[3] = (BitmapDrawable) getResources().getDrawable(R.drawable.test_04, getTheme());

        bitmapIndex = 1;

        updateImageView();

        TextView mode = findViewById(R.id.mode);
        mode.append(MODE_UTF8);

        TextView textView = findViewById(R.id.textView);
        textView.setMovementMethod(new ScrollingMovementMethod());

    }

    public void previousImage(View view) {
        bitmapIndex--;
        if (bitmapIndex < MIN_IMAGE_INDEX)
            bitmapIndex = MAX_IMAGE_INDEX;
        updateImageView();
    }

    public void nextImage(View view) {
        bitmapIndex++;
        if (bitmapIndex > MAX_IMAGE_INDEX)
            bitmapIndex = MIN_IMAGE_INDEX;
        updateImageView();
    }

    private void updateImageView() {
        ImageView imageView = findViewById(R.id.imageView);
        imageView.setImageBitmap(bitmaps[bitmapIndex].getBitmap());
    }

    private void updateImageView(Bitmap bitmap) {
        ImageView imageView = findViewById(R.id.imageView);
        imageView.setImageBitmap(bitmap);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        String mode = null;
        switch (menuItem.getItemId()) {
            case R.id.characters:
                action = TessBaseAPI::getConnectedComponents;
                mode = MODE_CHAR;
                break;
            case R.id.words:
                action = TessBaseAPI::getWords;
                mode = MODE_WORD;
                break;
            case R.id.text_lines:
                action = TessBaseAPI::getTextlines;
                mode = MODE_LINE;
                break;
            case R.id.regions:
                action = TessBaseAPI::getRegions;
                mode = MODE_REGS;
                break;
            case R.id.utf8:
                action = null;
                mode = MODE_UTF8;
                break;
            case R.id.hocr:
                action = (tessBaseAPI) -> null;
                mode = MODE_HOCR;
                break;
        }
        TextView textView = findViewById(R.id.mode);
        textView.setText(R.string.mode);
        textView.append(mode);
        return false;
    }

    public void start(View view) {

        if (tool.isPermissionsGranted(this, PERMISSIONS)) {

            view.setEnabled(false);
            findViewById(R.id.progressBar).setVisibility(View.VISIBLE);

            Ocr ocr = new Ocr(this, bitmaps[bitmapIndex].getBitmap(), action);
            ocr.execute();

        } else {
            tool.requestPermissions(this, PERMISSIONS);
        }
    }

    public void finish(Pixa pixa, String string, long time) {
        findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
        findViewById(R.id.actionButton).setEnabled(true);

        Date date = new Date(time);
        DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.US);

        TextView textView = findViewById(R.id.textView);
        textView.setText((CLOCK + dateFormat.format(date)));

        if (pixa != null) {
            draw(pixa);
        } else {
            textView.append("\n" + string);
        }
    }

    private void draw(Pixa pixa) {
        // prepare bitmap
        Bitmap bitmap = bitmaps[bitmapIndex].getBitmap().copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(bitmap);

        Paint color1 = new Paint();
        color1.setARGB(63, 0, 0, 255);

        Paint color2 = new Paint();
        color2.setARGB(63, 0, 255, 0);

        Paint color3 = new Paint();
        color3.setARGB(63, 255, 0, 0);

        Paint[] color = {color1, color2, color3};


        AtomicInteger i = new AtomicInteger();
        pixa.getBoxRects().forEach((rect) -> canvas.drawRect(rect, color[i.getAndIncrement() % 3]));

        updateImageView(bitmap);

        pixa.recycle();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, int[] grantResults) {

        boolean grantedAllPermissions = true;
        for (int grantResult : grantResults) {
            if (grantResult != PackageManager.PERMISSION_GRANTED) {
                grantedAllPermissions = false;
            }
        }

        if (grantResults.length != permissions.length || (!grantedAllPermissions)) {
            tool.onPermissionDenied();
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


}