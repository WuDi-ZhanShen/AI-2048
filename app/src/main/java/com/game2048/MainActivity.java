package com.game2048;

import android.animation.Animator;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Rect;
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

public class MainActivity extends Activity {


    MainView view;
    private boolean isAnimPlayed = false, shouldLoad;
    public static final String WIDTH = "width";
    public static final String HEIGHT = "height";
    public static final String SCORE = "score";
    public static final String HIGH_SCORE = "high score temp";
    public static final String UNDO_SCORE = "undo score";
    public static final String GAME_STATE = "game state";
    public static final String UNDO_GAME_STATE = "undo game state";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        boolean isNight = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_YES) == Configuration.UI_MODE_NIGHT_YES;
        if (!isNight) {
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
        view = new MainView(this, isNight);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION | WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
            setTheme(isNight ? android.R.style.Theme_DeviceDefault : android.R.style.Theme_DeviceDefault_Light);
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
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        view.hasSaveState = settings.getBoolean("save_state", false);
        shouldLoad = !settings.getBoolean("changeScale", false);
        settings.edit().putBoolean("changeScale", false).apply();
        if (savedInstanceState != null && savedInstanceState.getBoolean("hasState")) {
            load();
        }
        setContentView(view);

        // 创建水波动画
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            getWindow().getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(() -> {
                if (!isAnimPlayed) {
                    isAnimPlayed = true;
                    Rect rect = getIntent().getSourceBounds();
                    float x, y;
                    if (null != rect) {
                        x = (rect.right + rect.left) >> 1;
                        y = (rect.bottom + rect.top) >> 1;
                    } else {
                        DisplayMetrics metrics = new DisplayMetrics();
                        getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
                        x = metrics.widthPixels;
                        y = metrics.heightPixels;
                    }

                    // 创建水波背景视图
                    // 获取点击位置相对于屏幕的坐标
                    int[] location = new int[2];
                    view.getLocationOnScreen(location);
                    int screenX = (int) (x - location[0]);
                    int screenY = (int) (y - location[1]);

                    // 计算动画半径
                    int screenWidth = getResources().getDisplayMetrics().widthPixels;
                    int screenHeight = getResources().getDisplayMetrics().heightPixels;
                    double maxRadius = Math.sqrt(Math.pow(screenWidth, 2) + Math.pow(screenHeight, 2));
                    int finalRadius = (int) Math.max(maxRadius - x, Math.max(maxRadius - y, Math.max(x, y)));

                    // 创建水波动画
                    Animator animator = ViewAnimationUtils.createCircularReveal(view, screenX, screenY, 0, finalRadius);
                    animator.setDuration(500);
                    animator.setInterpolator(new AccelerateInterpolator()); // 设置插值器
                    animator.start();

                }

            });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            // Do nothing
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN || keyCode == KeyEvent.KEYCODE_S) {
            view.game.move(2);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_W) {
            view.game.move(0);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_A) {
            view.game.move(3);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_D) {
            view.game.move(1);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean("hasState", true);
        save();
    }

    @Override
    protected void onPause() {
        super.onPause();
        save();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        save();
    }

    private void save() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = settings.edit();
        Tile[][] field = view.game.grid.field;
        editor.putInt(WIDTH, field.length);
        editor.putInt(HEIGHT, field.length);
        for (int xx = 0; xx < field.length; xx++) {
            for (int yy = 0; yy < field[0].length; yy++) {
                if (field[xx][yy] != null) {
                    editor.putInt(xx + " " + yy, field[xx][yy].getValue());
                } else {
                    editor.putInt(xx + " " + yy, 0);
                }

            }
        }
        editor.putLong(SCORE, view.game.score);
        editor.putLong(HIGH_SCORE, view.game.highScore);
        editor.putLong(UNDO_SCORE, view.game.score);
        editor.putInt(GAME_STATE, view.game.gameState);
        editor.putInt(UNDO_GAME_STATE, view.game.lastGameState);
        editor.apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        // Stopping all animations
        view.game.aGrid.cancelAnimations();
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        if (shouldLoad) {
            for (int xx = 0; xx < view.game.grid.field.length; xx++) {
                for (int yy = 0; yy < view.game.grid.field[0].length; yy++) {
                    int value = settings.getInt(xx + " " + yy, -1);
                    if (value > 0) {
                        view.game.grid.field[xx][yy] = new Tile(xx, yy, value);
                    } else if (value == 0) {
                        view.game.grid.field[xx][yy] = null;
                    }
                }
            }
            view.game.score = settings.getLong(SCORE, view.game.score);
            view.game.lastScore.add(settings.getLong(UNDO_SCORE, 0));
            view.game.lastGameState = settings.getInt(UNDO_GAME_STATE, view.game.lastGameState);
            view.game.gameState = settings.getInt(GAME_STATE, view.game.gameState);
        }
        view.game.highScore = settings.getLong(HIGH_SCORE, view.game.highScore);

    }

    @Override
    public void onBackPressed() {
        animateFinish();
    }

    public void animateFinish() {
        view.game.isAIRunning = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
            Rect rect = getIntent().getSourceBounds();
            int x, y;
            if (null != rect) {
                x = (rect.right + rect.left) >> 1;
                y = (rect.bottom + rect.top) >> 1;
            } else {
                DisplayMetrics metrics1 = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getRealMetrics(metrics1);
                x = metrics1.widthPixels;
                y = metrics1.heightPixels;
            }
            // 计算动画半径
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            int screenHeight = getResources().getDisplayMetrics().heightPixels;
            double maxRadius = Math.sqrt(Math.pow(screenWidth, 2) + Math.pow(screenHeight, 2));
            int finalRadius = (int) Math.max(maxRadius - x, Math.max(maxRadius - y, Math.max(x, y)));

            // 创建水波动画
            Animator animator = ViewAnimationUtils.createCircularReveal(view, x, y, finalRadius, 0);
            animator.setDuration(500);
            animator.setInterpolator(new DecelerateInterpolator()); // 设置插值器
            animator.start();
            animator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animator) {

                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    view.setVisibility(View.GONE);
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
}
