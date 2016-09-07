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
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MusicListActivity extends AppCompatActivity implements AdapterView.OnItemClickListener{

    public static final int LIST_MSG_PLAY = 0x60;
    ContentResolver mContentResolver;
    private ListView mMusicLists;

    private List<MusicInfo> mMusicInfoList;
    boolean mBound;
    Messenger mServiceMessenger;
    private ImageView mMiniPlay;
    private TextView mSongName;
    private TextView mSinger;

    Messenger mMessenger = new Messenger(new Handler(){
        @Override
        public void handleMessage(Message msg) {
            Log.i("well", Thread.currentThread() + "");
            switch (msg.what){

                case 0x70:
                    int status = msg.arg1;
                    switch (status){
                        case 0x11:
                            mMiniPlay.setImageResource(R.drawable.mini_play);
                            break;
                        case 0x12:
                            Log.i("well","变成暂停1");
                            mMiniPlay.setImageResource(R.drawable.mini_pause);
                            Log.i("well", "变成暂停2");
                            break;
                    }
                    break;
                case 0x71:
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

        initViews();
        mContentResolver = getContentResolver();

        new MusicTask().execute();

        bindService(new Intent(MusicListActivity.this, MusicService.class), mServiceConnection, Context.BIND_AUTO_CREATE);

    }

    private void initViews() {

        mMusicLists = (ListView) findViewById(R.id.music_list);
        mMiniPlay = (ImageView) findViewById(R.id.mini_play);
        mSongName = (TextView) findViewById(R.id.mini_song_name);
        mSinger = (TextView) findViewById(R.id.mini_singer);

        mMusicLists.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //给服务发消息，播放音乐，同时更新MainActivity的ui和MusicListActivity的ui，跳转到MainActivity。
        Intent intent = new Intent(MusicListActivity.this, MainActivity.class);
        intent.putExtra("musicInfo", mMusicInfoList.get(position));
        intent.setAction("listTonView");
        startActivity(intent);

        Message message = Message.obtain(null, LIST_MSG_PLAY);
        Bundle data = new Bundle();
        data.putString("musicUrl", mMusicInfoList.get(position).getUrl());
        data.putInt("musicDuration", mMusicInfoList.get(position).getDuration());
        message.setData(data);
        try {
            mServiceMessenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }


        mSongName.setText(mMusicInfoList.get(position).getTitle());
        mSinger.setText(mMusicInfoList.get(position).getArtist());

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
     *
     * @return 歌曲信息列表
     */
    private List<MusicInfo> getMusicList(){
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

        }
    }

}
