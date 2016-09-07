package com.example.w.musicbroadcast;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

/**
 *
 * Created by W on 2016/9/7.
 */
public class MusicAdapter extends BaseAdapter{

    List<MusicInfo> mMusicInfos;

    LayoutInflater mLayoutInflater;

    Context mContext;

    public MusicAdapter(Context context, List<MusicInfo> musicInfos) {
        this.mMusicInfos = musicInfos;
        this.mContext = context;
        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);


    }
    @Override
    public int getCount() {
        return mMusicInfos.size();
    }

    @Override
    public Object getItem(int position) {
        return mMusicInfos.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null){
            viewHolder = new ViewHolder();
            convertView = mLayoutInflater.inflate(R.layout.activit_misic_content_item, parent,false);
            viewHolder.itemSongName = (TextView) convertView.findViewById(R.id.item_song_name);
            viewHolder.itemSongSingerAndAlbum = (TextView) convertView.findViewById(R.id.item_song_singer_and_album);

            convertView.setTag(viewHolder);
        }else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        viewHolder.itemSongName.setText(mMusicInfos.get(position).getTitle());
        viewHolder.itemSongSingerAndAlbum.setText(String.valueOf(mMusicInfos.get(position).getArtist() + " — " + mMusicInfos.get(position).getAlbum()));

        return convertView;
    }


    class ViewHolder{
        TextView itemSongName;
        TextView itemSongSingerAndAlbum;
    }
}
