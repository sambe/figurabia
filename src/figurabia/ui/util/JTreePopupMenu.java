/*
 * Copyright (c) 2011 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 27.11.2011
 */
package figurabia.ui.util;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;
import javax.swing.JTree;

public class JTreePopupMenu {

    public JTreePopupMenu(final JTree tree, final JPopupMenu popupMenu) {
        tree.addMouseListener(new MouseAdapter() {
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
                    int selected = tree.getRowForLocation(e.getX(), e.getY());
                    if (selected != -1) {
                        tree.setSelectionRow(selected);
                        popupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
        });

    }

}
