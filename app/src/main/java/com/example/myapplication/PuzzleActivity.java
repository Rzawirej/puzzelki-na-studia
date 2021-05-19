package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import static java.lang.Math.abs;

public class PuzzleActivity extends AppCompatActivity {

    private long startTime;
    ArrayList<Puzzle> pieces;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_puzzle);

        final RelativeLayout layout = findViewById(R.id.layout);
        ImageView imageView = findViewById(R.id.imageView);

        Intent intent = getIntent();
        final String assetName = intent.getStringExtra("assetName");
        // run image related code after the view was laid out
        // to have all dimensions calculated
        getSupportActionBar().hide();
        imageView.post(() -> {
            if (assetName != null) {
                setPicFromAsset(assetName, imageView);
            }
            pieces = splitImage();
            TouchListener touchListener = new TouchListener(PuzzleActivity.this);
            Collections.shuffle(pieces);
            for(Puzzle piece : pieces) {
                piece.setOnTouchListener(touchListener);
                layout.addView(piece);
                RelativeLayout.LayoutParams lParams = (RelativeLayout.LayoutParams) piece.getLayoutParams();
                lParams.leftMargin = new Random().nextInt(layout.getWidth() - piece.pieceWidth);
                lParams.topMargin = layout.getHeight() - piece.pieceHeight * (1 + new Random().nextInt(2));;
                piece.setLayoutParams(lParams);
            }
            startTime = System.currentTimeMillis();
        });
    }

    public void checkGameOver() {
        if (isGameOver()) {
            TextView textView = findViewById(R.id.textView);

            long difference = System.currentTimeMillis() - startTime;
            textView.setText("Ukończono w "+difference/1000+" sekund.");
        }
    }

    private boolean isGameOver() {
        for (Puzzle piece : pieces) {
            if (piece.canMove) {
                return false;
            }
        }

        return true;
    }
    private void setPicFromAsset(String assetName, ImageView imageView) {
        // Get the dimensions of the View
        int targetW = imageView.getWidth();
        int targetH = imageView.getHeight();

        AssetManager am = getAssets();
        try {
            InputStream is = am.open("img/" + assetName);
            // Get the dimensions of the bitmap
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, new Rect(-1, -1, -1, -1), bmOptions);
            int photoW = bmOptions.outWidth;
            int photoH = bmOptions.outHeight;

            // Determine how much to scale down the image
            int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

            is.reset();

            // Decode the image file into a Bitmap sized to fill the View
            bmOptions.inJustDecodeBounds = false;
            bmOptions.inSampleSize = scaleFactor;
            bmOptions.inPurgeable = true;

            Bitmap bitmap = BitmapFactory.decodeStream(is, new Rect(-1, -1, -1, -1), bmOptions);
            imageView.setImageBitmap(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private ArrayList<Puzzle> splitImage() {

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;

        //For the number of rows and columns of the grid to be displayed
        int rows =3,cols=4;

        //For height and width of the small image chunks
        int chunkHeight,chunkWidth;

        //To store all the small image chunks in bitmap format in this list
        ArrayList<Puzzle> chunkedImages = new ArrayList<>(rows*cols);
        ImageView imageView = findViewById(R.id.imageView);
        BitmapDrawable drawable = (BitmapDrawable) imageView.getDrawable();
        Bitmap bitmap = drawable.getBitmap();

        float[] f = new float[9];
        imageView.getImageMatrix().getValues(f);

        // Extract the scale values using the constants (if aspect ratio maintained, scaleX == scaleY)
        for(int x = 0; x < 9; x++) {
            System.out.println(f[x]);
        }
        final float scaleX = f[Matrix.MSCALE_X];
        final float scaleY = f[Matrix.MSCALE_Y];

        final int origW = drawable.getIntrinsicWidth();
        final int origH = drawable.getIntrinsicHeight();
        final int actW = Math.round(origW * scaleX);
        final int actH = Math.round(origH * scaleY);

        int imgViewW = imageView.getWidth();
        int imgViewH = imageView.getHeight();
        int top = (int) (imgViewH - actH)/2;
        int left = (int) (imgViewW - actW)/2;

        int scaledBitmapLeft = left;
        int scaledBitmapTop = top;
        int scaledBitmapWidth = actW;
        int scaledBitmapHeight = actH;

        int croppedImageWidth = scaledBitmapWidth - 2 * abs(scaledBitmapLeft);
        int croppedImageHeight = scaledBitmapHeight - 2 * abs(scaledBitmapTop);


        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledBitmapWidth, scaledBitmapHeight, true);
        Bitmap croppedBitmap = Bitmap.createBitmap(scaledBitmap, abs(scaledBitmapLeft), abs(scaledBitmapTop), croppedImageWidth, croppedImageHeight);

        //Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 300, 300, true);
        chunkHeight = croppedBitmap.getHeight() / rows;
        chunkWidth = croppedBitmap.getWidth() / cols;
        System.out.println(chunkHeight);
        //xCoord and yCoord are the pixel positions of the image chunks
        int yCoord = 0;
        for(int x = 0; x < rows; x++) {
            int xCoord = 0;
            for (int y = 0; y < cols; y++) {
                Bitmap pieceBitmap = Bitmap.createBitmap(croppedBitmap, xCoord, yCoord, chunkWidth, chunkHeight);
                Puzzle piece = new Puzzle(getApplicationContext());
                piece.setImageBitmap(pieceBitmap);
                piece.xCoord = xCoord + 48;
                piece.yCoord = yCoord + 48;
                piece.pieceWidth = chunkWidth;
                piece.pieceHeight = chunkHeight;
                chunkedImages.add(piece);
                xCoord += chunkWidth;
            }
            yCoord += chunkHeight;
        }
        return chunkedImages;
    }
}