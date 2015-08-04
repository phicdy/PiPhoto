package com.phicdy.piphoto;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.widget.ImageView;

import com.phicdy.piphoto.util.FileUtil;


public class PhotoActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);

        Intent intent = getIntent();
        String photoPath = intent.getStringExtra(FileUtil.EXTRA_PHOTO);
        ImageView imageView = (ImageView)findViewById(R.id.image);
        imageView.setImageBitmap(BitmapFactory.decodeFile(photoPath));
    }

}
