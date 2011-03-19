/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 05.04.2010
 */
package figurabia.ui.util;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;

import javax.swing.JList;
import javax.swing.JPopupMenu;

public class JListPopupMenu {

    public JListPopupMenu(final JList list, final JPopupMenu popupMenu) {
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowPopupMenu(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowPopupMenu(e);
            }

            private void maybeShowPopupMenu(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    // update selection first
                    int selected = list.locationToIndex(e.getPoint());
                    if (selected != -1) {
                        if (Arrays.binarySearch(list.getSelectedIndices(), selected) < 0) {
                            list.setSelectedIndex(selected);
                        }
                        popupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
        });

    }
}
