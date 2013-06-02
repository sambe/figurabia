/*
 * Copyright (c) 2009 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 19.10.2009
 */
package figurabia.ui.figureexplorer;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import figurabia.domain.PuertoOffset;
import figurabia.domain.PuertoPosition;
import figurabia.ui.framework.PositionChangeListener;
import figurabia.ui.positionviewer.PositionDialogEditor;

@SuppressWarnings("serial")
public class PositionChooser extends JPanel {

    private PositionDialogEditor editor;

    private List<PositionChangeListener> positionListeners = new ArrayList<PositionChangeListener>();

    public PositionChooser() {
        editor = new PositionDialogEditor(false);
        PuertoOffset baseOffset = PuertoOffset.getInitialOffset();
        editor.setBaseOffset(baseOffset);

        setLayout(new MigLayout("ins 0"));
        add(editor);

        // propagate position listener events
        editor.addPositionChangeListener(new PositionChangeListener() {
            @Override
            public void positionActive(PuertoPosition p, PuertoOffset offset, PuertoOffset offsetChange) {
                updatePositionListeners(p, offset, offsetChange);
            }
        });
    }

    public void setPosition(PuertoPosition p, PuertoOffset offset) {
        //System.out.println("DEBUG: offset: line = " + offset.getAbsPosLineDir() + " side = "
        //        + offset.getAbsPosSideDir());
        editor.startCollectingUpdates();
        try {
            editor.setPosition(p);
            editor.setOffset(offset);
        } finally {
            editor.finishCollectingUpdates();
        }
    }

    public void addPositionListener(PositionChangeListener l) {
        positionListeners.add(l);
    }

    public void removePositionListener(PositionChangeListener l) {
        positionListeners.remove(l);
    }

    protected void updatePositionListeners(PuertoPosition p, PuertoOffset offset, PuertoOffset offsetChange) {
        for (PositionChangeListener l : positionListeners) {
            try {
                l.positionActive(p, offset, offsetChange);
            } catch (RuntimeException e) {
                // catch exceptions here to avoid unnecessary effects
                System.err.println("Exception from a PositionListener. Position: " + p);
                e.printStackTrace();
            }
        }
    }
}
