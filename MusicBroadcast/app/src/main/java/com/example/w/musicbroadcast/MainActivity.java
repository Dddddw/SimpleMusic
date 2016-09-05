package com.example.w.musicbroadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.lang.ref.WeakReference;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{


    //Activity广播接收器监听的行为
    public static final String UPDATE_ACTION = "update_action";
    public static final String CURRENT = "current";
    public static final String STATUS = "status";
    private ImageView mPlay_imageView;
    private ImageView mImageView;
    ImageView prev_imageView;
    ImageView next_imageView;
    private ActivityReceiver mActivityReceiver;
    private static  SeekBar mSeekBar;

    static boolean mStop = true;
    //歌曲名称数组
    static String [] songName = new String[]{"黄昏晓", "不完美女孩", "玛卡瑞拉"};
    //歌曲图片数组
    static int[] songPicture = new int[]{R.drawable.one,R.drawable.two, R.drawable.three};
    private Intent mIntent ;
    private TextView mTextView;

    private MusicService.activity_handler mActivity_handler = new MusicService.activity_handler();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialView();
        //注册广播接收器
        mActivityReceiver = new ActivityReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UPDATE_ACTION);
        registerReceiver(mActivityReceiver, intentFilter);

    }

    /**
     * 初始化控件，并设置监听
     */
    private void initialView(){
        mPlay_imageView = (ImageView) findViewById(R.id.music_play_image);
        prev_imageView = (ImageView) findViewById(R.id.music_previous_image);
        next_imageView = (ImageView) findViewById(R.id.music_next_image);
        mImageView = (ImageView) findViewById(R.id.imageView);
        mTextView = (TextView) findViewById(R.id.music_song_name_text);
        mSeekBar = (SeekBar) findViewById(R.id.music_seek_bar);
        mSeekBar.setMax(999);

        mPlay_imageView.setOnClickListener(this);
        prev_imageView.setOnClickListener(this);
        next_imageView.setOnClickListener(this);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                seekBar.setProgress(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            //滑块完成使的行为
            public void onStopTrackingTouch(SeekBar seekBar) {

                Message message = mActivity_handler.obtainMessage();
                message.what = 0x124;
                message.arg1 = seekBar.getProgress();
                mActivity_handler.sendMessage(message);
            }
        });
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()){
            case R.id.music_play_image:
                if (mStop){
                    mIntent = new Intent(this, MusicService.class);
                    startService(mIntent);
                    mStop = false;
                }
                pushAction(1);
                break;
            case R.id.music_previous_image:
                pushAction(2);
                break;
            case R.id.music_next_image:
                pushAction(3);
                break;
        }
    }

    public class  ActivityReceiver extends BroadcastReceiver{

        //接收从Service传来的广播
        @Override
        public void onReceive(Context context, Intent intent) {
            String update_action = intent.getAction();
            if (UPDATE_ACTION.contains(update_action)){
                int current = intent.getIntExtra(CURRENT, -1);
                int status = intent.getIntExtra(STATUS, -1);
                if (current >= 0){
                    mTextView.setText(songName[current]);
                    mImageView.setImageResource(songPicture[current]);
                }
                switch (status){
                    case 0x11:
                        mPlay_imageView.setImageResource(R.drawable.play);
                        break;
                    case 0x12:
                        mPlay_imageView.setImageResource(R.drawable.pause);
                }
            }

        }
    }

    //给MusicService发广播
    private void pushAction(int action){
        Intent intent = new Intent(MusicService.CTRL_ACTION);
        intent.putExtra(MusicService.USER_ACTION_KEY, action);
        sendBroadcast(intent);
    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mActivityReceiver);
        stopService(new Intent(this, MusicService.class));
        System.out.println("我是activity的destroy");
    }

    /**
     * 重写返回键，使之变成home键的功能
     */
    @Override
    public void onBackPressed() {

        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addCategory(Intent.CATEGORY_HOME);
        startActivity(intent);
        System.out.println("我是返回键");

    }

    public static class service_Handler extends Handler{

        public final WeakReference<MainActivity> mMainActivityWeakReference;

        public service_Handler(MainActivity activity) {

            mMainActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        //随歌曲的播放改变SeekBar的progress
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 0x123){
                int Max = mSeekBar.getMax();
                int a  = msg.arg1 * Max / msg.arg2;
                mSeekBar.setProgress(a);

            }
        }
    }
}
