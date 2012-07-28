/*
 * Copyright (c) 2009 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 23.07.2009
 */
package figurabia.ui.figureeditor;

import it.cnr.imaa.essi.lablib.gui.checkboxtree.CheckboxTree;
import it.cnr.imaa.essi.lablib.gui.checkboxtree.TreeCheckingEvent;
import it.cnr.imaa.essi.lablib.gui.checkboxtree.TreeCheckingListener;

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

import figurabia.domain.Figure;
import figurabia.domain.TreeItem;
import figurabia.domain.TreeItem.ItemType;
import figurabia.framework.FigureModel;
import figurabia.io.FiguresTreeStore;
import figurabia.service.FigureCreationService;
import figurabia.service.FigureUpdateService;
import figurabia.ui.util.JTreePopupMenu;

@SuppressWarnings("serial")
public class FigureList extends JPanel {

    private final FiguresTreeStore figuresTreeStore;
    private final FigureCreationService creationService;
    private final FigureUpdateService figureUpdateService;

    private FiguresTreeModel treeModel;
    private CheckboxTree tree;

    private JPopupMenu popupMenu;

    private static DataFlavor folderItemFlavor;
    private static DataFlavor uriListAsString;
    static {
        try {
            folderItemFlavor = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=\""
                    + TreeItem.class.getName() + "\"");
            uriListAsString = new DataFlavor("text/uri-list;class=java.lang.String");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static class FolderItemTransferable implements Transferable {

        private TreeItem item;

        private static DataFlavor[] flavors = new DataFlavor[] {
                folderItemFlavor,
                DataFlavor.stringFlavor
        };

        public FolderItemTransferable(TreeItem item) {
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

    public FigureList(FiguresTreeStore fts, FigureCreationService fcs, FigureUpdateService fus,
            final FigureModel figureModel) {
        this.figuresTreeStore = fts;
        this.creationService = fcs;
        this.figureUpdateService = fus;

        treeModel = new FiguresTreeModel(figuresTreeStore);
        tree = new CheckboxTree();
        tree.setModel(treeModel);
        tree.getSelectionModel().setSelectionMode(DefaultTreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.getCheckingModel().addTreeCheckingListener(new TreeCheckingListener() {
            @Override
            public void valueChanged(TreeCheckingEvent e) {
                TreePath p = e.getPath();
                TreeItem item = (TreeItem) p.getLastPathComponent();
                if (item != null) {
                    List<Figure> figures = figureUpdateService.getAllActiveFiguresInSubTree(item);
                    if (e.isCheckedPath())
                        figureModel.addToViewSet(figures);
                    else
                        figureModel.removeFromViewSet(figures);
                }
            }
        });
        TransferHandler transferHandler = new TransferHandler() {
            @Override
            public int getSourceActions(JComponent c) {
                return MOVE;
            }

            @Override
            protected Transferable createTransferable(JComponent c) {
                JTree t = (JTree) c;
                TreeItem item = (TreeItem) t.getSelectionPath().getLastPathComponent();
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
                try {
                    if (!canImport(support))
                        return false;

                    // get drop position
                    JTree.DropLocation dropLocation = (JTree.DropLocation) support.getDropLocation();

                    // get target folder and index
                    TreeItem target = (TreeItem) dropLocation.getPath().getLastPathComponent();
                    TreeItem targetParent;
                    if (target.getType() == ItemType.FOLDER)
                        targetParent = target;
                    else
                        targetParent = figuresTreeStore.getParentFolder(target);
                    int childIndex = dropLocation.getChildIndex();

                    if (support.isDataFlavorSupported(folderItemFlavor)) {
                        // selection is drag position
                        TreePath selected = tree.getSelectionPath();
                        tree.clearSelection();
                        //support.getTransferable().getTransferData(folderItemFlavor);
                        moveItem(selected, dropLocation.getPath(), dropLocation.getChildIndex());
                        // restore selection after move
                        tree.setSelectionPath(dropLocation.getPath().pathByAddingChild(selected.getLastPathComponent()));

                    } else if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        // import all files that were dropped
                        try {
                            @SuppressWarnings("unchecked")
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
                } catch (RuntimeException e) {
                    System.err.println("RuntimeException during drag and drop (import):");
                    e.printStackTrace();
                    return false;
                }
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
                deleteTreeItem(e);
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
                TreeItem item = getSelectedFolderItem();
                boolean isFigure = false;
                if (item.getType() == ItemType.ITEM) {
                    String figureId = item.getRefId();
                    boolean isActive = figureUpdateService.figureIsActive(figureId);
                    activeFigure.setSelected(isActive);
                    isFigure = true;
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

    private void createFigureForVideo(File video, TreeItem targetFolder, int childIndex) {
        // create a new figure for video
        try {
            creationService.createNewFigure(video, targetFolder,
                    childIndex);
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, e.getLocalizedMessage());
        }
    }

    private void setFigureActive(boolean newState) {
        try {
            TreeItem item = getSelectedFolderItem();
            figureUpdateService.setActive(item, newState);
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, e.getLocalizedMessage());
        } catch (RuntimeException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, e.getMessage());
        }
    }

    private TreeItem getSelectedFolderItem() {
        TreePath selection = tree.getSelectionModel().getSelectionPath();
        if (selection != null)
            return (TreeItem) selection.getLastPathComponent();
        return null;
    }

    private void renameFigure(ActionEvent e) {
        try {
            TreeItem item = getSelectedFolderItem();
            String newName = JOptionPane.showInputDialog((Component) e.getSource(), "Please enter the new name:",
                    item.getName());
            if (newName != null && !newName.equals("")) {
                figureUpdateService.setName(item, newName);
            }
        } catch (RuntimeException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error renaming figure: " + ex.getMessage());
        }
    }

    private void deleteTreeItem(ActionEvent e) {
        TreeItem item = getSelectedFolderItem();
        if (item.getType() == ItemType.ITEM) {
            if (JOptionPane.showConfirmDialog((Component) e.getSource(), "Are you sure you want to delete the figure '"
                    + item.getName()
                    + "'?", "Safety Warning", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                try {
                    figureUpdateService.delete(item);
                } catch (RuntimeException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog((Component) e.getSource(), "Error deleting: " + ex.getMessage());
                }
            }
        } else if (item.getType() == ItemType.FOLDER) {
            if (JOptionPane.showConfirmDialog((Component) e.getSource(), "Are you sure you want to delete the folder '"
                    + item.getName()
                    + "' and all its contents?", "Safety Warning", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                try {
                    figureUpdateService.delete(item);
                } catch (RuntimeException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog((Component) e.getSource(), "Error deleting: " + ex.getMessage());
                }
            }
        }
    }

    private void moveItem(TreePath pathToMove, TreePath newPath, int newIndex) {
        try {
            TreeItem itemToMove = (TreeItem) pathToMove.getLastPathComponent();
            TreeItem target = (TreeItem) newPath.getLastPathComponent();
            TreeItem newParent;
            if (target.getType() == ItemType.FOLDER)
                newParent = target;
            else {
                newParent = figuresTreeStore.getParentFolder(target);
                // FIXME newIndex is invalid in this case
                newIndex = 0;
            }
            newParent = figuresTreeStore.read(newParent.getId()); // get latest, because it sometimes seems to be stale
            figuresTreeStore.moveItem(itemToMove, newParent, newIndex);
        } catch (RuntimeException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error moving: " + ex.getMessage());
        }
    }

    private void cloneFigure(ActionEvent e) {
        try {
            TreeItem item = getSelectedFolderItem();
            creationService.cloneFigure(item);
        } catch (RuntimeException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error cloning: " + ex.getMessage());
        }
    }

    public void newFolder(ActionEvent e) {
        try {
            String name = JOptionPane.showInputDialog((Component) e.getSource(), "Please enter folder name:",
                    "");
            if (name != null && !name.equals("")) {
                TreeItem item = getSelectedFolderItem();
                creationService.createNewFolder(item, name);
            }
        } catch (RuntimeException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error creating new folder: " + ex.getMessage());
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
        TreeItem item = getSelectedFolderItem();
        return figureUpdateService.getFigure(item);
    }

    public void setSelectedFigure(Figure f) {
        TreeItem item = figuresTreeStore.getByRefId(f.getId());
        TreePath path = treeModel.createTreePath(item);
        tree.setSelectionPath(path);
    }
}
