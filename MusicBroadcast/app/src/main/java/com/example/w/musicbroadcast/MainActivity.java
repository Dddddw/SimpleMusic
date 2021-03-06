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
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity implements View.OnClickListener,SeekBar.OnSeekBarChangeListener{

    private ImageView mPlay_imageView;
    private TextView mTextView;
    private SeekBar mSeekBar;

    private int mPosition;
    ArrayList<MusicInfo> mMusicInfoList;
    Messenger mServiceMessenger;

    /**
     * 判断是否绑定服务
     */
    boolean mBound;
    boolean mIsMusicSelected = false;
    boolean mPlay;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialView();

        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        Intent intent = getIntent();
        String action = intent.getAction();
        Bundle data = intent.getBundleExtra("data");
        mMusicInfoList = data.getParcelableArrayList("musicInfoList");
        mPosition = data.getInt("position", -1);

        switch (action){
            case "listTonView":
                mIsMusicSelected = data.getBoolean("isMusicSelected");
                mTextView.setText(mMusicInfoList.get(mPosition).getTitle());
                mPlay_imageView.setImageResource(R.drawable.pause);
                mPlay = true;
                break;
            case "buttonToView":
                mPlay = data.getBoolean("isPlay");
                if (mPosition == -1){
                    Toast.makeText(MainActivity.this, "请先选择音乐", Toast.LENGTH_SHORT).show();
                }else {
                    if(mPlay) {
                        mPlay_imageView.setImageResource(R.drawable.pause);
                    }
                    mTextView.setText(mMusicInfoList.get(mPosition).getTitle());
                    mIsMusicSelected = true;
                }
                break;
        }

        bindService(new Intent(MainActivity.this, MusicService.class), mCon, Context.BIND_AUTO_CREATE);

    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case android.R.id.home:
                Intent intent = new Intent(MainActivity.this, MusicListActivity.class);
                if (mIsMusicSelected){
                    intent.putExtra("position", mPosition);
                    intent.putExtra("isMusicSelected", mIsMusicSelected);
                    intent.putExtra("isPlay", mPlay);
                }
                startActivity(intent);

                break;
        }
        return true;
    }

    Messenger mMessenger = new Messenger(new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case MusicService.PLAY_PAUSE_WHAT:
                    int status = msg.arg1;
                    switch (status){
                        case 0x11:
                            mPlay_imageView.setImageResource(R.drawable.play);
                            break;
                        case 0x12:
                            mPlay_imageView.setImageResource(R.drawable.pause);
                            break;
                    }
                    break;

                case MusicService.AUTO_PLAY_NEXT_WHAT:
                    preOrNext(playNext());
                    break;
                case MusicService.REFRESH_SEEK_BAR_WHAT:
                    int Max = mSeekBar.getMax();
                    int a  = msg.arg1 * Max / msg.arg2;
                    mSeekBar.setProgress(a);
            }

        }
    });


    /**
     * 初始化控件，并设置监听
     */
    private void initialView(){
        mPlay_imageView = (ImageView) findViewById(R.id.music_play_image);
        ImageView prev_imageView = (ImageView) findViewById(R.id.music_previous_image);
        ImageView next_imageView = (ImageView) findViewById(R.id.music_next_image);
        mTextView = (TextView) findViewById(R.id.music_song_name_text);
        mSeekBar = (SeekBar) findViewById(R.id.music_seek_bar);
        mSeekBar.setMax(999);

        mPlay_imageView.setOnClickListener(this);
        mSeekBar.setOnSeekBarChangeListener(this);
        prev_imageView.setOnClickListener(this);
        next_imageView.setOnClickListener(this);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if(fromUser){
            Message message = Message.obtain(null, 0x63, progress, 1);
            try {
                mServiceMessenger.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onClick(View v) {
        if (mIsMusicSelected){
            switch (v.getId()){
                case R.id.music_play_image:
                    Message message =  Message.obtain(null, 0x61);
                    message.replyTo = mMessenger;
                    try {
                        mServiceMessenger.send(message);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    mPlay = !mPlay;
                    break;
                case R.id.music_previous_image:
                    Log.i("well", "41");
                    preOrNext(playPre());
                    break;
                case R.id.music_next_image:
                    preOrNext(playNext());
                    break;
            }
        }else {
            Toast.makeText(MainActivity.this, "请先选择音乐", Toast.LENGTH_SHORT).show();
        }

    }

    /**
     * 发送Message
     * @param url 歌曲路径
     */
    private void preOrNext(String url){
        Log.i("well", "下一首4");
        Message message = Message.obtain(null, 0x62);
        Bundle data = new Bundle();
        data.putString("musicName", mMusicInfoList.get(mPosition).getTitle());
        data.putString("musicSinger", mMusicInfoList.get(mPosition).getArtist());
        data.putString("musicUrl", url);
        message.setData(data);
        message.replyTo = mMessenger;
        try {
            mServiceMessenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        data.clear();
    }

    /**
     *
     *
     * @return url
     */
    private String playNext() {
        Log.i("well", "下一首3");
        if ( ++mPosition > (mMusicInfoList.size() - 1)){
            mPosition = 0;
        }
        Log.i("po","" + mPosition);
        mTextView.setText(mMusicInfoList.get(mPosition).getTitle());
        return mMusicInfoList.get(mPosition).getUrl();
    }

    /**
     * 播放上一曲
     *
     * @return url
     */
    private String playPre() {
        if ( --mPosition < 0){
            mPosition = (mMusicInfoList.size() - 1);
        }
        Log.i("po","" + mPosition);
        mTextView.setText(mMusicInfoList.get(mPosition).getTitle());
        mPlay = true;
        return mMusicInfoList.get(mPosition).getUrl();
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
}
