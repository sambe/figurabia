/*
 * Copyright (c) 2011 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 18.08.2011
 */
package figurabia.ui.video.experiment;

public class MediaInputStreamSoundTest {

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        //experiment1();
        //experiment2();
    }

    /*private static void experiment1() throws Exception {
        int n = 40;

        FobsMediaInputStream mis = new FobsMediaInputStream(new File("/home/sberner/Desktop/10-21.04.09.flv"));
        MediaFrame frame = mis.createFrameBuffer();
        AudioFormat audioFormat = mis.getAudioFormat();
        VideoFormat videoFormat = mis.getVideoFormat();
        double frameRate = videoFormat.getFrameRate();
        int audioBufferSize = frame.audio.getAudioData().length;
        byte[] sound = new byte[n * audioBufferSize];
        System.err.println("Total buffer size: " + sound.length);
        int insertPos = 0;

        // 1) get a long array of sound
        mis.readFrame(frame);
        mis.setPosition(3);
        mis.readFrame(frame);
        mis.setPosition(5);
        mis.readFrame(frame);
        mis.setPosition(3);
        mis.readFrame(frame);

        mis.setPosition(0);
        for (int i = 0; i < n; i++) {
            mis.readFrame(frame);
            int length = frame.audio.getAudioData().length;
            System.arraycopy(frame.audio.getAudioData(), 0, sound, insertPos, length);
            insertPos += length;
        }
        System.out.println("sound = " + Arrays.toString(Arrays.copyOfRange(sound, 0, 200)));

        // 2) go to an arbitrary position, retrieve piece of sound and locate it
        mis.readFrame(frame);
        mis.setPosition(3);
        mis.readFrame(frame);
        mis.setPosition(5);
        mis.readFrame(frame);
        mis.setPosition(3);

        long seqNum = 0;
        double seconds = seqNum / frameRate;
        mis.setPosition(seconds);
        mis.readFrame(frame);
        byte[] frameSound = (byte[]) frame.audio.getAudioData();
        System.out.println("array: " + Arrays.toString(frameSound));
        int maxMatch = 0;
        for (int i = 0; i < sound.length; i++) {

            int matchLength = 0;
            int matchJ = -1;
            int max = Math.min(frameSound.length, sound.length - i);
            for (int j = 0; j < max; j++) {
                if (sound[i + j] != frameSound[j]) {
                    matchLength = 0;
                    matchJ = j + 1;
                } else {
                    matchLength++;
                }
                if (matchLength > maxMatch) {
                    maxMatch = matchLength;
                    System.err.println("*** found new max match length at byte position " + (i + matchJ) + " with "
                            + matchLength
                            + " bytes matching");
                }
            }
        }

        mis.close();
    }

    private static void experiment2() throws Exception {
        int n = 1;
        int m = 40;

        FobsMediaInputStream mis = new FobsMediaInputStream(new File("/home/sberner/Desktop/10-21.04.09.flv"));
        MediaFrame frame = mis.createFrameBuffer();
        AudioFormat audioFormat = mis.getAudioFormat();
        VideoFormat videoFormat = mis.getVideoFormat();
        double frameRate = videoFormat.getFrameRate();
        int audioBufferSize = frame.audio.getAudioData().length;
        byte[][] sound = new byte[m][n * audioBufferSize];
        System.err.println("Total buffer size: " + sound[0].length);

        // get a long array of sound
        for (int k = 0; k < m; k++) {
            int insertPos = 0;
            //mis.readFrame(frame);
            //mis.setPosition(Math.random() * 3);
            //mis.readFrame(frame);
            //mis.setPosition(Math.random() * 5);
            //mis.readFrame(frame);
            mis.setPosition(Math.random() * 3);
            mis.readFrame(frame);

            double actualPos = mis.setPosition(5.00);
            System.out.println("DEBUG actual pos = " + actualPos);
            for (int i = 0; i < n; i++) {
                mis.readFrame(frame);
                int length = frame.audio.getAudioData().length;
                System.arraycopy(frame.audio.getAudioData().length, 0, sound[k], insertPos, length);
                insertPos += length;
            }
            System.out.println("sound = " + Arrays.toString(Arrays.copyOfRange(sound[k], 0, 200)));
        }

        // print part of data
        System.out.println();
        for (int i = 0; i < 40; i++) {
            for (int j = 0; j < 40; j++) {
                System.out.print(String.format("%5d", sound[i][j]));
            }
            System.out.println();
        }

        short[][] shortValues = new short[40][];
        for (int i = 0; i < 40; i++) {
            shortValues[i] = JMFUtil.convertByteToShort(sound[i]);
        }

        // print part of data
        System.out.println();
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                System.out.print(String.format("%7d", shortValues[i][j]));
            }
            System.out.println();
        }

        SoundViewer.displayViewer(shortValues);
    }*/
}
