package com.example.w.musicbroadcast;

import android.app.Notification;
import android.app.PendingIntent;
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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.RemoteViews;


import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;


/**
 *
 * Created by W on 2016/7/31.
 */
public class MusicService extends Service{
    //Service监听的行为
    public static final String NOTIFICATION_ACTION = "notification_action";
    public static final String CTRL_ACTION = "ctrlActionFromNotification";
    public static final String USER_ACTION_KEY = "user_action_key";

    public static final int PLAY_PAUSE_WHAT = 0x70;
    public static final int AUTO_PLAY_NEXT_WHAT = 0x71;
    public static final int REFRESH_SEEK_BAR_WHAT = 0x72;

    public static final int MSG_REGISTER_CLIENT = 10;
    public static final int MSG_UNREGISTER_CLIENT = 11;
    //0x11,暂停播放，0x12,正在播放

    /**
     * 歌曲是否在播放
     */
    static boolean  mPlay = false;

    /**
     * 歌曲是否准备好
     */
    boolean isPrepare;
    Timer mTimer = new Timer();
    ArrayList<Messenger> mClients = new ArrayList<>();
    MusicServiceReceiver mMusicServiceReceiver;
    private MediaPlayer mMediaPlayer;

    /**
     * 服务端处理消息
     */
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
                    updateImage(getApplicationContext(),0x12);
                    Bundle data = msg.getData();
                    updateText(getApplicationContext(), 1,data);
                    playSong(data.getString("musicUrl"));
                    break;
                case 0x61:
                    if (mPlay){
                        Pause();
                    }else {
                        Play();
                        Log.i("well", "mPlay:true");
                    }
                    break;
                case 0x62:
                    if (!mPlay){
                        sendMessageToActivity(PLAY_PAUSE_WHAT, 0x12, 0);
                        updateImage(getApplicationContext(),0x12);
                    }
                    Log.i("well", "下一首5");
                    Bundle data1 = msg.getData();
                    updateText(getApplicationContext(),1,data1);
                    playSong(data1.getString("musicUrl"));

