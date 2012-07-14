/*
 * Copyright (c) 2012 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on Jun 30, 2012
 */
package figurabia.io;

import java.awt.Image;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.WeakHashMap;

import figurabia.io.workspace.Workspace;

/**
 * Loads and caches pictures of individual beats of figures.
 * 
 * @author Samuel Berner
 */
public class BeatPictureCache {

    private final Workspace workspace;
    private final String basePath;

    private Map<String, Image> imageCache = new WeakHashMap<String, Image>();

    public BeatPictureCache(Workspace workspace, String basePath) {
        this.workspace = workspace;
        this.basePath = basePath;
    }

    public Image getPicture(String figureId, int bar, int beat) {
        String name = getPictureName(figureId, bar, beat);
        if (!imageCache.containsKey(name)) {
            File file = workspace.fileForReading(basePath + "/" + name);
            Toolkit tk = Toolkit.getDefaultToolkit();
            Image image = tk.createImage(file.toString());
            imageCache.put(name, image);
        }
        return imageCache.get(name);
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
        workspace.delete(basePath + "/" + figureId);
    }
}
