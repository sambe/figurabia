/*
 * Copyright (c) 2009 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 24.05.2009
 */
package figurabia.experiment;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.media.Buffer;
import javax.media.Codec;
import javax.media.Demultiplexer;
import javax.media.Format;
import javax.media.Manager;
import javax.media.MediaException;
import javax.media.MediaLocator;
import javax.media.Track;
import javax.media.format.AudioFormat;
import javax.media.protocol.DataSource;

/**
 * Contains utility methods to create JMF classes.
 * 
 * @author Samuel Berner
 */
public class JMFUtil {

    public static Demultiplexer createParser(DataSource ds) throws IOException, MediaException {
        Demultiplexer d = new com.omnividea.media.parser.video.Parser();
        d.setSource(ds);
        return d;
    }
    
    public static Codec createVideoDecoder(Format inputFormat) throws MediaException {
        Codec c = new com.omnividea.media.codec.video.NativeDecoder();
        c.setInputFormat(inputFormat);
        c.open();
        return c;
    }
    
    public static Codec createAudioDecoder(Format inputFormat) throws MediaException {
        Codec c = new com.omnividea.media.codec.audio.NativeDecoder();
        c.setInputFormat(inputFormat);
        c.open();
        return c;
    }
    
    public static short[] getAudioTrack(File file) throws IOException, MediaException {
        return getAudioTrack("file:" + file.getAbsolutePath());
    }
    
    public static short[] getAudioTrack(String url) throws IOException, MediaException {
        MediaLocator ml = new MediaLocator(url);
        DataSource ds = Manager.createDataSource(ml);
        Demultiplexer parser = JMFUtil.createParser(ds);
        Track audioTrack = null;
        for( Track t : parser.getTracks()) {
            if(t.getFormat() instanceof AudioFormat) {
                if( audioTrack != null)
                    throw new IllegalStateException("Found more than one audio track.");
                audioTrack = t;
            }
            else {
                t.setEnabled(false);
            }
        }
        if( audioTrack == null)
            throw new IllegalStateException("Found no Audio track");
        System.out.println("Audio Format: " + audioTrack.getFormat());
        
        Codec audioDecoder = JMFUtil.createAudioDecoder(audioTrack.getFormat());
        AudioFormat finalOutputFormat = (AudioFormat)audioDecoder.getSupportedOutputFormats(audioTrack.getFormat())[0];
        System.out.println("Supported Output Formats: " + Arrays.toString(audioDecoder.getSupportedOutputFormats(audioTrack.getFormat())));
        if( finalOutputFormat == null)
            throw new IllegalStateException("Specified output format was rejected.");
        System.out.println("Decoded Audio Format: " + finalOutputFormat);
        
        // extract audio data
        Buffer in = new Buffer();
        Buffer out = new Buffer();
        ArrayList<Short> values = new ArrayList<Short>();

        while(!out.isEOM()) {
            audioTrack.readFrame(in);
            audioDecoder.process(in, out);
            // now examine "out"
            byte[] buffer = (byte[])out.getData();
            //System.out.println("buffer.offset = " + out.getOffset() + "; buffer.length = " + out.getLength());
            convert(buffer, out.getOffset(), out.getLength(), values);
        }
        
        return toShortArray(values);
    }
    
    private static void convert(byte[] data, int offset, int length, ArrayList<Short> targetBuffer) {
        // discard second channel of stereo signal
        for( int i = 0; i < length; i+=4) {
            targetBuffer.add(twoBytesToShort(data[offset+i], data[offset+i+1]));
        }
    }
    
    private static short twoBytesToShort(byte b1, byte b2) {
        int bv1 = (int)b1+128;
        int bv2 = (int)b2+128;
        int combined = (bv2 << 8) + bv1;
        return (short)(combined + Short.MIN_VALUE);
    }
    
    private static short[] toShortArray(List<Short> list) {
        short[] array = new short[list.size()];
        for( int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }
}
