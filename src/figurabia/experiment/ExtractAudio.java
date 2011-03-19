/*
 * Copyright (c) 2009 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 03.05.2009
 */
package figurabia.experiment;

import java.util.Arrays;

import javax.media.Buffer;
import javax.media.Codec;
import javax.media.Demultiplexer;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.Track;
import javax.media.format.AudioFormat;
import javax.media.protocol.DataSource;

import figurabia.experiment.sound.SoundUtil;
import figurabia.math.Complex;
import figurabia.math.FFT;

@SuppressWarnings("unused")
public class ExtractAudio {

    /**
     * Application Main method.
     * 
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {

        // get required JMF plugins
        MediaLocator ml = new MediaLocator("file:/home/sberner/Desktop/10-21.04.09.flv");
        DataSource ds = Manager.createDataSource(ml);
        Demultiplexer parser = JMFUtil.createParser(ds);
        Track audioTrack = null;
        for (Track t : parser.getTracks()) {
            if (t.getFormat() instanceof AudioFormat) {
                if (audioTrack != null)
                    throw new IllegalStateException("Found more than one audio track.");
                audioTrack = t;
            } else {
                t.setEnabled(false);
            }
        }
        if (audioTrack == null)
            throw new IllegalStateException("Found no Audio track");
        System.out.println("Audio Format: " + audioTrack.getFormat());

        Codec audioDecoder = JMFUtil.createAudioDecoder(audioTrack.getFormat());
        AudioFormat finalOutputFormat = (AudioFormat) audioDecoder.getSupportedOutputFormats(audioTrack.getFormat())[0];//(AudioFormat)audioDecoder.setOutputFormat(audioOutputFormat);
        System.out.println("Supported Output Formats: "
                + Arrays.toString(audioDecoder.getSupportedOutputFormats(audioTrack.getFormat())));
        if (finalOutputFormat == null)
            throw new IllegalStateException("Specified output format was rejected.");
        System.out.println("Decoded Audio Format: " + finalOutputFormat);

        // extract audio data
        Buffer in = new Buffer();
        Buffer out = new Buffer();
        short[][] values = new short[2][576];
        short[][] spectralValues = new short[2][512];
        short[][] spectralTable = new short[200][120];
        int specTablePos = 0;

        short[] smallAudioBuffer = new short[512];
        short[] bigAudioBuffer = new short[500 * 512];
        int bigBufPos = 0;

        while (!out.isEOM()) {
            audioTrack.readFrame(in);
            audioDecoder.process(in, out);
            // now examine "out"
            byte[] buffer = (byte[]) out.getData();
            //System.out.println("buffer.offset = " + out.getOffset() + "; buffer.length = " + out.getLength());
            values = convert(buffer, out.getOffset(), out.getLength(), values);
            //System.out.print("Values: ");
            //for( int i = 0; i < 100; i++) {
            //    System.out.print(values[0][i]);
            //    System.out.print(" ");
            //}
            //System.out.println();
            //System.out.println("Values: " + Arrays.toString(values[0]));
            //System.out.println("Sequence number: " + buffer[0] + " " + buffer[1] + " " + buffer[2] + " " + buffer[3] + " " + buffer[4] + " " + buffer[5] + " " + buffer[6] + " " + buffer[7] + " " + buffer[8] + " " + buffer[9] + " " + buffer[10] + " " + buffer[11] + " " + buffer[12] + " " + buffer[13] + " " + buffer[14] + " " + buffer[15] + " " + buffer[16] + " " + buffer[17] + " " + buffer[18] + " " + buffer[19] + " " + buffer[20] + " " + buffer[21] + " " + buffer[22] + " " + buffer[23] + " ");
            //System.out.println("Buffer length " + buffer.length);

            /*spectralAnalysis(values[0], spectralValues[0]);
            
            //// display panel with data (to visualize)
            //SoundViewer.displayViewer(spectralValues);
            
            copyToSpectralTable(spectralValues[0], spectralTable[specTablePos]);
            specTablePos++;
            
            if(specTablePos == spectralTable.length) {
                specTablePos = 0;
                SpectralTableViewer.displayViewer(spectralTable, 15);
            }*/

            extractFrequencies(values[0], smallAudioBuffer, 40, 80);
            System.arraycopy(smallAudioBuffer, 0, bigAudioBuffer, bigBufPos, smallAudioBuffer.length);
            bigBufPos += smallAudioBuffer.length;
            if (bigBufPos == bigAudioBuffer.length) {
                SoundUtil.playSound(bigAudioBuffer);
                bigBufPos = 0;
            }
        }

        // play sound
        short[] exactSizeBuffer = new short[bigBufPos];
        System.arraycopy(bigAudioBuffer, 0, exactSizeBuffer, 0, exactSizeBuffer.length);
        SoundUtil.playSound(exactSizeBuffer);

        // create big table of spectral data, normalize it, display with colors

        // visualize audio data

        // dispose resources
        audioDecoder.close();
        parser.close();
    }

    private static short[][] convert(byte[] data, int offset, int length, short[][] newData) {
        for (int i = 0; i < newData[0].length; i++) {
            int iOffset = offset + i * 4;
            newData[0][i] = twoBytesToShort(data[iOffset], data[iOffset + 1]);
            newData[1][i] = twoBytesToShort(data[iOffset + 2], data[iOffset + 3]);
        }
        return newData;
    }

    private static short intToShort(int number) {
        return (short) number;
    }

    private static short twoBytesToShort(byte b1, byte b2) {
        int bv1 = (int) b1 + 128;
        int bv2 = (int) b2 + 128;
        int combined = (bv2 << 8) + bv1;
        return intToShort(combined + Short.MIN_VALUE);
    }

    private static void spectralAnalysis(short[] in, short[] out) {
        Complex[] complexIn = new Complex[512];
        for (int i = 0; i < complexIn.length; i++)
            complexIn[i] = new Complex(in[i], 0);
        Complex[] complexOut = FFT.fft(complexIn);
        for (int i = 0; i < complexOut.length; i++) {
            out[i] = (short) (complexOut[i].abs() + Short.MIN_VALUE); // just to make it fit into the chart
        }
    }

    private static void extractFrequencies(short[] in, short[] out, int begin, int end) {
        // fft
        Complex[] complexIn = new Complex[512];
        for (int i = 0; i < complexIn.length; i++)
            complexIn[i] = new Complex(in[i], 0);
        Complex[] complexOut = FFT.fft(complexIn);
        Complex zero = new Complex(0, 0);
        // extract frequency (on both sides, because of the symmetry)
        for (int i = 0; i < begin; i++) {
            complexOut[i] = zero;
        }
        for (int i = end + 1; i < complexOut.length / 2; i++) {
            complexOut[i] = zero;
        }
        for (int i = complexOut.length / 2; i < complexOut.length - end - 1; i++) {
            complexOut[i] = zero;
        }
        for (int i = complexOut.length - begin; i < complexOut.length; i++) {
            complexOut[i] = zero;
        }

        // ifft
        Complex[] complexBack = FFT.ifft(complexOut);
        for (int i = 0; i < complexBack.length; i++) {
            out[i] = (short) complexBack[i].re();
        }
    }

    private static void copyToSpectralTable(short[] spectralValues, short[] spectralTable) {
        System.arraycopy(spectralValues, 0, spectralTable, 0, spectralTable.length);
    }
}
