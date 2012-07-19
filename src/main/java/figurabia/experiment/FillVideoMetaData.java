/*
 * Copyright (c) 2012 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on Jul 15, 2012
 */
package figurabia.experiment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;

import exmoplay.access.MediaAnalyzer;
import figurabia.io.VideoMetaData;
import figurabia.io.VideoMetaDataStore;
import figurabia.io.workspace.LocalFileWorkspace;
import figurabia.io.workspace.Workspace;

public class FillVideoMetaData {

    public static void main(String[] args) throws Exception {
        Workspace ws = new LocalFileWorkspace(new File("figurantdata"));
        VideoMetaDataStore metaDataStore = new VideoMetaDataStore(ws, "/vids/meta");

        for (String video : ws.list("/vids")) {
            String videoName = video.substring("/vids/".length());
            VideoMetaData md = new VideoMetaData();
            md.setId(videoName);
            md.setMd5Sum(calculateMD5(ws, video));
            md.setMediaInfo(MediaAnalyzer.analyze(ws.fileForReading(video)));
            metaDataStore.createWithId(md);
            System.out.println("Added meta data info for " + videoName);
        }
    }

    private static String calculateMD5(Workspace ws, String path) throws IOException {
        // generate md5/sha1 or the like of the file
        String md5;
        InputStream is = null;
        try {
            is = ws.read(path);
            md5 = DigestUtils.md5Hex(is);
        } finally {
            IOUtils.closeQuietly(is);
        }
        return md5;
    }
}
