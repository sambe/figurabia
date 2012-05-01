/*
 * Copyright (c) 2009 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 23.07.2009
 */
package figurabia.ui.figureeditor;

import it.cnr.imaa.essi.lablib.gui.checkboxtree.CheckboxTree;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;

import javax.swing.DropMode;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
import javax.swing.TransferHandler;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreePath;

import org.apache.commons.io.FileUtils;

import figurabia.domain.Figure;
import figurabia.domain.Folder;
import figurabia.domain.FolderItem;
import figurabia.framework.PersistenceProvider;
import figurabia.framework.Workspace;
import figurabia.service.FigureCreationService;
import figurabia.ui.util.JTreePopupMenu;

@SuppressWarnings("serial")
public class FigureList extends JPanel {

    private Workspace workspace;

    private PersistenceProvider persistenceProvider;

    //private JList list;
    private FiguresTreeModel treeModel;
    private CheckboxTree tree;

    private JPopupMenu popupMenu;

    private static DataFlavor folderItemFlavor;
    private static DataFlavor uriListAsString;
    static {
        try {
            folderItemFlavor = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=\""
                    + FolderItem.class.getName() + "\"");
            uriListAsString = new DataFlavor("text/uri-list;class=java.lang.String");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static class FolderItemTransferable implements Transferable {

        private FolderItem item;

        private static DataFlavor[] flavors = new DataFlavor[] {
                folderItemFlavor,
                DataFlavor.stringFlavor
        };

        public FolderItemTransferable(FolderItem item) {
            this.item = item;
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
            if (flavor.equals(folderItemFlavor))
                return item;
            if (flavor.equals(DataFlavor.stringFlavor))
                return item.getName();
            throw new UnsupportedFlavorException(flavor);
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return flavors.clone();
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            for (DataFlavor f : flavors) {
                if (f.equals(flavor))
                    return true;
            }
            return false;
        }

    }

    public FigureList(Workspace workspace_, PersistenceProvider pp) {
        this.workspace = workspace_;
        this.persistenceProvider = pp;
        //list = new JList();
        //list.setModel(new DefaultListModel());
        //list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        //setLayout(new BorderLayout());
        //JScrollPane scrollPane = new JScrollPane(list);
        //scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        //add(scrollPane, BorderLayout.CENTER);

        treeModel = new FiguresTreeModel(persistenceProvider);
        tree = new CheckboxTree();
        tree.setModel(treeModel);
        tree.getSelectionModel().setSelectionMode(DefaultTreeSelectionModel.SINGLE_TREE_SELECTION);
        TransferHandler transferHandler = new TransferHandler() {
            @Override
            public int getSourceActions(JComponent c) {
                return MOVE;
            }

            @Override
            protected Transferable createTransferable(JComponent c) {
                JTree t = (JTree) c;
                FolderItem item = (FolderItem) t.getSelectionPath().getLastPathComponent();
                return new FolderItemTransferable(item);
            }

            @Override
            protected void exportDone(JComponent source, Transferable data, int action) {
                // nothing to do, because move is only within tree
            }

            @Override
            public boolean canImport(TransferSupport support) {
                if (!support.isDrop())
                    return false;
                return true;
            }

            @Override
            public boolean importData(TransferSupport support) {
                if (!canImport(support))
                    return false;

                // get drop position
                JTree.DropLocation dropLocation = (JTree.DropLocation) support.getDropLocation();

                // get target folder and index
                FolderItem target = (FolderItem) dropLocation.getPath().getLastPathComponent();
                Folder targetParent;
                if (target instanceof Folder)
                    targetParent = (Folder) target;
                else
                    targetParent = target.getParent();
                int childIndex = dropLocation.getChildIndex();

                if (support.isDataFlavorSupported(folderItemFlavor)) {
                    // selection is drag position
                    TreePath selected = tree.getSelectionPath();
                    //support.getTransferable().getTransferData(folderItemFlavor);
                    moveItem(selected, dropLocation.getPath(), dropLocation.getChildIndex());

                } else if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    // import all files that were dropped
                    try {
                        List<File> files = (List<File>) support.getTransferable().getTransferData(
                                DataFlavor.javaFileListFlavor);
                        for (File f : files) {
                            createFigureForVideo(f, targetParent, childIndex);
                        }
                    } catch (UnsupportedFlavorException e) {
                        throw new IllegalStateException("Should not be possible to happen.", e);
                    } catch (IOException e) {
                        JOptionPane.showMessageDialog(tree, "Import failed due to IO error: " + e.getMessage());
                        e.printStackTrace();
                        return false;
                    }
                } else if (support.isDataFlavorSupported(uriListAsString)) {
                    try {
                        String uriStrings = (String) support.getTransferable().getTransferData(uriListAsString);
                        String[] uris = uriStrings.split("\n");
                        for (String uri : uris) {
                            if (uri == null || uri.isEmpty())
                                continue;
                            File f = new File(URI.create(uri.trim()));
                            createFigureForVideo(f, targetParent, childIndex);
                        }
                    } catch (RuntimeException e) {
                        System.err.println("Runtime Exception on accepting drop: ");
                        e.printStackTrace();
                        return false;
                    } catch (UnsupportedFlavorException e) {
                        throw new IllegalStateException("Should not be possible to happen.", e);
                    } catch (IOException e) {
                        JOptionPane.showMessageDialog(tree, "Import failed due to IO error: " + e.getMessage());
                        e.printStackTrace();
                        return false;
                    }
                } else {
                    return false;
                }
                return true;
            }
        };
        tree.setTransferHandler(transferHandler);
        tree.setDragEnabled(true);
        tree.setDropMode(DropMode.INSERT);

        setLayout(new BorderLayout());
        JScrollPane scrollPane = new JScrollPane(tree);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);

