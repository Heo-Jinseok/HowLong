package com.google.ar.core.examples.java.helloar;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;


public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);




        //개발자 정보 버튼 클릭시 액티비티 전환
        Button button = (Button) findViewById(R.id.wdh_who);
        button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(),OpenGLES20Activity.class);
                EditText editPhone1 = (EditText) findViewById(R.id.ww);
                EditText editPhone2 = (EditText) findViewById(R.id.dd);
                EditText editPhone3 = (EditText) findViewById(R.id.hh);
                String width = editPhone1.getText().toString();
                String depth = editPhone2.getText().toString();
                String height = editPhone3.getText().toString();
                Bundle bd = new Bundle();
                bd.putString("width",width);
                bd.putString("depth",depth);
                bd.putString("height",height);
                intent.putExtras(bd);
                startActivity(intent);
            }
        });
        Button button1 = (Button) findViewById(R.id.argogo);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent1 = new Intent(getApplicationContext(), HelloArActivity.class);
                EditText editPhone1 = (EditText) findViewById(R.id.ww);
                EditText editPhone2 = (EditText) findViewById(R.id.dd);
                EditText editPhone3 = (EditText) findViewById(R.id.hh);
                String width = editPhone1.getText().toString();
                String depth = editPhone2.getText().toString();
                String height = editPhone3.getText().toString();
                Bundle bd1 = new Bundle();
                bd1.putString("width",width);
                bd1.putString("depth",depth);
                bd1.putString("height",height);
                intent1.putExtras(bd1);
                startActivity(intent1);
            }
        });


    }
}