package com.radioyps.gcm_test;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

/**
 * Created by developer on 12/10/16.
 */


    public class QrActivity extends AppCompatActivity {

        private ImageView imageView;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_qr);
            imageView = (ImageView) this.findViewById(R.id.imageView);
            Bitmap bitmap = getIntent().getParcelableExtra("pic");
            imageView.setImageBitmap(bitmap);
        }
    }

