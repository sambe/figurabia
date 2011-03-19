/*
 * Copyright (c) 2009 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 31.05.2009
 */
package figurabia.experiment.sound;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class SoundUtil {
    
    public static void playSound(short[] values) throws LineUnavailableException {
        AudioFormat audioFormat = new AudioFormat(22050,16,1,true,false); //sample rate, bits per sample, 
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        SourceDataLine line = (SourceDataLine)AudioSystem.getLine(info);
        line.open(audioFormat);
        line.start();
        
        byte[] binary = new byte[values.length*2];
        for(int i = 0; i < values.length; i++) {
            binary[i*2] = (byte)(values[i] & 0xff);
            binary[i*2+1] = (byte)((values[i] >> 8) & 0xff);
        }
        line.write(binary, 0, binary.length);
        
        line.drain();
        line.close();
    }

}
