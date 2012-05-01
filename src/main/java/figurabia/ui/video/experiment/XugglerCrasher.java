/*
 * Copyright (c) 2012 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 03.03.2012
 */
package figurabia.ui.video.experiment;

import java.io.File;
import java.util.Random;

import figurabia.ui.video.access.MediaFrame;
import figurabia.ui.video.access.VideoFormat;
import figurabia.ui.video.access.XugglerMediaInputStream;

public class XugglerCrasher {

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {

        //TODO there was a not working video: (problem was that another video frame came too early)
        // /home/sberner/workspace/Figurabia/figurantdata/vids/10-13.01.09.flv

        // Problem for Figurabia crashes is probably that when player is still fetching and a new fetcher
        // is created with a new actor, it can mean that there are multiple native accesses overlapping
        // Potential solution -> try to reuse fetcher, or wait with creating new fetcher until previous one has stopped

        // Real cause of issue was: On actor shutdown FrameCache deleted the frames, while in FrameFetcher fetching was still in progress
        // Solved by doing a copyReference when creating each frame, storing those references and only deleting those when MediaInputStream is closed

        int n = 100;
        int minBatches = 1;
        int maxBatches = 9;
        int batchSize = 8;
        Random rand = new Random(1);

        for (int i = 0; i < n; i++) {
            int m = (rand.nextInt(maxBatches - minBatches) + minBatches) * batchSize;
            File video = new File(args[i % args.length]);
            System.out.println("Loading " + m + " frames from video " + (i + 1) + " (" + video + ")...");

            XugglerMediaInputStream is = new XugglerMediaInputStream(video);
            VideoFormat vf = is.getVideoFormat();
            double frameRate = vf.getFrameRate();

            // aquire frames
            MediaFrame[] frames = new MediaFrame[m];
            for (int j = 0; j < m; j++) {
                frames[j] = is.createFrame();
            }

            // read frames
            for (int j = 0; j < m; j++) {
                if (j % batchSize == 0)
                    is.setPosition((long) (j / frameRate * 1000));
                is.readFrame(frames[j]);
            }

            // release frames
            is.close();
            for (int j = 0; j < m; j++) {
                frames[j].delete();
            }

            System.out.println("Finished video " + (i + 1) + " (" + video + ")");
        }
    }
}
