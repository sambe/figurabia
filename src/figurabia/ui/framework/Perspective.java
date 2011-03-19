/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 04.07.2010
 */
package figurabia.ui.framework;

public interface Perspective {

    public void updateOnPerspectiveSwitch(boolean active);

    public String getPerspectiveId();
}
