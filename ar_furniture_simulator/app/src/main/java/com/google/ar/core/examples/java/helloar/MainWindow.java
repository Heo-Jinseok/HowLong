package com.google.ar.core.examples.java.helloar;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

public class MainWindow extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mainwindow);

        ImageView iv=(ImageView) findViewById(R.id.imageView);
        iv.setImageResource(R.drawable.ic_launcher);

        Button button2 = (Button) findViewById(R.id.appStart);
        button2.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Intent intent123 = new Intent(getApplicationContext(),MainActivity.class);
                startActivity(intent123);
            }
        });
        Button button3 = (Button) findViewById(R.id.Explain);
        button3.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Intent intent12 = new Intent(getApplicationContext(),ExplainActivity.class);
                startActivity(intent12);
            }
        });
    }
}