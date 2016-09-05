package com.example.w.musicbroadcast;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/**
 *
 * Created by W on 2016/8/4.
 */
public class MusicWidget extends AppWidgetProvider{

    public static final String WIDGET_ACTION = "widget_action";

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (intent != null){
            String action = intent.getAction();
            if (WIDGET_ACTION.contains(action)){
                String button_name = intent.getStringExtra(WIDGET_ACTION);
                switch (button_name){
                    case "play":
                        pushAction(context, 1);
                        if (MainActivity.mStop){
                            Intent mIntent = new Intent(context, MusicService.class);
                            context.startService(mIntent);
                            MainActivity.mStop = false;
                        }
                        System.out.println("我是播放");
                        break;
                    case "previous":
                        pushAction(context, 2);
                        break;
                    case "next":
                        System.out.println("我是下一首");
                        pushAction(context, 3);
                        break;

                }
            }else if (MainActivity.UPDATE_ACTION.contains(action)){
                int current = intent.getIntExtra(MainActivity.CURRENT, -1);
                int status = intent.getIntExtra(MainActivity.STATUS, -1);
                update_View(context,AppWidgetManager.getInstance(context), current, status);

            }
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);


        update_View(context, appWidgetManager, -1 ,1);
    }


    private PendingIntent getPendingIntent(Context context, int requestCode ,String button_name) {
        Intent intent = new Intent(WIDGET_ACTION);
        intent.setClass(context, MusicWidget.class);
        intent.putExtra(WIDGET_ACTION, button_name);
        return PendingIntent.getBroadcast(context, requestCode, intent, 0);
    }

    /**
     * 给MusicService回发广播
     *
     * @param context 上下文
     * @param action 发送的行为
     */
    private void pushAction(Context context, int action){
        Intent intent = new Intent(MusicService.CTRL_ACTION);
        intent.putExtra(MusicService.USER_ACTION_KEY, action);
        context.sendBroadcast(intent);
    }

    /**
     * 更新view操作
     *
     * @param context 上下文
     * @param appWidgetManager AppWidgetManager对象
     * @param current 当前歌曲的编号
     * @param status 当前歌曲播放的状态
     */
    private void update_View(Context context, AppWidgetManager appWidgetManager, int current ,int status ){
        //加载指定的页面布局文件
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);


        remoteViews.setOnClickPendingIntent(R.id.music_play_button, getPendingIntent(context, 0, "play"));
        remoteViews.setOnClickPendingIntent(R.id.music_next_button, getPendingIntent(context, 1, "next"));
        remoteViews.setOnClickPendingIntent(R.id.music_previous_button, getPendingIntent(context, 2, "previous"));

        if (current >= 0){
            remoteViews.setTextViewText(R.id.music_name_text, MainActivity.songName[current]);
            remoteViews.setImageViewResource(R.id.music_image, MainActivity.songPicture[current]);
        }
        switch (status){
            case 0x11:
                remoteViews.setImageViewResource(R.id.music_play_button, R.drawable.play);
                break;
            case 0x12:
                remoteViews.setImageViewResource(R.id.music_play_button, R.drawable.pause);
                break;
        }

        ComponentName componentName = new ComponentName(context, MusicWidget.class);
        appWidgetManager.updateAppWidget(componentName, remoteViews);
    }

}
