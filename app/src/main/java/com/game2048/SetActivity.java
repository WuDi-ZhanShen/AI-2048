package com.game2048;

import android.animation.Animator;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Toast;

public class SetActivity extends Activity {
    private boolean isAnimPlayed = false;
    private ScrollView scrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION | WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        boolean isNight = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_YES) == Configuration.UI_MODE_NIGHT_YES;
        if (!isNight) {
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.setDecorFitsSystemWindows(false);
                WindowInsetsController insetsController = window.getInsetsController();
                if (insetsController != null) {
                    insetsController.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.displayCutout());
                    insetsController.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
            setTheme(isNight ? android.R.style.Theme_DeviceDefault : android.R.style.Theme_DeviceDefault_Light);

        setContentView(R.layout.set);
        scrollView = findViewById(R.id.scrollView);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(() -> {
                if (!isAnimPlayed) {
                    isAnimPlayed = true;
                    int x, y;
                    DisplayMetrics metrics = new DisplayMetrics();
                    getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
                    x = getIntent().getIntExtra("startX", metrics.widthPixels >> 1);
                    y = getIntent().getIntExtra("startY", metrics.heightPixels >> 1);


                    scrollView.setBackgroundColor(getResources().getColor(R.color.background));
                    // 计算动画半径
                    int screenWidth = getResources().getDisplayMetrics().widthPixels;
                    int screenHeight = getResources().getDisplayMetrics().heightPixels;
                    double maxRadius = Math.sqrt(Math.pow(screenWidth, 2) + Math.pow(screenHeight, 2));
                    int finalRadius = (int) Math.max(maxRadius - x, Math.max(maxRadius - y, Math.max(x, y)));

                    // 创建水波动画
                    Animator animator = ViewAnimationUtils.createCircularReveal(scrollView, x, y, 0, finalRadius);
                    animator.setDuration(500);
                    animator.setInterpolator(new AccelerateInterpolator()); // 设置插值器
                    animator.start();
                }

            });
        }
        EditText e1, e2, e3, e4, e5, e6, e7, e8, e9, e10, e11;
        CheckBox c1;
        SeekBar s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11;

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
        e11 = findViewById(R.id.e11);
        c1 = findViewById(R.id.c1);
        s1 = findViewById(R.id.s1);
        s2 = findViewById(R.id.s2);
        s3 = findViewById(R.id.s3);
        s4 = findViewById(R.id.s4);
        s5 = findViewById(R.id.s5);
        s6 = findViewById(R.id.s6);
        s7 = findViewById(R.id.s7);
        s8 = findViewById(R.id.s8);
        s9 = findViewById(R.id.s9);
        s10 = findViewById(R.id.s10);
        s11 = findViewById(R.id.s11);
        EditText[] editTexts = {e1, e2, e3, e4, e5, e6, e7, e8, e9, e10, e11};
        SeekBar[] seekBars = {s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11};
        int[] defaultValues = {4, 4, 1, 2, 90, 100, 10, 1, 40, 27, 5};
        int[] minValues = {3, 3, 1, 1, 0, 50, 0, 0, 0, 0, 0};
        String[] preferencesKeys = {"x", "y", "time", "start", "poss", "AItime", "max", "smooth", "mono", "empty", "maxDepth"};

        SharedPreferences set = PreferenceManager.getDefaultSharedPreferences(this);

        for (int i = 0; i < editTexts.length; i++) {
            EditText editText = editTexts[i];
            SeekBar seekBar = seekBars[i];
            int defaultValue = defaultValues[i];
            int minValue = minValues[i];
            String preferencesKey = preferencesKeys[i];

            editText.setText(String.valueOf(set.getInt(preferencesKey, defaultValue)));
            seekBar.setProgress(set.getInt(preferencesKey, defaultValue));

            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (progress < minValue) {
                        seekBar.setProgress(minValue);
                        return;
                    }
                    editText.setText(String.valueOf(progress));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });

            editText.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) {
                    String text = editText.getText().toString();
                    int value = Integer.parseInt(text.isEmpty() ? "0" : text);
                    seekBar.setProgress(value);
                }
            });
            editText.setOnKeyListener((view, i1, keyEvent) -> {
                if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER && keyEvent.getAction() == KeyEvent.ACTION_DOWN && editText.getText().length() > 0) {

                    String text = editText.getText().toString();
                    int value = Integer.parseInt(text.isEmpty() ? "0" : text);
                    seekBar.setProgress(value);

                }
                return false;
            });
        }

        c1.setChecked(set.getBoolean("considerFour", true));
        final Button save = findViewById(R.id.save);
        save.setOnClickListener(view -> {
            for (EditText e : new EditText[]{e1, e2, e3, e4, e5}) {
                if (e.getText().length() == 0) {
                    Toast.makeText(SetActivity.this, R.string.re_input, Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            for (EditText e : new EditText[]{e7, e8, e9, e10}) {
                if (e.getText().length() == 0) {
                    Toast.makeText(SetActivity.this, R.string.re_input_ai_weight, Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            if (Integer.parseInt(e1.getText().toString()) != set.getInt("x", 4) || Integer.parseInt(e2.getText().toString()) != set.getInt("y", 4)) {
                set.edit().putBoolean("changeScale", true).apply();
            }
            set.edit().putInt("x", Integer.parseInt(e1.getText().toString()))
                    .putInt("y", Integer.parseInt(e2.getText().toString()))
                    .putInt("time", Integer.parseInt(e3.getText().toString()))
                    .putInt("start", Integer.parseInt(e4.getText().toString()))
                    .putInt("poss", Integer.parseInt(e5.getText().toString()))
                    .putInt("AItime", Integer.parseInt(e6.getText().toString()))
                    .putInt("max", Integer.parseInt(e7.getText().toString()))
                    .putInt("smooth", Integer.parseInt(e8.getText().toString()))
                    .putInt("mono", Integer.parseInt(e9.getText().toString()))
                    .putInt("empty", Integer.parseInt(e10.getText().toString()))
                    .putInt("maxDepth", Integer.parseInt(e11.getText().toString()))
                    .putBoolean("considerFour", c1.isChecked())
                    .apply();
            Toast.makeText(SetActivity.this, R.string.setting_change_effect, Toast.LENGTH_SHORT).show();
            animateFinish();
        });


    }

    public void animateFinish() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int x, y;
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
            x = getIntent().getIntExtra("startX", metrics.widthPixels >> 1);
            y = getIntent().getIntExtra("startY", metrics.heightPixels >> 1);

            // 计算动画半径
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            int screenHeight = getResources().getDisplayMetrics().heightPixels;
            double maxRadius = Math.sqrt(Math.pow(screenWidth, 2) + Math.pow(screenHeight, 2));
            int finalRadius = (int) Math.max(maxRadius - x, Math.max(maxRadius - y, Math.max(x, y)));

            // 创建水波动画
            Animator animator = ViewAnimationUtils.createCircularReveal(scrollView, x, y, finalRadius, 0);
            animator.setDuration(500);
            animator.setInterpolator(new DecelerateInterpolator()); // 设置插值器
            animator.start();
            animator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animator) {

                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    scrollView.setVisibility(View.GONE);
                    finish();
                }

                @Override
                public void onAnimationCancel(Animator animator) {

                }

                @Override
                public void onAnimationRepeat(Animator animator) {

                }
            });
        } else {
            finish();
        }
    }

    public void cancel(View view) {
        animateFinish();
    }

    @Override
    public void onBackPressed() {
        animateFinish();
    }
}
