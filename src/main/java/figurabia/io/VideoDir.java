/*
 * Copyright (c) 2012 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on Jul 8, 2012
 */
package figurabia.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;

import exmoplay.access.MediaAnalyzer;
import figurabia.io.workspace.Workspace;

public class VideoDir {

    private final Workspace workspace;
    private final String basePath;
    private final VideoMetaDataStore metaDataStore;

    public VideoDir(Workspace workspace, String basePath, VideoMetaDataStore metaDataStore) {
        this.workspace = workspace;
        this.basePath = basePath;
        this.metaDataStore = metaDataStore;
    }

    /**
     * Adds a video to the collection of videos available. Returns the video id (file name).
     * 
     * @param localVideoFile a video file on the user's file system
     * @return the name under which the video was stored on the server
     */
    public String addVideo(File localVideoFile) throws IOException {
        // generate md5/sha1 or the like of the file
        String md5;
        InputStream is = null;
        try {
            is = new FileInputStream(localVideoFile);
            md5 = DigestUtils.md5Hex(is);
        } finally {
            IOUtils.closeQuietly(is);
        }

        // look for existing video (if it exists just return that)
        VideoMetaData existingMetaData = metaDataStore.getByMD5(md5);
        if (existingMetaData != null)
            return existingMetaData.getId();
        else
            // otherwise copy it in (via stream to support client/server later on)
            return uploadVideo(localVideoFile, md5);

    }

    private String uploadVideo(File localVideoFile, String md5Sum) throws IOException {
        // generate video name, which is also used as an id
        // try to reuse original video name, add extension in case of name collision
        String videoId;
        String videoPath;
        int counter = 0;
        do {
            videoId = localVideoFile.getName();
            if (counter > 0) {
                int extPos = videoId.lastIndexOf('.');
                videoId = videoId.substring(0, extPos) + "_" + counter + videoId.substring(extPos);
            }
            counter++;
            videoPath = basePath + "/" + videoId;
        } while (workspace.exists(videoPath));

        // copy video file to server
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(localVideoFile);
            os = workspace.write(videoPath);
            IOUtils.copy(is, os);
        } finally {
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(os);
        }

        // add an entry to metaDataStore
        VideoMetaData md = new VideoMetaData();
        md.setId(videoId);
        md.setMd5Sum(md5Sum);
        md.setMediaInfo(MediaAnalyzer.analyze(localVideoFile));
        metaDataStore.createWithId(md);

        return videoId;
    }
}
