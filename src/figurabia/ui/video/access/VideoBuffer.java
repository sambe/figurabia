/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 24.10.2010
 */
package figurabia.ui.video.access;

import java.awt.Image;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;

import javax.media.Buffer;
import javax.media.format.RGBFormat;

/**
 * Represents the memory unit that stores one image of one frame of the media file. This class can be passed around
 * between different actors.
 */
public class VideoBuffer {

    private Buffer buffer;

    public VideoBuffer() {
        buffer = new Buffer();
    }

    public Image getImage() {
        return bufferToImage(buffer);
    }

    public Buffer getBuffer() {
        return buffer;
    }

    private static BufferedImage bufferToImage(Buffer buffer) {
        RGBFormat format = (RGBFormat) buffer.getFormat();
        int rMask, gMask, bMask;
        Object data = buffer.getData();
        DirectColorModel dcm;

        rMask = format.getRedMask();
        gMask = format.getGreenMask();
        bMask = format.getBlueMask();
        int[] masks = new int[3];
        masks[0] = rMask;
        masks[1] = gMask;
        masks[2] = bMask;

        DataBuffer db = new DataBufferInt((int[]) data, format.getLineStride() * format.getSize().height);

        SampleModel sm = new SinglePixelPackedSampleModel(DataBuffer.TYPE_INT, format.getLineStride(),
                format.getSize().height, masks);
        WritableRaster wr = Raster.createWritableRaster(sm, db, new Point(0, 0));

        dcm = new DirectColorModel(24, rMask, gMask, bMask);
        return new BufferedImage(dcm, wr, true, null);
    }
}
