package com.example.w.musicbroadcast;

import android.os.Parcel;
import android.os.Parcelable;

/**
 *
 * Created by W on 2016/9/7.
 */
public class MusicInfo implements Parcelable {
    /**
     * 歌曲id
     */
    private long id;

    /**
     * 歌曲标题
     */
    private String title;

    /**
     * 艺术家
     */
    private String artist;

    /**
     * 歌曲长度
     */
    private int duration;

    /**
     * 文件目录
     */
    private String url;

    /**
     * 是否为文件
     */
    private int isMusic;

    /**
     * 专辑
     */
    private String album;

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public int getIsMusic() {
        return isMusic;
    }

    public void setIsMusic(int isMusic) {
        this.isMusic = isMusic;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.id);
        dest.writeString(this.title);
        dest.writeString(this.artist);
        dest.writeInt(this.duration);
        dest.writeString(this.url);
        dest.writeInt(this.isMusic);
        dest.writeString(this.album);
    }

    public MusicInfo() {
    }

    protected MusicInfo(Parcel in) {
        this.id = in.readLong();
        this.title = in.readString();
        this.artist = in.readString();
        this.duration = in.readInt();
        this.url = in.readString();
        this.isMusic = in.readInt();
        this.album = in.readString();
    }

    public static final Parcelable.Creator<MusicInfo> CREATOR = new Parcelable.Creator<MusicInfo>() {
        @Override
        public MusicInfo createFromParcel(Parcel source) {
            return new MusicInfo(source);
        }

        @Override
        public MusicInfo[] newArray(int size) {
            return new MusicInfo[size];
        }
    };
}
