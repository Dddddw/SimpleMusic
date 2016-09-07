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
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private ImageView mPlay_imageView;
    private ImageView mImageView;
    ImageView prev_imageView;
    ImageView next_imageView;
    private static  SeekBar mSeekBar;
    static boolean mStop = true;
    private TextView mTextView;
    boolean mBound;
    Messenger mServiceMessenger;
    private MusicInfo mMusicInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialView();

        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        Intent intent = getIntent();
        if (TextUtils.equals("listTonView",intent.getAction())){
            mMusicInfo = intent.getParcelableExtra("musicInfo");
            mTextView.setText(String.valueOf(mMusicInfo.getTitle() + " - " + mMusicInfo.getArtist()));
            mPlay_imageView.setImageResource(R.drawable.pause);

        }

        bindService(new Intent(MainActivity.this, MusicService.class), mCon, Context.BIND_AUTO_CREATE);

    }



    Messenger mMessenger = new Messenger(new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 0x70:
                    int status = msg.arg1;
                    switch (status){
                        case 0x11:
                            mPlay_imageView.setImageResource(R.drawable.play);
                            break;
                        case 0x12:
                            Log.i("well", "变成大暂停1");
                            mPlay_imageView.setImageResource(R.drawable.pause);
                            Log.i("well", "变成大暂停2");
                            break;
                    }
                    break;

                case 0x71:
                    break;
            }
            //接收服务发出来的消息
//            switch (msg.what){
//                case 0x123:
//                    //int Max = mSeekBar.getMax();
//                    // int a  = msg.arg1 * Max / msg.arg2;
//                    // mSeekBar.setProgress(a);
//                    break;
//            }

        }
    });



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
//        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
//            @Override
//            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//                seekBar.setProgress(progress);
//                Message message = Message.obtain(null, 0x124, seekBar.getProgress());
//                message.replyTo = mMessenger;
//                try {
//                    mServiceMessenger.send(message);
//                } catch (RemoteException e) {
//                    e.printStackTrace();
//                }
//
//            }
//
//            @Override
//            public void onStartTrackingTouch(SeekBar seekBar) {
//
//            }
//
//            @Override
//            //滑块完成使的行为
//            public void onStopTrackingTouch(SeekBar seekBar) {
//
//            }
//        });
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()){
            case R.id.music_play_image:
                if (mStop){
                    mStop = false;
                }
                break;
            case R.id.music_previous_image:
                break;
            case R.id.music_next_image:
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case android.R.id.home:
                startActivity(new Intent(MainActivity.this, MusicListActivity.class));
                break;

        }

        return true;
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
        unbindService(mCon);
        mBound = false;
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

    ServiceConnection mCon = new ServiceConnection() {
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

}
