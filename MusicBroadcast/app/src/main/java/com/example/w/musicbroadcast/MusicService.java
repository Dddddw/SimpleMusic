package com.example.w.musicbroadcast;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;


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
    //歌曲的列表
    private String[] songList = new String[]{"my.mp3", "girls.mp3", "macerila.mp3"};
    //歌曲是否在播放
    boolean mPlay = false;
    //歌曲是否准备好了
    boolean isPrepare;
    Timer mTimer = new Timer();

    AssetManager am;
    MusicServiceReceiver musicServiceReceiver;
    private static MediaPlayer mMediaPlayer;
    private MainActivity.service_Handler mHandler = new MainActivity.service_Handler(new MainActivity());


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
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

        am = getAssets();
        mMediaPlayer = new MediaPlayer();
        //Service开始就播放指定的第一首歌曲
        playSong(songList[current]);
        mPlay = true;

        pushBackAction(0x12, current);
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
                        Message message = mHandler.obtainMessage();
                        message.arg1 = mMediaPlayer.getCurrentPosition();
                        message.arg2 = mMediaPlayer.getDuration();
                        message.what = 0x123;
                        mHandler.sendMessage(message);
                    }

                }
            },0 ,1000);

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
     * 播放歌曲
     *
     * @param ids 歌曲名称
     */
    private void playSong(String ids){

           try {
               isPrepare = false;
               AssetFileDescriptor afd = am.openFd(ids);
               mMediaPlayer.reset();
               mMediaPlayer.setDataSource(afd.getFileDescriptor(),
                           afd.getStartOffset(), afd.getLength());
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
                            System.out.println(3);

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
        pushBackAction(0x12, current);
        System.out.println(2);
    }

    /**
     * 暂停
     */
    private void Pause(){
        mPlay = false;
        mMediaPlayer.pause();
        pushBackAction(0x11, current);

    }

    /**
     * 播放下一曲
     */
    private void playNext(){
        if (++current > 2){
            current = 0;
        }
        playSong(songList[current]);
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
        playSong(songList[current]);
        pushBackAction(0x12, current);
        mPlay = true;
    }

    /**
     * 给activity回发广播
     *
     * @param status 当前歌曲的状态
     * @param current 当前歌曲的编号
     */
    private void pushBackAction(int status , int current){
        Intent intent = new Intent(MainActivity.UPDATE_ACTION);
        intent.putExtra(MainActivity.STATUS, status);
        intent.putExtra(MainActivity.CURRENT, current);
        sendBroadcast(intent);
    }

    public static class activity_handler extends Handler{

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 0x124){
                mMediaPlayer.seekTo(mMediaPlayer.getDuration() * msg.arg1 / 999);
            }

        }
    }

}
