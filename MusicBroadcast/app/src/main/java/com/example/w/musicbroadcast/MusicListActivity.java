package com.example.w.musicbroadcast;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.MediaStore.Audio.Media;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MusicListActivity extends AppCompatActivity implements AdapterView.OnItemClickListener, View.OnClickListener{

    public static final int LIST_MSG_PLAY = 0x60;
    ContentResolver mContentResolver;
    private ListView mMusicLists;

    private ArrayList<MusicInfo> mMusicInfoList;
    boolean mBound;
    Messenger mServiceMessenger;
    private ImageView mMiniPlay;
    private TextView mSongName;
    private TextView mSinger;
    private int mPosition;

    static boolean mPlay = false;
    /**
     * 判断歌曲是否选择
     */
    boolean mIsMusicSelected = false;

    Messenger mMessenger = new Messenger(new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case MusicService.PLAY_PAUSE_WHAT:
                    int status = msg.arg1;
                    switch (status){
                        case 0x11:
                            mMiniPlay.setImageResource(R.drawable.mini_play);
                            break;
                        case 0x12:
                            mMiniPlay.setImageResource(R.drawable.mini_pause);
                            break;
                    }
                    break;
                case MusicService.AUTO_PLAY_NEXT_WHAT:
                    miniPlayNext();
                    //int Max = mSeekBar.getMax();
                   // int a  = msg.arg1 * Max / msg.arg2;
                   // mSeekBar.setProgress(a);
                    break;
                default:
                    super.handleMessage(msg);
            }

        }
    });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_list);
        Log.i("well", "onCreate");

        initViews();
        mContentResolver = getContentResolver();

        new MusicTask().execute();

        bindService(new Intent(MusicListActivity.this, MusicService.class), mServiceConnection, Context.BIND_AUTO_CREATE);

    }

    private void initViews() {

        mMusicLists = (ListView) findViewById(R.id.music_list);
        mMiniPlay = (ImageView) findViewById(R.id.mini_play);
        ImageView miniNext = (ImageView) findViewById(R.id.mini_next);
        mSongName = (TextView) findViewById(R.id.mini_song_name);
        mSinger = (TextView) findViewById(R.id.mini_singer);

        mMusicLists.setOnItemClickListener(this);

        RelativeLayout relativeLayout = (RelativeLayout)findViewById(R.id.button_line);

        relativeLayout.setOnClickListener(this);

        miniNext.setOnClickListener(this);
        mMiniPlay.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
            switch (v.getId()){
                case R.id.button_line:
                    Intent intent = new Intent(MusicListActivity.this, MainActivity.class);
                    Bundle data = new Bundle();
                    data.putParcelableArrayList("musicInfoList", mMusicInfoList);
                    if (mIsMusicSelected){
                    data.putInt("position", mPosition);
                    }
                    intent.putExtra("data", data);
                    intent.setAction("buttonToView");
                    startActivity(intent);
                    data.clear();
                    break;
                case R.id.mini_play:
                    if (!mIsMusicSelected){
                        Toast.makeText(MusicListActivity.this, "请先选择歌曲", Toast.LENGTH_SHORT).show();
                    }else {
                        Message message =  Message.obtain(null, 0x61);
                        message.replyTo = mMessenger;
                        try {
                            mServiceMessenger.send(message);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                    if (!mPlay){
                        mPlay = true;
                    }
                    System.out.println(mPlay);
                    break;
                case R.id.mini_next:
                    if (!mIsMusicSelected){
                        Toast.makeText(MusicListActivity.this, "请先选择歌曲", Toast.LENGTH_SHORT).show();
                    }else {
                        miniPlayNext();
                        break;
                    }
            }
        }

    private void miniPlayNext() {
        mPlay = true;
        Message message1 = Message.obtain(null, 0x62);
        Bundle data1 = new Bundle();
        if ( ++mPosition > (mMusicInfoList.size() - 1)){
            mPosition = 0;
        }
        data1.putString("musicUrl", mMusicInfoList.get(mPosition).getUrl());
        message1.setData(data1);
        message1.replyTo = mMessenger;
        try {
            mServiceMessenger.send(message1);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        mSongName.setText(mMusicInfoList.get(mPosition).getTitle());
        mSinger.setText(mMusicInfoList.get(mPosition).getArtist());
        data1.clear();
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //给服务发消息，播放音乐，跳转到MainActivity。
        mIsMusicSelected = true;
        mPlay = true;
        Intent intent = new Intent(MusicListActivity.this, MainActivity.class);
        Bundle data = new Bundle();
        data.putParcelableArrayList("musicInfoList", mMusicInfoList);
        data.putInt("position", position);
        data.putBoolean("isMusicSelected", mIsMusicSelected);
        intent.putExtra("data", data);
        intent.setAction("listTonView");
        startActivity(intent);
        data.clear();

        Message message = Message.obtain(null, LIST_MSG_PLAY);
        data.putString("musicUrl", mMusicInfoList.get(position).getUrl());
        data.putInt("musicDuration", mMusicInfoList.get(position).getDuration());
        message.setData(data);

        try {
            mServiceMessenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }


    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i("well", "onDestroy");
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
     *
     * @return 歌曲信息列表
     */
    private ArrayList<MusicInfo> getMusicList(){
        Cursor cursor = mContentResolver.query(Media.EXTERNAL_CONTENT_URI, null, null, null, null);
        mMusicInfoList = new ArrayList<>();
        if (cursor == null){
            Log.i("ph", "cursor为空");
        }else if (!cursor.moveToFirst()){
            Log.i("ph", "cursor没有检索出来数据");
        }else {
            do{
                MusicInfo musicInfo = new MusicInfo();
                int isMusic = cursor.getInt(cursor.getColumnIndex(Media.IS_MUSIC));
                if (isMusic != 0){
                    musicInfo.setIsMusic(isMusic);
                    musicInfo.setId(cursor.getLong(cursor.getColumnIndex(Media._ID)));
                    musicInfo.setTitle(cursor.getString(cursor.getColumnIndex(Media.TITLE)));
                    musicInfo.setArtist(cursor.getString(cursor.getColumnIndex(Media.ARTIST)));
                    musicInfo.setAlbum(cursor.getString(cursor.getColumnIndex(Media.ALBUM)));
                    musicInfo.setDuration(cursor.getInt(cursor.getColumnIndex(Media.DURATION)));
                    musicInfo.setUrl(cursor.getString(cursor.getColumnIndex(Media.DATA)));
                }
                mMusicInfoList.add(musicInfo);
            }while (cursor.moveToNext());
            cursor.close();
        }

        return mMusicInfoList;
    }


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


    class MusicTask extends AsyncTask<String , Integer, List<MusicInfo>>{

        @Override
        protected List<MusicInfo> doInBackground(String... params) {
            return getMusicList();
        }

        @Override
        protected void onPostExecute(List<MusicInfo> musicInfos) {
            super.onPostExecute(musicInfos);
            MusicAdapter musicAdapter = new MusicAdapter(MusicListActivity.this, musicInfos);
            mMusicLists.setAdapter(musicAdapter);

            Intent intent = getIntent();
            mIsMusicSelected = intent.getBooleanExtra("isMusicSelected",false);
            if (mIsMusicSelected){
                System.out.println(mPlay);
                if (mPlay) {
                    Log.i("well", "底下图标文字变化");
                    mMiniPlay.setImageResource(R.drawable.mini_pause);
                }
                mPosition = intent.getIntExtra("position", -1);
                mSongName.setText(musicInfos.get(mPosition).getTitle());
                mSinger.setText(musicInfos.get(mPosition).getArtist());
            }
        }
    }
}
