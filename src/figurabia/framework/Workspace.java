/*
 * Copyright (c) 2009 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 20.07.2009
 */
package figurabia.framework;

import java.awt.Image;
import java.io.File;
import java.io.IOException;

public interface Workspace {

    File getWorkspaceBaseDir();

    File getVideoDir();

    File getPictureDir();

    Image getPicture(int figureId, int bar, int beat);

    String getPictureName(int figureId, int bar, int beat);

    void removePictureFromCache(int figureId, int bar, int beat);

    void deleteAllPictures(int figureId) throws IOException;

    File getDatabaseDir();
}