        setMinimumSize(new Dimension(150, 300));
        setPreferredSize(new Dimension(150, 300));

        // set up popup menu 
        popupMenu = new JPopupMenu();
        final JCheckBoxMenuItem activeFigure = new JCheckBoxMenuItem("Active");
        activeFigure.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                setFigureActive(activeFigure.getState());
            }
        });
        JMenuItem rename = new JMenuItem("Rename");
        rename.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                renameFigure(e);
            }
        });
        JMenuItem delete = new JMenuItem("Delete");
        delete.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteFigure(e);
            }
        });
        final JMenuItem cloneFigure = new JMenuItem("Clone");
        cloneFigure.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cloneFigure(e);
            }
        });
        JMenuItem newFolder = new JMenuItem("New Folder");
        newFolder.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                newFolder(e);
            }
        });
        popupMenu.add(activeFigure);
        popupMenu.add(rename);
        popupMenu.add(delete);
        popupMenu.add(cloneFigure);
        popupMenu.add(newFolder);
        // only required for standard gtk look and feel
        //popupMenu.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        popupMenu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                FolderItem fi = getSelectedFolderItem();
                boolean isFigure = false;
                if (fi instanceof Figure) {
                    Figure f = (Figure) fi;
                    activeFigure.setSelected(f.isActive());
                    isFigure = true;
                } else if (fi instanceof Folder) {

                }
                activeFigure.setEnabled(isFigure);
                cloneFigure.setEnabled(isFigure);
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });

        new JTreePopupMenu(tree, popupMenu); // connects the popup menu to the list
    }

    private void createFigureForVideo(File video, Folder targetFolder, int childIndex) {
        // create a new figure for video
        Figure figure = null;
        try {
            figure = new FigureCreationService(workspace, persistenceProvider).createNewFigure(video, targetFolder,
                    childIndex);
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, e.getLocalizedMessage());
        }
    }

    private void setFigureActive(boolean newState) {
        try {
            FolderItem fi = getSelectedFolderItem();
            if (fi instanceof Figure) {
                Figure f = (Figure) fi;
                if (newState) {
                    new FigureCreationService(workspace, persistenceProvider).validateAndFinalizeFigure(f);
                }
                f.setActive(newState);
                FigureList.this.persistenceProvider.updateFigure(f);
            } else {
                throw new IllegalStateException("called set figure active " + newState
                        + " on something else than a figure: " + fi);
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, e.getLocalizedMessage());
        } catch (RuntimeException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, e.getMessage());
        }
    }

    private FolderItem getSelectedFolderItem() {
        TreePath selection = tree.getSelectionModel().getSelectionPath();
        if (selection != null)
            return (FolderItem) selection.getLastPathComponent();
        return null;
    }

    private void renameFigure(ActionEvent e) {
        FolderItem fi = getSelectedFolderItem();
        if (fi instanceof Figure) {
            Figure f = (Figure) fi;
            String newName = JOptionPane.showInputDialog((Component) e.getSource(), "Please enter the new name:",
                    f.getName());
            if (newName != null && !newName.equals("")) {
                f.setName(newName);
                persistenceProvider.updateFigure(f);
            }
        } else if (fi instanceof Folder) {
            Folder f = (Folder) fi;
            String newName = JOptionPane.showInputDialog((Component) e.getSource(), "Please enter the new name:",
                    f.getName());
            if (newName != null && !newName.equals("")) {
                f.setName(newName);
                persistenceProvider.updateItem(f);
            }
        }
    }

    private void deleteFigure(ActionEvent e) {
        FolderItem fi = getSelectedFolderItem();
        if (fi instanceof Figure) {
            Figure f = (Figure) fi;
            if (JOptionPane.showConfirmDialog((Component) e.getSource(), "Are you sure you want to delete the figure '"
                    + f.toString()
                    + "'?", "Safety Warning", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                deleteFigure(f);
            }
        } else if (fi instanceof Folder) {
            Folder f = (Folder) fi;
            if (JOptionPane.showConfirmDialog((Component) e.getSource(), "Are you sure you want to delete the folder '"
                    + f.getName()
                    + "' and all its contents?", "Safety Warning", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                deleteFolder(f);
            }
        }
    }

    private void deleteFigure(Figure f) {
        Folder parent = f.getParent();
        int index = persistenceProvider.getItems(parent).indexOf(f);
        persistenceProvider.removeItem(parent, index);
        // TODO maybe delete video too
        try {
            workspace.deleteAllPictures(f.getId());
        } catch (IOException e1) {
            e1.printStackTrace();
            JOptionPane.showMessageDialog(null, "IO Error occured while deleting pictures of the deleted figure: "
                    + e1.getLocalizedMessage());
        }
    }

    private void deleteFolder(Folder f) {
        Folder parent = f.getParent();
        int index = persistenceProvider.getItems(parent).indexOf(f);
        persistenceProvider.removeItem(f.getParent(), index);
        // TODO pictures of removed figures are not removed here, probably better done at closing the application (or in a proper backend)
    }

    private void moveItem(TreePath pathToMove, TreePath newPath, int newIndex) {
        FolderItem itemToMove = (FolderItem) pathToMove.getLastPathComponent();
        FolderItem target = (FolderItem) newPath.getLastPathComponent();
        Folder newParent;
        if (target instanceof Folder)
            newParent = (Folder) target;
        else {
            newParent = target.getParent();
            // FIXME newIndex is invalid in this case
            newIndex = 0;
        }
        persistenceProvider.moveItem(itemToMove, newParent, newIndex);

        persistenceProvider.updateItem(itemToMove);
    }

    private void cloneFigure(ActionEvent e) {
        FolderItem fi = getSelectedFolderItem();
        if (fi instanceof Figure) {
            Figure f = (Figure) fi;
            String newName = JOptionPane.showInputDialog((Component) e.getSource(),
                    "Please enter a name for the cloned figure:", f.getName());
            if (newName != null && !newName.equals("")) {
                try {
                    Figure clone = persistenceProvider.cloneFigure(f);
                    clone.setName(newName);
                    File from = new File(workspace.getPictureDir() + File.separator + f.getId());
                    File to = new File(workspace.getPictureDir() + File.separator + clone.getId());
                    FileUtils.copyDirectory(from, to);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog((Component) e.getSource(),
                            "IO Error occured while copying pictures of the cloned figure: " + ex.getLocalizedMessage());
                }
            }
        } else {
            throw new IllegalStateException("called cloneFigure with something else than a figure: " + fi);
        }
    }

    public void newFolder(ActionEvent e) {
        String name = JOptionPane.showInputDialog((Component) e.getSource(), "Please enter folder name:",
                "");
        if (name != null && !name.equals("")) {
            int index = 0;
            FolderItem fi = getSelectedFolderItem();
            if (!(fi instanceof Folder)) {
                index = persistenceProvider.getItems(fi.getParent()).indexOf(fi) + 1;
                fi = fi.getParent();
            }
            Folder parent = (Folder) fi;
            persistenceProvider.newFolder(name, index, parent);
        }
    }

    public void addTreeSelectionListener(TreeSelectionListener listener) {
        tree.getSelectionModel().addTreeSelectionListener(listener);
    }

    /**
     * @param listener
     * @see javax.swing.JList#removeListSelectionListener(javax.swing.event.ListSelectionListener)
     */
    public void removeTreeSelectionListener(TreeSelectionListener listener) {
        tree.getSelectionModel().removeTreeSelectionListener(listener);
    }

    // TODO probably this should be removed (or fixed)
    public Figure getSelectedFigure() {
        FolderItem fi = getSelectedFolderItem();
        if (fi instanceof Figure)
            return (Figure) fi;
        return null;
    }

    public void setSelectedFigure(Figure f) {
        TreePath path = treeModel.createTreePath(f);
        tree.setSelectionPath(path);
    }
}
