/*
 * Copyright (c) 2012 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on Jul 8, 2012
 */
package figurabia.io;

import exmoplay.access.MediaInfo;
import figurabia.io.store.Identifiable;

/**
 * Stores all the meta data known about a video
 * 
 * @author Samuel Berner
 */
public class VideoMetaData implements Identifiable {

    private String id;
    private String rev;
    private String md5Sum;
    private MediaInfo mediaInfo;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getRev() {
        return rev;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public void setRev(String rev) {
        this.rev = rev;
    }

    /**
     * @return the md5Sum
     */
    public String getMd5Sum() {
        return md5Sum;
    }

    /**
     * @param md5Sum the md5Sum to set
     */
    public void setMd5Sum(String md5Sum) {
        this.md5Sum = md5Sum;
    }

    /**
     * @return the mediaInfo
     */
    public MediaInfo getMediaInfo() {
        return mediaInfo;
    }

    /**
     * @param mediaInfo the mediaInfo to set
     */
    public void setMediaInfo(MediaInfo mediaInfo) {
        this.mediaInfo = mediaInfo;
    }
}
