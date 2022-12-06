package com.game2048;

import android.app.Activity;
import android.app.Service;
import android.app.UiModeManager;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

public class Setting extends Activity {

    EditText e1, e2, e3, e4, e5, e6, e7, e8, e9, e10;
    SharedPreferences set;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION | WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
            setTheme(((UiModeManager) getSystemService(Service.UI_MODE_SERVICE)).getNightMode() == UiModeManager.MODE_NIGHT_YES?android.R.style.Theme_DeviceDefault:android.R.style.Theme_DeviceDefault_Light);
        setContentView(R.layout.set);
        e1 = findViewById(R.id.e1);
        e2 = findViewById(R.id.e2);
        e3 = findViewById(R.id.e3);
        e4 = findViewById(R.id.e4);
        e5 = findViewById(R.id.e5);
        e6 = findViewById(R.id.e6);
        e7 = findViewById(R.id.e7);
        e8 = findViewById(R.id.e8);
        e9 = findViewById(R.id.e9);
        e10 = findViewById(R.id.e10);
        set = PreferenceManager.getDefaultSharedPreferences(this);
        e1.setText(String.valueOf(set.getInt("x", 4)));
        e2.setText(String.valueOf(set.getInt("y", 4)));
        e3.setText(String.valueOf(set.getInt("time", 1)));
        e4.setText(String.valueOf(set.getInt("start", 2)));
        e5.setText(String.valueOf(set.getInt("poss", 90)));
        e6.setText(String.valueOf(set.getInt("AItime", 200)));
        e7.setText(String.valueOf(set.getInt("max", 10)));
        e8.setText(String.valueOf(set.getInt("smooth", 1)));
        e9.setText(String.valueOf(set.getInt("mono", 13)));
        e10.setText(String.valueOf(set.getInt("empty", 27)));

    }

    public void cancel(View view) {
        finish();
    }

    public void save(View view) {
        for (EditText e : new EditText[]{e1, e2, e3, e4, e5}) {
            if (e.getText().length() < 1) {
                Toast.makeText(this, "请重新输入", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        }
        for (EditText e : new EditText[]{ e7, e8, e9, e10}) {
            if (e.getText().length() < 1) {
                Toast.makeText(this, "请重新输入AI各项权重值", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        }
        if (e6.getText().length() < 3) {
            Toast.makeText(this, "请重新输入AI思考时间", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        set.edit().putInt("x", Integer.parseInt(e1.getText().toString())).putInt("y", Integer.parseInt(e2.getText().toString())).putInt("time", Integer.parseInt(e3.getText().toString())).putInt("start", Integer.parseInt(e4.getText().toString())).putInt("poss", Integer.parseInt(e5.getText().toString())).putInt("AItime", Integer.parseInt(e6.getText().toString())).putInt("max", Integer.parseInt(e7.getText().toString())).putInt("smooth", Integer.parseInt(e8.getText().toString())).putInt("mono", Integer.parseInt(e9.getText().toString())).putInt("empty", Integer.parseInt(e10.getText().toString())).apply();
        Toast.makeText(this, "重启游戏生效", Toast.LENGTH_SHORT).show();
        finish();
    }
}
