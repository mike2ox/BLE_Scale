package com.example.moonh.inbody_scale;


import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

public class MainActivity extends Activity {

    private Button button1;
    private EditText edit2;
    private EditText edit3;
    private RadioButton radio1;
    private RadioButton radio2;
    public static String radio = "";
    public static String height = "";
    public static String age = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        edit2 = (EditText) findViewById(R.id.edit2);
        edit3 = (EditText) findViewById(R.id.edit3);
        radio1 = (RadioButton) findViewById(R.id.menbutton);
        radio2 = (RadioButton) findViewById(R.id.womenbutton);

        radio1.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                radio = "남자";
                Toast.makeText(getApplicationContext(), radio + "선택", Toast.LENGTH_SHORT).show();
            }
        });

        radio2.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                radio = "여자";
                Toast.makeText(getApplicationContext(), radio + "선택", Toast.LENGTH_SHORT).show();
            }
        });


        button1 = (Button) findViewById(R.id.button1);
        button1.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub

                if(radio.equals("")) {
                    Toast.makeText(getApplicationContext(), "성별을 선택해 주세요", Toast.LENGTH_SHORT).show();
                } else if(edit2.getText().toString().length() == 0) {
                    Toast.makeText(getApplicationContext(), "키를 입력해주세요", Toast.LENGTH_SHORT).show();
                } else if(Integer.parseInt(edit2.getText().toString()) > 400) {
                    Toast.makeText(getApplicationContext(), "키를 올바르게 입력해 주세요", Toast.LENGTH_SHORT).show();
                } else if(edit3.getText().toString().length() == 0) {
                    Toast.makeText(getApplicationContext(), "나이를 입력해주세요", Toast.LENGTH_SHORT).show();
                } else if(Integer.parseInt(edit3.getText().toString()) > 150) {
                    Toast.makeText(getApplicationContext(), "나이를 올바르게 입력해 주세요", Toast.LENGTH_SHORT).show();
                } else  {
                    height = edit2.getText().toString();
                    age = edit3.getText().toString();
                    Intent i = new Intent(getApplicationContext(), DeviceScanActivity.class);
                    startActivity(i);
                    finish();
                }

            }
        });
    }

}
