/*
 * Copyright (c) 2013 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on Mar 17, 2013
 */
package figurabia.io;

import java.awt.Graphics;
import java.awt.Image;
import java.util.ArrayList;
import java.util.List;

/**
 * This class allows to return an image immediately before it is actually loaded, later asynchronously (but on the Swing
 * Event Dispatcher Thread), the image will be updated.
 * 
 * @author Samuel Berner
 */
public class ProxyImage {

    public interface ImageUpdateListener {
        void imageUpdated(ProxyImage img);
    }

    private List<ImageUpdateListener> listeners = new ArrayList<ImageUpdateListener>();
    private Image img;
    public final String name;

    public ProxyImage(String name) {
        this.name = name;
    }

    public Image getImage() {
        return img;
    }

    //public final int width, height;

    /*public ProxyImage(int width, int height) {
        this.width = width;
        this.height = height;
    }*/

    public void update(Image img) {
        this.img = img;
        notifyListeners();
    }

    protected void notifyListeners() {
        for (ImageUpdateListener l : new ArrayList<ImageUpdateListener>(listeners)) {
            try {
                l.imageUpdated(ProxyImage.this);
            } catch (RuntimeException e) {
                System.err.println("Exception occured when notifying ImageUpdateListeners:");
                e.printStackTrace();
            }
        }
    }

    public void addImageUpdateListener(ImageUpdateListener l) {
        listeners.add(l);
    }

    public void removeImageUpdateListener(ImageUpdateListener l) {
        listeners.add(l);
    }

    public void draw(Graphics g, int x, int y, int width, int height) {
        if (img != null)
            g.drawImage(img, x, y, width, height, null);
    }

    public void draw(Graphics g, int dx, int dy, int dw, int dh, int sx, int sy, int sw, int sh) {
        if (img != null)
            g.drawImage(img, dx, dy, dw, dh, sx, sy, sw, sh, null);
    }

    public void once(final ImageUpdateListener oneTimeListener) {
        if (img != null)
            oneTimeListener.imageUpdated(this);
        else {
            addImageUpdateListener(new ImageUpdateListener() {
                @Override
                public void imageUpdated(ProxyImage img) {
                    removeImageUpdateListener(this);
                    oneTimeListener.imageUpdated(ProxyImage.this);
                }
            });
        }

    }
}
