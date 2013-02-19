/*
 * Copyright (c) 2012 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on Jul 8, 2012
 */
package figurabia.io;

import java.util.HashMap;
import java.util.Map;

import com.thoughtworks.xstream.XStream;

import exmoplay.access.MediaInfo;
import figurabia.io.store.StoreListener;
import figurabia.io.store.XStreamStore;
import figurabia.io.workspace.Workspace;

public class VideoMetaDataStore extends XStreamStore<VideoMetaData> {

    private Map<String, VideoMetaData> byMD5 = new HashMap<String, VideoMetaData>();

    public VideoMetaDataStore(Workspace workspace, String basePath) {
        super(createXStream(), workspace, basePath, VideoMetaData.class);

        for (VideoMetaData vmd : allObjects()) {
            byMD5.put(vmd.getMd5Sum(), vmd);
        }
        addStoreListener(new StoreListener<VideoMetaData>() {
            @Override
            public void update(figurabia.io.store.StoreListener.StateChange change, VideoMetaData o) {
                switch (change) {
                case CREATED:
                    byMD5.put(o.getMd5Sum(), o);
                    break;
                case UPDATED:
                    // should never happen, MD5 cannot change
                    break;
                case DELETED:
                    byMD5.remove(o.getMd5Sum());
                    break;
                }
            }
        });
    }

    private static XStream createXStream() {
        XStream xstream = new XStream();
        xstream.alias("VideoMetaData", VideoMetaData.class);
        xstream.alias("MediaInfo", MediaInfo.class);
        xstream.omitField(MediaInfo.class, "audioSamplesInfoBySamplesOffset");
        xstream.omitField(MediaInfo.class, "videoPictureInfoDecompressed");
        return xstream;
    }

    public VideoMetaData getByMD5(String md5) {
        return byMD5.get(md5);
    }
}
