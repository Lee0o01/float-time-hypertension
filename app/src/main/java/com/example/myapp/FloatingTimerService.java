package com.example.myapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.app.NotificationCompat;

public class FloatingTimerService extends Service {
    private static final String CHANNEL_ID = "FloatingTimerChannel";
    private static final int NOTIFICATION_ID = 1;

    private WindowManager windowManager;
    private View floatingView;
    private TextView timerDisplay;
    private ImageButton startPauseButton;
    private ImageButton resetButton;
    private ImageButton switchModeButton;
    private LinearLayout controlsContainer;
    private boolean isExpanded = true;

    private boolean isCountUp = true; // true为正计时，false为倒计时
    private boolean isRunning = false;
    private long elapsedTime = 0;
    private long countDownTime = 5 * 60 * 1000; // 默认5分钟倒计时
    private Handler handler;
    private Runnable timerRunnable;
    private CountDownTimer countDownTimer;

    private float touchStartX;
    private float touchStartY;
    private int initialX;
    private int initialY;
    private WindowManager.LayoutParams params;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());
        
        handler = new Handler(Looper.getMainLooper());
        initializeTimerRunnable();
        initializeViews();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Floating Timer",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("计时器运行中")
                .setContentText("点击管理悬浮计时器")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentIntent(pendingIntent)
                .build();
    }

    private void initializeViews() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        floatingView = LayoutInflater.from(this).inflate(R.layout.overlay_floating, null);

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 100;

        timerDisplay = floatingView.findViewById(R.id.timerDisplay);
        startPauseButton = floatingView.findViewById(R.id.startPauseButton);
        resetButton = floatingView.findViewById(R.id.resetButton);
        switchModeButton = floatingView.findViewById(R.id.switchModeButton);
        controlsContainer = floatingView.findViewById(R.id.controlsContainer);
        ImageButton collapseButton = floatingView.findViewById(R.id.collapseButton);

        setupClickListeners();
        setupTouchListener();
        windowManager.addView(floatingView, params);
    }

    private void setupClickListeners() {
        startPauseButton.setOnClickListener(v -> {
            if (isRunning) {
                pauseTimer();
            } else {
                startTimer();
            }
        });

        resetButton.setOnClickListener(v -> resetTimer());

        switchModeButton.setOnClickListener(v -> toggleTimerMode());

        floatingView.findViewById(R.id.collapseButton).setOnClickListener(v -> {
            isExpanded = !isExpanded;
            controlsContainer.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        });
    }

    private void setupTouchListener() {
        floatingView.setOnTouchListener((view, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touchStartX = event.getRawX();
                    touchStartY = event.getRawY();
                    initialX = params.x;
                    initialY = params.y;
                    return true;

                case MotionEvent.ACTION_MOVE:
                    float moveX = event.getRawX() - touchStartX;
                    float moveY = event.getRawY() - touchStartY;
                    params.x = initialX + (int) moveX;
                    params.y = initialY + (int) moveY;
                    windowManager.updateViewLayout(floatingView, params);
                    return true;
            }
            return false;
        });
    }

    private void initializeTimerRunnable() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    elapsedTime += 1000;
                    updateTimerDisplay(elapsedTime);
                    handler.postDelayed(this, 1000);
                }
            }
        };
    }

    private void startTimer() {
        isRunning = true;
        startPauseButton.setImageResource(android.R.drawable.ic_media_pause);
        if (isCountUp) {
            handler.post(timerRunnable);
        } else {
            startCountDownTimer();
        }
    }

    private void pauseTimer() {
        isRunning = false;
        startPauseButton.setImageResource(android.R.drawable.ic_media_play);
        if (isCountUp) {
            handler.removeCallbacks(timerRunnable);
        } else if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    private void resetTimer() {
        pauseTimer();
        if (isCountUp) {
            elapsedTime = 0;
        }
        updateTimerDisplay(isCountUp ? elapsedTime : countDownTime);
    }

    private void toggleTimerMode() {
        pauseTimer();
        isCountUp = !isCountUp;
        resetTimer();
    }

    private void startCountDownTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new CountDownTimer(countDownTime, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                updateTimerDisplay(millisUntilFinished);
            }

            @Override
            public void onFinish() {
                isRunning = false;
                updateTimerDisplay(0);
                startPauseButton.setImageResource(android.R.drawable.ic_media_play);
                showTimerFinishedNotification();
            }
        }.start();
    }

    private void updateTimerDisplay(long timeInMillis) {
        int seconds = (int) (timeInMillis / 1000) % 60;
        int minutes = (int) (timeInMillis / 1000) / 60;
        String timeString = String.format("%02d:%02d", minutes, seconds);
        timerDisplay.setText(timeString);
    }

    private void showTimerFinishedNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("计时结束")
                .setContentText("倒计时已完成")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.notify(2, builder.build());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null && windowManager != null) {
            windowManager.removeView(floatingView);
        }
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        handler.removeCallbacks(timerRunnable);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}