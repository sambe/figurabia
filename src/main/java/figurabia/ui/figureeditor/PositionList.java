/*
 * Copyright (c) 2009 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 23.07.2009
 */
package figurabia.ui.figureeditor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.ImageObserver;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;

import figurabia.domain.Element;
import figurabia.domain.Figure;
import figurabia.domain.PuertoPosition;
import figurabia.framework.PersistenceProvider;
import figurabia.framework.Workspace;
import figurabia.ui.FigurabiaBlackLookAndFeel;
import figurabia.ui.util.JListPopupMenu;
import figurabia.ui.util.JListSelectionFollower;

@SuppressWarnings("serial")
public class PositionList extends JPanel {

    private PersistenceProvider persistenceProvider;

    private Workspace workspace;

    private JList list;

    private Figure figure;

    private List<Image> positionImages;

    private Set<String> elementNames;

    private JMenuItem deletePosition;

    private JPopupMenu popupMenu;

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

    public PositionList(Workspace workspace_, PersistenceProvider persistenceProvider) {
        this.persistenceProvider = persistenceProvider;
        this.workspace = workspace_;
        list = new JList();
        list.setModel(new DefaultListModel());
        list.setCellRenderer(new PositionListCellRenderer());
        list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        list.setVisibleRowCount(1);
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.setFixedCellHeight(160);
        list.setFixedCellWidth(160);

        JScrollPane scrollPane = new JScrollPane(list, JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        setLayout(new BorderLayout());
        setAutoscrolls(true);
        add(scrollPane, BorderLayout.CENTER);

        setMinimumSize(new Dimension(400, 180));
        setPreferredSize(new Dimension(400, 180));

        elementNames = createElementNames();

        // set up mouse listener for inplace editing of element name
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openElementNameEditor(e.getPoint());
            }
        });

        // update scroll position if the entry is not visible
        new JListSelectionFollower(list, scrollPane);

        // set up popup menu
        popupMenu = new JPopupMenu();
        popupMenu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                deletePosition.setEnabled(!figure.isActive());
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });
        deletePosition = new JMenuItem("Delete Position");
        deletePosition.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteSelectedPosition();
            }
        });
        popupMenu.add(deletePosition);

        new JListPopupMenu(list, popupMenu);

        list.getActionMap().put("deletePosition", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!figure.isActive()) {
                    deleteSelectedPosition();
                }
            }
        });
        list.getInputMap().put(KeyStroke.getKeyStroke("DELETE"), "deletePosition");
    }

    private void deleteSelectedPosition() {
        int[] selectedIndices = list.getSelectedIndices();
        if (JOptionPane.showConfirmDialog(list, "Do you really want to delete the selected positions from the figure?",
                "Delete Positions Warning", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            List<PuertoPosition> positions = figure.getPositions();
            List<Element> elements = figure.getElements();
            List<Long> videoPositions = figure.getVideoPositions();
            List<Integer> barIds = figure.getBarIds();
            for (int i = selectedIndices.length - 1; i >= 0; i--) {
                int selected = selectedIndices[i];
                // delete picture of position
                PuertoPosition p = positions.get(selected);
                int bar = barIds.get(selected);
                String pictureName = workspace.getPictureName(figure.getId(), bar, p.getBeat());
                new File(workspace.getPictureDir() + pictureName).delete();
                positions.remove(selected);
                videoPositions.remove(selected);
                barIds.remove(selected);
                // the last position does not have a corresponding element (because it is the trailing position)
                if (selected != elements.size()) {
                    elements.remove(selected);
                    if (selected != 0) {
                        // reassign the final position to be the new one at index "selected"
                        elements.get(selected - 1).setFinalPosition(positions.get(selected));
                    }
                } else {
                    // only remove an element if there are remaining positions with elements
                    if (selected != 0) {
                        elements.remove(selected - 1);
                    }
                }
            }

            updateList();
        }
    }

    private void openElementNameEditor(Point mouse) {
        Rectangle r = list.getCellBounds(list.getSelectedIndex(), list.getSelectedIndex());
        // last position is the trailing position which has no element to edit its name
        if (r == null || list.getSelectedIndex() == figure.getElements().size())
            return;
        r = new Rectangle(r.x, r.y + 140, r.width, 20);

        if (r.contains(mouse)) {
            // activate popup with editable combo box for entering element name
            final JComboBox elementNameBox = new JComboBox(elementNames.toArray(new String[elementNames.size()]));
            //List<String> elementNames = Arrays.asList(elementNames);
            //final JTextField elementNameBox = new JTextField();
            //AutoCompleteDecorator.decorate(elementNameBox, elementNames, false);
            //elementNameBox.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
            elementNameBox.setMinimumSize(new Dimension(r.width, 20 + 2/*+ 5*/));
            elementNameBox.setPreferredSize(new Dimension(r.width, 20 + 2/*+ 5*/));
            elementNameBox.setEditable(true);
            AutoCompleteDecorator.decorate(elementNameBox);
            final int selectedIndex = list.getSelectedIndex();
            String elementName = figure.getElements().get(selectedIndex).getName();
            if (elementName != null) {
                elementNameBox.setSelectedItem(elementName);
                //elementNameBox.setText(elementName);
                //elementNameBox.setSelectionStart(0);
                //elementNameBox.setSelectionEnd(elementName.length());
            }
            Point screenLocation = new Point(r.x, r.y - 1/*- 3*/);
            SwingUtilities.convertPointToScreen(screenLocation, list);
            final Popup popup = PopupFactory.getSharedInstance().getPopup(list, elementNameBox, screenLocation.x,
                    screenLocation.y);
            final boolean[] acceptChange = new boolean[] { true };
            JTextField textField = (JTextField) elementNameBox.getEditor().getEditorComponent();
            textField.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    // finish edit operation
                    if (acceptChange[0]) {
                        String elementName = (String) elementNameBox.getSelectedItem();//elementNameBox.getText();
                        setElementName(selectedIndex, elementName);
                    }
                    popup.hide();
                }
            });
            textField.getInputMap().put(KeyStroke.getKeyStroke("ESCAPE"), "discard");
            textField.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "accept");
            textField.getActionMap().put("discard", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    acceptChange[0] = false;
                    list.requestFocusInWindow(); // causes focusLost event
                }
            });
            textField.getActionMap().put("accept", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    acceptChange[0] = true;
                    list.requestFocusInWindow(); // causes focusLost event
                }
            });
            popup.show();
            elementNameBox.requestFocusInWindow();
        }
    }

    private void setElementName(int selectedIndex, String name) {
        if ("".equals(name)) {
            figure.getElements().get(selectedIndex).setName(null);
        } else {
            name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
            figure.getElements().get(selectedIndex).setName(name);
            // add the name to the set of element names (duplicates are ignored)
            elementNames.add(name);
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

    private class PositionListCellRenderer extends JPanel implements ListCellRenderer {

        private int index;
        private boolean selected;
        private Color foreground;
        private Color background;

        public PositionListCellRenderer() {
            Dimension d = new Dimension(160, 160);
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
            g.drawImage(image, 0, 0, 160, 120, imageObserver);
            if (selected) {
                g.setColor(new Color(255, 255, 255, 63));
                g.fillRect(0, 0, 160, 120);
            }

            // draw text below
            g.setColor(foreground);
            int barId = figure.getBarIds().get(index);
            //String bar = barId >= 1000 ? Character.toString((char) (barId - 1000 + 'A')) : Integer.toString(barId);
            String bar = barId >= 1000 ? "#" : Integer.toString(barId);
            int beat = figure.getPositions().get(index).getBeat();
            String subtitle = bar + " - " + beat;
            g.drawString(subtitle, 65, 135);
            if (index < figure.getElements().size()) {
                String elementName = figure.getElements().get(index).getName();
                if (elementName != null)
                    g.drawString(elementName, 3, 154);
            }
        }
    }

    public void setFigure(Figure f) {
        figure = f;

        updateList();
    }

    public void updateList() {
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
            int bar = figure.getBarIds().get(i);
            int beat = figure.getPositions().get(i).getBeat();
            positionImages.add(workspace.getPicture(figure.getId(), bar, beat));
        }
        repaint();
    }

    public void updatePicture(int index) {
        int bar = figure.getBarIds().get(index);
        int beat = figure.getPositions().get(index).getBeat();
        workspace.removePictureFromCache(figure.getId(), bar, beat);
        positionImages.set(index, workspace.getPicture(figure.getId(), bar, beat));
        repaint();
    }

    /**
     * @param listener
     * @see javax.swing.JList#addListSelectionListener(javax.swing.event.ListSelectionListener)
     */
    public void addListSelectionListener(ListSelectionListener listener) {
        list.addListSelectionListener(listener);
    }

    /**
     * @param listener
     * @see javax.swing.JList#removeListSelectionListener(javax.swing.event.ListSelectionListener)
     */
    public void removeListSelectionListener(ListSelectionListener listener) {
        list.removeListSelectionListener(listener);
    }

    /**
     * @see javax.swing.JList#getSelectedIndex() Only returns the index if only one has been selected
     */
    public int getSelectedIndex() {
        int min = list.getSelectionModel().getMinSelectionIndex();
        int max = list.getSelectionModel().getMaxSelectionIndex();
        if (max - min == 0) {
            return list.getSelectedIndex();
        }
        return -1;
    }

    /**
     * @param index
     * @see javax.swing.JList#setSelectedIndex(int)
     */
    public void setSelectedIndex(int index) {
        list.setSelectedIndex(index);
    }

    private Set<String> createElementNames() {
        Set<String> names = new TreeSet<String>(Arrays.asList("", "360", "Ambulancia", "Caminata", "Copa",
                "Dile que no", "Doppeldrehung", "Dreifachdrehung", "Encuentro", "Enchufla", "Grundschritt",
                "Inside turn", "Lazo", "Manitos volandos", "Open break", "Outside turn", "Titanic"));

        // collect all element names in use
        for (Figure f : persistenceProvider.getAllFigures()) {
            for (Element e : f.getElements()) {
                if (e != null && e.getName() != null)
                    names.add(e.getName());
            }
        }

        return names;
    }
}
