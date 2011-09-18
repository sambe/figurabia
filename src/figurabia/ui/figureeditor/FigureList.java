/*
 * Copyright (c) 2009 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 23.07.2009
 */
package figurabia.ui.figureeditor;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.DefaultListModel;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.apache.commons.io.FileUtils;

import figurabia.domain.Figure;
import figurabia.framework.FigureListener;
import figurabia.framework.PersistenceProvider;
import figurabia.framework.Workspace;
import figurabia.service.FigureCreationService;
import figurabia.ui.util.JListPopupMenu;

@SuppressWarnings("serial")
public class FigureList extends JPanel {

    private Workspace workspace;

    private PersistenceProvider persistenceProvider;

    private JList list;

    private JPopupMenu popupMenu;

    public FigureList(Workspace workspace_, PersistenceProvider persistenceProvider) {
        this.workspace = workspace_;
        this.persistenceProvider = persistenceProvider;
        list = new JList();
        list.setModel(new DefaultListModel());
        updateListFromPersistence();
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setLayout(new BorderLayout());
        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);

        setMinimumSize(new Dimension(150, 300));
        setPreferredSize(new Dimension(150, 300));

        // set up figure listener
        this.persistenceProvider.addFigureChangeListener(new FigureListener() {
            @Override
            public void update(ChangeType type, Figure figure) {
                // no matter what change, the list needs to be updated
                updateListFromPersistence();
            }
        });

        // set up popup menu 
        popupMenu = new JPopupMenu();
        final JCheckBoxMenuItem activeFigure = new JCheckBoxMenuItem("Active");
        activeFigure.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                setFigureActive(activeFigure.getState());
            }
        });
        popupMenu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                Figure f = (Figure) list.getSelectedValue();
                activeFigure.setSelected(f.isActive());
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });
        JMenuItem renameFigure = new JMenuItem("Rename Figure");
        renameFigure.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                renameFigure(e);
            }
        });
        JMenuItem deleteFigure = new JMenuItem("Delete Figure");
        deleteFigure.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteFigure(e);
            }
        });
        JMenuItem cloneFigure = new JMenuItem("Clone Figure");
        cloneFigure.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cloneFigure(e);
            }
        });
        popupMenu.add(activeFigure);
        popupMenu.add(renameFigure);
        popupMenu.add(deleteFigure);
        popupMenu.add(cloneFigure);
        // only required for standard gtk look and feel
        //popupMenu.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        new JListPopupMenu(list, popupMenu); // connects the popup menu to the list
    }

    private void setFigureActive(boolean newState) {
        try {
            Figure f = (Figure) list.getSelectedValue();
            if (newState) {
                new FigureCreationService(workspace, persistenceProvider).validateAndFinalizeFigure(f);
            }
            f.setActive(newState);
            FigureList.this.persistenceProvider.updateFigure(f);
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, e.getLocalizedMessage());
        } catch (RuntimeException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, e.getMessage());
        }
    }

    private void renameFigure(ActionEvent e) {
        Figure f = (Figure) list.getSelectedValue();
        String newName = JOptionPane.showInputDialog((Component) e.getSource(), "Please enter the new name:",
                f.getName());
        if (newName != null && !newName.equals("")) {
            f.setName(newName);
            FigureList.this.persistenceProvider.updateFigure(f);
        }
    }

    private void deleteFigure(ActionEvent e) {
        Figure f = (Figure) list.getSelectedValue();
        if (JOptionPane.showConfirmDialog((Component) e.getSource(), "Are you sure you want to delete the figure '" + f
                + "'?", "Safety Warning", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            FigureList.this.persistenceProvider.deleteFigure(f);
            // TODO maybe delete video too
            try {
                FigureList.this.workspace.deleteAllPictures(f.getId());
            } catch (IOException e1) {
                e1.printStackTrace();
                JOptionPane.showMessageDialog((Component) e.getSource(),
                        "IO Error occured while deleting pictures of the deleted figure: " + e1.getLocalizedMessage());
            }
        }
    }

    private void cloneFigure(ActionEvent event) {
        Figure f = (Figure) list.getSelectedValue();
        String newName = JOptionPane.showInputDialog((Component) event.getSource(),
                "Please enter a name for the cloned figure:", f.getName());
        if (newName != null && !newName.equals("")) {
            try {
                Figure currentFigure = (Figure) list.getSelectedValue();
                Figure clone = persistenceProvider.cloneFigure(currentFigure);
                clone.setName(newName);
                File from = new File(workspace.getPictureDir() + File.separator + currentFigure.getId());
                File to = new File(workspace.getPictureDir() + File.separator + clone.getId());
                FileUtils.copyDirectory(from, to);
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog((Component) event.getSource(),
                        "IO Error occured while copying pictures of the cloned figure: " + e.getLocalizedMessage());
            }
        }
    }

    public void updateListFromPersistence() {
        Figure selected = (Figure) list.getSelectedValue();
        int selectedIndex = list.getSelectedIndex();
        list.clearSelection();

        DefaultListModel model = (DefaultListModel) list.getModel();
        model.removeAllElements();
        Collection<Figure> figures = persistenceProvider.getAllFigures();
        Iterator<Figure> fi = figures.iterator();
        for (int i = 0; fi.hasNext(); i++) {
            Figure f = fi.next();
            model.addElement(f);
            if (selected == f)
                selectedIndex = i;
        }

        if (selectedIndex >= figures.size())
            selectedIndex = figures.size() - 1;
        list.setSelectedIndex(selectedIndex);
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

    public Figure getSelectedFigure() {
        return (Figure) list.getSelectedValue();
    }

    public void setSelectedFigure(Figure f) {
        list.setSelectedValue(f, true);
    }
}
