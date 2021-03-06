/*
 * Copyright (c) 2012 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on Jun 30, 2012
 */
package figurabia.io;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutionException;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.SwingWorker;

import org.apache.commons.io.IOUtils;

import figurabia.io.ProxyImage.ImageUpdateListener;
import figurabia.io.workspace.Workspace;

/**
 * Loads and caches pictures of individual beats of figures.
 * 
 * @author Samuel Berner
 */
public class BeatPictureCache {

    private final Workspace workspace;
    private final String basePath;

    private Map<String, ProxyImage> imageCache = new WeakHashMap<String, ProxyImage>();

    public BeatPictureCache(Workspace workspace, String basePath) {
        this.workspace = workspace;
        this.basePath = basePath;
    }

    public ProxyImage getPicture(String figureId, int bar, int beat) {
        String name = getPictureName(figureId, bar, beat);
        return getPictureByName(name);
    }

    private ProxyImage getPictureByName(final String name) {
        if (!imageCache.containsKey(name)) {
            imageCache.put(name, new ProxyImage(name));
            loadPicture(name);
        }
        return imageCache.get(name);
    }

    private void reloadPicture(final String name) {
        if (imageCache.containsKey(name))
            loadPicture(name);
    }

    private void loadPicture(final String name) {
        SwingWorker<Image, Void> worker = new SwingWorker<Image, Void>() {
            @Override
            protected Image doInBackground() throws Exception {
                File file = workspace.fileForReading(basePath + "/" + name);
                Image image = ImageIO.read(file);
                return image;
            }

            /**
             * @see javax.swing.SwingWorker#done()
             */
            @Override
            protected void done() {
                if (getState() == StateValue.DONE) {
                    try {
                        Image image = get();
                        ProxyImage proxy = imageCache.get(name);
                        proxy.update(image);
                    } catch (ExecutionException e) {
                        System.err.println("Exception while loading image " + name + " in background.");
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        // ignore
                    }
                }
            }

        };
        worker.execute();
    }

    public ProxyImage getScaledPicture(String name, final int width, final int height) {
        String scaledName = getScaledPictureName(name, width, height);
        if (!imageCache.containsKey(scaledName)) {
            final ProxyImage scaledProxyImage = new ProxyImage(scaledName);
            imageCache.put(scaledName, scaledProxyImage);
            ProxyImage proxyImage = getPictureByName(name);
            proxyImage.foreach(new ImageUpdateListener() {
                @Override
                public void imageUpdated(final ProxyImage img) {
                    SwingWorker<Image, Void> worker = new SwingWorker<Image, Void>() {
                        @Override
                        protected Image doInBackground() throws Exception {
                            Image scaledImage = new BufferedImage(width, height, ColorSpace.TYPE_RGB);
                            Graphics g = scaledImage.getGraphics();
                            img.draw(g, 0, 0, width, height);
                            return scaledImage;
                        }

                        @Override
                        protected void done() {
                            if (getState() == StateValue.DONE) {
                                try {
                                    Image image = get();
                                    scaledProxyImage.update(image);
                                } catch (ExecutionException e) {
                                    System.err.println("Exception while loading image " + scaledProxyImage.name
                                            + " in background.");
                                    e.printStackTrace();
                                } catch (InterruptedException e) {
                                    // ignore
                                }
                            }
                        }
                    };
                    worker.execute();
                }
            });
        }
        return imageCache.get(scaledName);
    }

    public void storePicture(String figureId, int bar, int beat, Image picture) {
        String name = getPictureName(figureId, bar, beat);

        // store
        writePicture(name, picture);

        // reload (causes update if already displayed somewhere, otherwise not loaded)
        reloadPicture(name);
    }

    private void writePicture(String name, Image picture) {
        String picturePath = basePath + name;
        OutputStream os = null;
        boolean existed = workspace.exists(picturePath);
        try {
            os = workspace.write(picturePath);
            try {
                BufferedImage outImage = new BufferedImage(picture.getWidth(null), picture.getHeight(null),
                        BufferedImage.TYPE_INT_RGB);
                Graphics og = outImage.getGraphics();
                og.drawImage(picture, 0, 0, picture.getWidth(null), picture.getHeight(null), null);
                Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
                ImageWriter writer = writers.next();

                // Once an ImageWriter has been obtained, its destination must be set to an ImageOutputStream:
                ImageOutputStream ios = ImageIO.createImageOutputStream(os);
                writer.setOutput(ios);

                // Finally, the image may be written to the output stream:
                writer.write(outImage);
                ios.close();
            } catch (IOException e) {
                System.out.println("ERROR: An IO problem occured during saving of the picture");
                e.printStackTrace();
            }
        } finally {
            IOUtils.closeQuietly(os);
        }
        workspace.finishedWriting(picturePath, !existed);

    }

    public void removePictureFromCache(String figureId, int bar, int beat) {
        String name = getPictureName(figureId, bar, beat);
        imageCache.remove(name);
    }

    public String getPictureName(String figureId, int bar, int beat) {
        return "/" + figureId + "/" + String.format("%03d-%d.jpg", bar, beat);
    }

    public String getPicturePath(String figureId, int bar, int beat) {
        return basePath + getPictureName(figureId, bar, beat);
    }

    public void deleteAllPictures(String figureId) throws IOException {
        String path = basePath + "/" + figureId;
        if (workspace.exists(path))
            workspace.deletePath(path);
    }

    private String getScaledPictureName(String baseName, int width, int height) {
        return baseName + ":" + width + ":" + height;
    }
}
