package com.example.project123;


import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;



public class MainActivity extends AppCompatActivity {

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



    }
}