/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 02.04.2010
 */
package figurabia.ui.util;

import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class JListSelectionFollower {

    private JList list;
    private JScrollPane scrollPane;

    public JListSelectionFollower(JList list_, JScrollPane scrollPane) {
        this.list = list_;
        this.scrollPane = scrollPane;

        list.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                int selected = list.getSelectedIndex();
                if (selected != -1) {
                    updateScrollPosition(selected);
                }
            }
        });
    }

    private void updateScrollPosition(int selected) {
        Rectangle viewRect = scrollPane.getViewport().getViewRect();
        Point loc = list.indexToLocation(selected);
        if (list.getLayoutOrientation() == JList.HORIZONTAL_WRAP) {
            int cellWidth = list.getFixedCellWidth();
            if (loc.x + cellWidth > viewRect.x + viewRect.width) {
                // scroll right so that item is visible as last item
                scrollPane.getHorizontalScrollBar().setValue(loc.x + cellWidth - viewRect.width);
            } else if (loc.x < viewRect.x) {
                // scroll left so that item is visible as first item
                scrollPane.getHorizontalScrollBar().setValue(loc.x);
            }
        } else {
            int cellHeight = list.getFixedCellHeight();
            if (loc.y + cellHeight > viewRect.y + viewRect.height) {
                // scroll down so that item is visible as last item
                scrollPane.getVerticalScrollBar().setValue(loc.y + cellHeight - viewRect.height);
            } else if (loc.y < viewRect.y) {
                // scroll up so that item is visible as first item
                scrollPane.getVerticalScrollBar().setValue(loc.y);
            }
        }
    }
}
