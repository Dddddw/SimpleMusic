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


public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private ImageView mPlay_imageView;
    private ImageView mImageView;
    private TextView mTextView;
    ImageView prev_imageView;
    ImageView next_imageView;
    private static  SeekBar mSeekBar;

    /**
     * 判断是否绑定服务
     */
    boolean mBound;
    private int mPosition;
    private ArrayList<MusicInfo> mMusicInfoList;
    Messenger mServiceMessenger;

    boolean mIsMusicSelected = false;

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
                break;
            case "buttonToView":
                if (!MusicListActivity.mPlay){
                    if (mPosition == -1){
                        //没有选音乐文件的时候
                        Toast.makeText(MainActivity.this, "请先选择音乐", Toast.LENGTH_SHORT).show();
                    }else {
                        //歌曲在播放但是暂停的时候
                        mTextView.setText(mMusicInfoList.get(mPosition).getTitle());
                        mIsMusicSelected = true;
                    }
                }else{
                    //歌曲正在播放
                    mTextView.setText(mMusicInfoList.get(mPosition).getTitle());
                    mPlay_imageView.setImageResource(R.drawable.pause);
                    mIsMusicSelected = true;
                }

                break;
        }

        bindService(new Intent(MainActivity.this, MusicService.class), mCon, Context.BIND_AUTO_CREATE);

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
            }

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
                    if (!MusicListActivity.mPlay){
                        MusicListActivity.mPlay = true;
                    }
                    break;
                case R.id.music_previous_image:
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
        Message message = Message.obtain(null, 0x62);
        Bundle data = new Bundle();
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
        MusicListActivity.mPlay = true;
        return mMusicInfoList.get(mPosition).getUrl();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case android.R.id.home:
                Intent intent = new Intent(MainActivity.this, MusicListActivity.class);
                if (mIsMusicSelected){
                    intent.putExtra("position", mPosition);
                    intent.putExtra("isMusicSelected", mIsMusicSelected);
                }
                startActivity(intent);

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
