package com.example.w.musicbroadcast;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    public static final int UPDATE_WHAT = 0x60;

    private ImageView mPlay_imageView;
    private ImageView mImageView;
    ImageView prev_imageView;
    ImageView next_imageView;
    private static  SeekBar mSeekBar;
    static boolean mStop = true;
    //歌曲名称数组
//    static String [] songName = new String[]{"黄昏晓", "不完美女孩", "玛卡瑞拉"};
    //歌曲图片数组
//    static int[] songPicture = new int[]{R.drawable.one,R.drawable.two, R.drawable.three};
    private TextView mTextView;
    boolean mBound;

    Messenger mMessenger = new Messenger(new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case UPDATE_WHAT:
                    int current = msg.arg1;
                    int status = msg.arg2;
                    if (current >= 0){
                        //mTextView.setText(songName[current]);
                        //mImageView.setImageResource(songPicture[current]);
                    }
                    switch (status){
                        case 0x11:
                            mPlay_imageView.setImageResource(R.drawable.play);
                            break;
                        case 0x12:
                            mPlay_imageView.setImageResource(R.drawable.pause);
                    }
                    break;
                case 0x123:
                    int Max = mSeekBar.getMax();
                    int a  = msg.arg1 * Max / msg.arg2;
                    mSeekBar.setProgress(a);
                    break;
            }

        }
    });
    Messenger mServiceMessenger;
    ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mServiceMessenger = new Messenger(service);
            mBound = true;
            Message message = Message.obtain(null, MusicService.MSG_REGISTER_CLIENT);
            message.replyTo = mMessenger;
            try {
                mServiceMessenger.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

        }
        @Override
        public void onServiceDisconnected(ComponentName name) {

            mServiceMessenger = null;
            mBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialView();
        bindService(new Intent(MainActivity.this, MusicService.class), mServiceConnection, Context.BIND_AUTO_CREATE);

    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBound){
            Message message = Message.obtain(null,MusicService.MSG_UNREGISTER_CLIENT);
            message.replyTo = mMessenger;
            try {
                mServiceMessenger.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        unbindService(mServiceConnection);
        mBound = false;
        stopService(new Intent(this, MusicService.class));
        System.out.println("我是activity的destroy");
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
                Message message = Message.obtain(null, 0x124, seekBar.getProgress());
                message.replyTo = mMessenger;
                try {
                    mServiceMessenger.send(message);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            //滑块完成使的行为
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()){
            case R.id.music_play_image:
                if (mStop){
                    mStop = false;
                }
                sendMessageToService(1);
                break;
            case R.id.music_previous_image:
                sendMessageToService(2);
                break;
            case R.id.music_next_image:
                sendMessageToService(3);
                break;
        }
    }


    //给MusicService发广播
    private void sendMessageToService(int what){
        Message message = Message.obtain(null, what);
        message.replyTo = mMessenger;
        try {
            mServiceMessenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
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

}