                    break;
                case 0x63:
                    if (msg.arg2 == 1){
                        mMediaPlayer.seekTo(mMediaPlayer.getDuration() * (msg.arg1) / 999);
                    }
                    break;
                default:
                    super.handleMessage(msg);

            }
        }
    });

    @Override
    public void onCreate() {
        super.onCreate();
        mMediaPlayer = new MediaPlayer();

        /**
         * 注册广播
         */
        mMusicServiceReceiver = new MusicServiceReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(NOTIFICATION_ACTION);
        registerReceiver(mMusicServiceReceiver, filter);

//      pushBackAction(0x12, current);

        /**
         * 歌曲播放完默认自动播放下一首
         */
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                sendMessageToActivity(AUTO_PLAY_NEXT_WHAT, 1000, 1000);
            }
        });

        /**
         * MediaPlayer的seekTo()调用完了会回调的方法
         */
        mMediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
            @Override
            public void onSeekComplete(MediaPlayer mp) {
                if (!mPlay){
                    return;
                }
                mp.start();

            }
        });

        updateText(getApplicationContext(), 0, null);

        refreshSeekBar();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        return mMessenger.getBinder();
    }

    /**
     * 获取PendingIntent对象
     *
     * @param context 上下文
     * @param requestCode 请求码，每个必须不一样
     * @param buttonName 传入按钮名字，在广播接收方法中以便于区分
     * @return PendingIntent 对象
     */
    private PendingIntent getPendingIntent(Context context, int requestCode, String buttonName) {
        Intent intent = new Intent(NOTIFICATION_ACTION);
        intent.putExtra(NOTIFICATION_ACTION, buttonName);
        return PendingIntent.getBroadcast(context, requestCode, intent, 0);
    }

    /**
     *
     * @return 返回Notification.Builder对象
     */
    private Notification.Builder getBuilder() {

        return new Notification.Builder(this.getApplicationContext())
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.music_player_mini)
                .setContentTitle("Notification");

    }

    /**
     *
     *
     * @param context 上下文
     * @return RemoteViews对象，好像RemoteViews最好不用复用
     */
    @NonNull
    private RemoteViews getRemoteViews(Context context) {

        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification_layout);
        remoteViews.setOnClickPendingIntent(R.id.no_play, getPendingIntent(context, 1 ,"play"));
        remoteViews.setOnClickPendingIntent(R.id.no_next, getPendingIntent(context, 2 ,"next"));
        return remoteViews;
    }

    /**
     * 更新文字
     *
     * @param context 上下文
     * @param a 判断对象
     * @param data 数据
     */
    private void updateText(Context context, int a, Bundle data){
        Log.i("well", "下一首6");
        RemoteViews remoteViews = getRemoteViews(context);
        if (a == 1) {
            remoteViews.setTextViewText(R.id.no_song_name, data.getString("musicName"));
            remoteViews.setTextViewText(R.id.no_singer,data.getString("musicSinger"));
        }
        Notification notification = getBuilder().setContent(remoteViews).build();
        startForeground(2016, notification);
    }

    /**
     * 更新暂停播放图片
     * @param context 上下文
     * @param status 歌曲状态
     */
    private void updateImage(Context context, int status){

        RemoteViews remoteViews = getRemoteViews(context);
        switch (status){
            case 0x11:
                remoteViews.setImageViewResource(R.id.no_play, R.drawable.mini_play);
                break;
            case 0x12:
                remoteViews.setImageViewResource(R.id.no_play, R.drawable.mini_pause);
                break;
        }
        Notification notification = getBuilder().setContent(remoteViews).build();
        startForeground(2016, notification);
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
        mMediaPlayer.release();
        stopForeground(true);
        unregisterReceiver(mMusicServiceReceiver);
        System.out.println("我是service的destroy");


    }
    /**
     * 更新SeekBar操作，每隔0.1s向Activity发送消息更新SeekBar的操作
     * 因为定时器一直在运行，所以需要加个判断，去判断MediaPlayer是否准备好
     * 在未准备好之前调用getDuration()会报错
     */
    private  void refreshSeekBar() {
                mTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        while (isPrepare){
                            sendMessageToActivity(REFRESH_SEEK_BAR_WHAT, mMediaPlayer.getCurrentPosition(),mMediaPlayer.getDuration());
                        }
                    }
                },0 ,100);
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
        mPlay = true;
    }

    /**
     * 播放
     */
    private void Play(){
        updateImage(getApplicationContext(),0x12);
        mPlay = true;
        mMediaPlayer.start();
        sendMessageToActivity(PLAY_PAUSE_WHAT, 0x12, 0);
    }

    /**
     * 暂停
     */
    private void Pause(){
        updateImage(getApplicationContext(),0x11);
        mPlay = false;
        Log.i("well", "mPlay:false");
        mMediaPlayer.pause();

        sendMessageToActivity(PLAY_PAUSE_WHAT,0x11, 0);
    }



    private void sendMessageToActivity(int what, int status , int arg2){
        if (what == 0x71){
        Log.i("well", "下一首2");
        }
        Message message = Message.obtain(null, what, status, arg2);
        try {
            for (int i = mClients.size() - 1; i >= 0 ; i--){
                mClients.get(i).send(message);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    class MusicServiceReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("well", "接收广播");
            String action = intent.getAction();
            if (MusicService.NOTIFICATION_ACTION.contains(action)){
                String button_name = intent.getStringExtra(NOTIFICATION_ACTION);
                switch (button_name){
                    case "play":
                        Log.i("well","我是播放");
                        if (mPlay){
                            Pause();
                        }else {
                            Play();
                        }
                        break;
                    case "next":
                        Log.i("well", "下一首1");
                        sendMessageToActivity(AUTO_PLAY_NEXT_WHAT,100,100);
                        break;

                }

            }
        }
    }
//    /**
//     * 给weight回发广播
//     *
//     * @param status 当前歌曲的状态
//     * @param current 当前歌曲的编号
//     */
//    private void pushBackAction(int status, int current) {
//        Intent intent = new Intent(MusicWidget.UPDATE_ACTION);
//        intent.putExtra(MusicWidget.STATUS, status);
//        intent.putExtra(MusicWidget.CURRENT, current);
//        sendBroadcast(intent);
//    }

}
