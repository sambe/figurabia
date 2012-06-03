/*
 * Copyright (c) 2012 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on May 20, 2012
 */
package figurabia.ui.video.access;

import java.io.File;

import org.junit.Test;

public class MediaAnalyzerTest {

    @Test
    public void testAnalyze() throws Exception {

        //File file = new File("/home/sberner/Desktop/10-07.04.09.flv");
        //File file = new File("/home/sberner/Desktop/10-21.04.09.flv");
        File file = new File("/home/sberner/Desktop/10-31.03.09.flv");
        //File file = new File("/home/sberner/media/salsavids/m2/MOV00357.MP4");
        //File file = new File("/home/sberner/Desktop/10-07.04.09.flv");

        //File file = new File("/home/sberner/media/salsavids/salsabrosa/10-08.07.08_.flv");
        //File file = new File("/home/sberner/media/films/Gwen_Stefani_-_What_You_Waiting_For-2004-bVz.m2v");
        //File file = new File("/home/sberner/media/films/shakira-dont_bother_(at_mtv_ema_2005).mpg");

        MediaInfo info = MediaAnalyzer.analyze(file);

    }
}
