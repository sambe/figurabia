/*
 * Copyright (c) 2009 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 20.07.2009
 */
package figurabia.framework.simpleimpl;

import java.awt.Image;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.commons.io.FileUtils;

import figurabia.framework.Workspace;

public class SimpleWorkspace implements Workspace {

    private File baseDir;

    private Map<String, Image> imageCache = new WeakHashMap<String, Image>();

    public SimpleWorkspace(File baseDir) {
        this.baseDir = baseDir;
        if (!baseDir.exists())
            throw new IllegalArgumentException("the given base directory does not exist");
    }

    @Override
    public File getPictureDir() {
        return new File(baseDir + File.separator + "pics");
    }

    @Override
    public File getVideoDir() {
        return new File(baseDir + File.separator + "vids");
    }

    @Override
    public File getWorkspaceBaseDir() {
        return baseDir;
    }

    @Override
    public File getDatabaseDir() {
        return new File(baseDir + File.separator + "db");
    }

    @Override
    public Image getPicture(int figureId, int bar, int beat) {
        String name = getPictureName(figureId, bar, beat);
        if (!imageCache.containsKey(name)) {
            Toolkit tk = Toolkit.getDefaultToolkit();
            Image image = tk.createImage(getPictureDir() + name);
            imageCache.put(name, image);
        }
        return imageCache.get(name);
    }

    @Override
    public void removePictureFromCache(int figureId, int bar, int beat) {
        String name = getPictureName(figureId, bar, beat);
        imageCache.remove(name);
    }

    @Override
    public String getPictureName(int figureId, int bar, int beat) {
        return File.separator + figureId + File.separator + String.format("%03d-%d.jpg", bar, beat);
    }

    @Override
    public void deleteAllPictures(int figureId) throws IOException {
        FileUtils.deleteDirectory(new File(getPictureDir() + File.separator + figureId));
    }
}
