/*
 * Copyright (c) 2009 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 19.10.2009
 */
package figurabia.ui.figureexplorer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.ImageObserver;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.miginfocom.swing.MigLayout;
import figurabia.domain.Figure;
import figurabia.framework.Workspace;
import figurabia.ui.FigurabiaBlackLookAndFeel;
import figurabia.ui.util.JListSelectionFollower;

@SuppressWarnings("serial")
public class FigurePositionsView extends JPanel {

    private final static int CELL_HEIGHT = 60;
    private final static int CELL_WIDTH = 240;

    private Workspace workspace;

    private JList list;
    private JScrollPane scrollPane;
    private List<Image> positionImages = Collections.emptyList();

    private Figure figure;
    private int active = -1;

    private boolean inSetter = false;
    private int lastSelectedNotified = -1;
    private List<ActionListener> actionListeners = new ArrayList<ActionListener>();

    private static final Color BACKGROUND_COLOR;
    private static final Color FOREGROUND_COLOR;
    static {
        if (UIManager.getLookAndFeel() instanceof FigurabiaBlackLookAndFeel) {
            BACKGROUND_COLOR = new Color(31, 31, 31);
            FOREGROUND_COLOR = Color.WHITE;
        } else {
            BACKGROUND_COLOR = Color.WHITE;
            FOREGROUND_COLOR = Color.DARK_GRAY;
        }
    }

    private ImageObserver imageObserver = new ImageObserver() {
        @Override
        public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) {
            if ((infoflags & ALLBITS) != 0) {
                repaint();
                return false;
            }
            return true;
        }
    };

    public FigurePositionsView(Workspace workspace) {
        this.workspace = workspace;
        list = new JList();
        list.setModel(new DefaultListModel());
        list.setAutoscrolls(true);
        list.setCellRenderer(new FigurePositionsViewCellRenderer());
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFixedCellHeight(CELL_HEIGHT);
        list.setFixedCellWidth(CELL_WIDTH);
        scrollPane = new JScrollPane(list, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        setLayout(new MigLayout("ins 0", "[fill]", "[fill]"));
        add(scrollPane, "push");

        setMinimumSize(new Dimension(CELL_WIDTH, 6 * CELL_HEIGHT));
        setPreferredSize(new Dimension(CELL_WIDTH, 6 * CELL_HEIGHT));

        list.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                int selected = list.getSelectedIndex();
                // only if there is a valid selection and if it has changed
                if (selected != -1 && selected != lastSelectedNotified) {
                    lastSelectedNotified = selected;
                    // only notify if the change did not come from the setter method
                    if (!inSetter) {
                        notifyActionListeners();
                    }
                }
            }
        });

        // update scroll position if the entry is not visible
        new JListSelectionFollower(list, scrollPane);
    }

    public void setPosition(Figure f, int i) {
        if (inSetter)
            return;

        boolean figureChanged = f != figure;
        figure = f;
        active = i;

        inSetter = true;
        if (figureChanged)
            updateFigure();
        if (active != -1)
            list.setSelectedIndex(active);
        else
            list.clearSelection();
        repaint();
        inSetter = false;
    }

    private void updateFigure() {
        list.clearSelection();

        // fill list with index values
        int n = figure.getVideoPositions().size();
        DefaultListModel model = (DefaultListModel) list.getModel();
        model.removeAllElements();
        for (int i = 0; i < n; i++) {
            model.addElement(i);
        }

        // load pictures
        positionImages = new ArrayList<Image>();
        for (int i = 0; i < n; i++) {
            positionImages.add(workspace.getPicture(figure.getId(), i / 2 + 1, (i % 2) * 4 + 1));
        }

        System.out.println("DEBUG: updated figure");
    }

    private class FigurePositionsViewCellRenderer extends JComponent implements ListCellRenderer {

        private int index;
        private boolean selected;
        private Color foreground;
        private Color background;

        public FigurePositionsViewCellRenderer() {
            Dimension d = new Dimension(CELL_WIDTH, CELL_HEIGHT);
            setMinimumSize(d);
            setPreferredSize(d);
            setSize(d);
            setOpaque(true); // might accelerate rendering
        }

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                boolean cellHasFocus) {
            // set picture (and related data)
            this.index = index;

            // set colors
            if (isSelected) {
                // just an idea, should not be in a concrete implementation
                //SubstanceColorScheme scheme = ((SubstanceListUI) list.getUI()).getHighlightColorScheme(ComponentState.SELECTED);
                //background = scheme.getSelectionBackgroundColor();
                //foreground = scheme.getSelectionForegroundColor();
                background = FOREGROUND_COLOR;
                foreground = BACKGROUND_COLOR;
            } else {
                background = BACKGROUND_COLOR;
                foreground = FOREGROUND_COLOR;
            }
            selected = isSelected;

            return this;
        }

        @Override
        public void paint(Graphics g) {
            g.setColor(background);
            g.fillRect(0, 0, getWidth(), getHeight());

            // draw picture
            Image image = positionImages.get(index);
            g.drawImage(image, 0, 0, 80, 60, imageObserver);
            if (selected) {
                g.setColor(new Color(255, 255, 255, 63));
                g.fillRect(0, 0, 80, 60);
            }

            // draw text below
            g.setColor(foreground);
            String barDescription = (index / 2 + 1) + " - " + ((index % 2) * 4 + 1);
            g.drawString(barDescription, 83, 15);
            if (index < figure.getElements().size()) {
                String elementName = figure.getElements().get(index).getName();
                if (elementName != null)
                    g.drawString(elementName, 83, 34);
            }
        }
    }

    /**
     * @return
     * @see javax.swing.JList#getSelectedIndex()
     */
    public int getSelectedIndex() {
        return list.getSelectedIndex();
    }

    public void addActionListener(ActionListener listener) {
        actionListeners.add(listener);
    }

    public void removeActionListener(ActionListener listener) {
        actionListeners.remove(listener);
    }

    protected void notifyActionListeners() {
        for (ActionListener l : actionListeners) {
            try {
                l.actionPerformed(new ActionEvent(this, ActionEvent.RESERVED_ID_MAX + 1, "selected"));
            } catch (RuntimeException e) {
                // catch exceptions here to avoid unnecessary effects
                System.err.println("Exception from an ActionListener. Figure: " + figure + " position: " + active);
                e.printStackTrace();
            }
        }

    }
}
