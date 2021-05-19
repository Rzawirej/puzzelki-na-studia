package com.example.myapplication;

import android.content.Context;

import androidx.appcompat.widget.AppCompatImageView;

public class Puzzle extends AppCompatImageView {
    public int xCoord;
    public int yCoord;
    public int pieceWidth;
    public int pieceHeight;
    public boolean canMove = true;

    public Puzzle(Context context) {
        super(context);
    }
}
