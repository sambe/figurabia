/*
 * Copyright (c) 2009 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 01.06.2009
 */
package figurabia.experiment;


public class ExtractFrequencies {

    /*public static void main(String[] args) throws Exception {
        File mediaFile = new File("/home/sberner/Desktop/10-21.04.09.flv");
        int segmentSize = 64;
        int evalLength = 200000;
        short[] values = JMFUtil.getAudioTrack(mediaFile);
        short[][] offsetValues = new short[segmentSize][];
        for( int i = 0; i < offsetValues.length; i++) {
            offsetValues[i] = Arrays.copyOf(values, evalLength);
        }
        for( int i = 0; i < offsetValues.length; i++) {
            System.out.println("Extracting with offset " + (i+1));
            extractFrequencies(offsetValues[i], i, segmentSize, 19, 26);
        }
        
        short[] averagedValues = new short[evalLength];
        // calculate average of individual calculations
        for( int i = 0; i < averagedValues.length; i++) {
            int sum = 0;
            for(int j = 0; j < segmentSize; j++) {
                sum += offsetValues[j][i];
            }
            averagedValues[i] = (short)(sum * 10 / segmentSize);
        }
        
        System.out.println("Starting sound output...");
        SoundUtil.playSound(averagedValues);
        System.out.println("averagedValues = " + Arrays.toString(averagedValues));
        SoundUtil.playSound(averagedValues);
        SoundUtil.playSound(averagedValues);
        SoundUtil.playSound(averagedValues);
        SoundUtil.playSound(averagedValues);
        SoundUtil.playSound(averagedValues);
        SoundUtil.playSound(averagedValues);
        
        SoundViewer.displayViewer(averagedValues);
    }
    
    private static void extractFrequencies(short[] values, int segmentOffset, int segmentSize, int begin, int end) {
        // check input parameters
        if( begin > end)
            throw new IllegalArgumentException("parameter begin must be smaller or equal to parameter end. Was " + begin + " and " + end);
        if( end > segmentSize/2)
            throw new IllegalArgumentException("parameter end must be no higher than half of parameter segmentSize");
        int num = segmentSize;
        while(num > 2) {
            if( num % 2 != 0)
                throw new IllegalArgumentException("parameter segmentSize must be a power of two");
            num /= 2;
        }
        // segment track
        for( int i = segmentOffset; i+segmentSize < values.length; i+=segmentSize) {
            extractFrequenciesInSegment(values, i, segmentSize, begin, end);
        }
    }
    
    private static void extractFrequenciesInSegment(short[] values, int offset, int length, int begin, int end) {
        // fft
        Complex[] complexIn = new Complex[length];
        for( int i = 0; i < complexIn.length; i++)
            complexIn[i] = new Complex(values[offset+i], 0);
        Complex[] complexOut = FFT.fft(complexIn);
        Complex zero = new Complex(0,0);
        // extract frequency (on both sides, because of the symmetry)
        for( int i = 0; i < begin; i++) {
            complexOut[i] = zero;
        }
        for( int i = end+1; i < complexOut.length/2; i++) {
            complexOut[i] = zero;
        }
        for( int i = complexOut.length/2; i < complexOut.length-end-1;i++) {
            complexOut[i] = zero;
        }
        for( int i = complexOut.length-begin; i < complexOut.length; i++) {
            complexOut[i] = zero;
        }
        
        // ifft
        Complex[] complexBack = FFT.ifft(complexOut);
        for( int i = 0; i < complexBack.length; i++) {
            values[offset+i] = (short)complexBack[i].re();
        }
    }*/
}
