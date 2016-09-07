package com.example.w.musicbroadcast;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;


import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;


/**
 *
 * Created by W on 2016/7/31.
 */
public class MusicService extends Service{
    //Service监听的行为
    public static final String CTRL_ACTION = "ctrl_action";
    public static final String USER_ACTION_KEY = "user_action_key";
    //0x11,暂停播放，0x12,正在播放
    //记录当前播放的音乐
    int current = 0;
    //歌曲是否在播放
    boolean mPlay = false;
    //歌曲是否准备好了
    boolean isPrepare;
    Timer mTimer = new Timer();

    ArrayList<Messenger> mClients = new ArrayList<>();

    /**
     * 绑定，解绑服务
     */
    public static final int MSG_REGISTER_CLIENT = 10;
    public static final int MSG_UNREGISTER_CLIENT = 11;

    MusicServiceReceiver musicServiceReceiver;
    private static MediaPlayer mMediaPlayer;


    Messenger mMessenger = new Messenger(new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case MSG_REGISTER_CLIENT:
                    mClients.add(msg.replyTo);
                    break;
                case MSG_UNREGISTER_CLIENT:
                    mClients.remove(msg.replyTo);
                    break;
                case MusicListActivity.LIST_MSG_PLAY:
                    Bundle data = msg.getData();
                    playSong(data.getString("musicUrl"));
                    Message message = Message.obtain(null, 0x70, 0x12 , 10010);
                    for(int i=mClients.size()-1; i>=0; i--){
                        try {
                            mClients.get(i).send(message);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                            mClients.remove(i);
                        }
                    }
                    break;
                case 0x61:
                    playPre();
                    break;
                case 0x63:
                    playNext();
                    break;
                case 0x64:
                    mMediaPlayer.seekTo(mMediaPlayer.getDuration() * (int)(msg.obj) / 999);
                    break;
                default:
                    super.handleMessage(msg);

            }
        }
    });

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        return mMessenger.getBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(this.getClass().getSimpleName(), "onCreate");

        //注册广播
        musicServiceReceiver = new MusicServiceReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(CTRL_ACTION);
        registerReceiver(musicServiceReceiver, filter);

        mMediaPlayer = new MediaPlayer();

//        pushBackAction(0x12, current);

        //一首歌播放完的时候自动播放下一曲
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                playNext();
            }
        });
        //MediaPlayer seekTo完了调用的监听器
        mMediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
            @Override
            public void onSeekComplete(MediaPlayer mp) {
                if (!mPlay){
                    return;
                }
                mMediaPlayer.start();
            }
        });

        refreshSeekBar();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return START_NOT_STICKY;

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isPrepare = false;
        mTimer.cancel();
        unregisterReceiver(musicServiceReceiver);
        mMediaPlayer.release();
        System.out.println("我是service的destroy");


    }
    /**
     * 更新SeekBar操作，每隔0.1s向Activity发送消息更新SeekBar的操作
     * 因为定时器一直在运行，所以需要加个判断，去判断MediaPlayer是否准备好
     * 在未准备好之前调用getDuration()会报错
     */
    private void refreshSeekBar() {
            mTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    while (isPrepare){
                        Message message = Message.obtain(null, 0x123, mMediaPlayer.getCurrentPosition(),mMediaPlayer.getDuration());
                        try {
                            mClients.get(0).send(message);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }

                }
            },0 ,1000);

    }

    /**
     * 播放歌曲
     *
     *
     */
    private void playSong(String url){
           try {
               isPrepare = false;
               mMediaPlayer.reset();
               mMediaPlayer.setDataSource(url);
               mMediaPlayer.prepare();
               isPrepare = true;
               mMediaPlayer.start();
               } catch (Exception e) {
                   e.printStackTrace();
               }

    }




    class MusicServiceReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (CTRL_ACTION.contains(action)){
                int control = intent.getIntExtra(MusicService.USER_ACTION_KEY, -1);
                switch (control){
                    case 1:
                        if (mPlay){
                            Pause();
                        }else {
                            Play();
                            mPlay = true;
                        }
                        break;
                    case 2:
                        playPre();
                        break;
                    case 3:
                        playNext();
                        break;

                }


            }

        }

    }

    /**
     * 播放
     */
    private void Play(){

        mPlay = true;
        mMediaPlayer.start();
        sendMessageToActivity(0x12, current);
        pushBackAction(0x12, current);
        System.out.println(2);
    }

    /**
     * 暂停
     */
    private void Pause(){
        mPlay = false;
        mMediaPlayer.pause();
        sendMessageToActivity(0x11, current);
        pushBackAction(0x11, current);
    }

    /**
     * 播放下一曲
     */
    private void playNext(){
        if (++current > 2){
            current = 0;
        }
        //playSong(songList[current]);
        sendMessageToActivity(0x12, current);
        pushBackAction(0x12, current);
        mPlay = true;
    }

    /**
     * 播放上一曲
     */
    private void playPre(){
        if (--current < 0){
            current = 2;
        }
        //playSong(songList[current]);
        sendMessageToActivity(0x12, current);
        pushBackAction(0x12, current);
        mPlay = true;
    }


    private void sendMessageToActivity(int status , int current){
        Message message = Message.obtain(null, 0, status, current);
        try {
            mClients.get(0).send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    /**
     * 给weight回发广播
     *
     * @param status 当前歌曲的状态
     * @param current 当前歌曲的编号
     */
    private void pushBackAction(int status, int current) {
        Intent intent = new Intent(MusicWidget.UPDATE_ACTION);
        intent.putExtra(MusicWidget.STATUS, status);
        intent.putExtra(MusicWidget.CURRENT, current);
        sendBroadcast(intent);
    }


}
