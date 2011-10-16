/*
 * Copyright (c) 2009 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 22.08.2009
 */
package figurabia.ui;

import java.awt.CardLayout;
import java.awt.Container;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.ButtonGroup;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import figurabia.domain.Figure;
import figurabia.framework.FigureModel;
import figurabia.framework.FigurePositionListener;
import figurabia.framework.PersistenceProvider;
import figurabia.framework.Workspace;
import figurabia.framework.simpleimpl.SimpleWorkspace;
import figurabia.persistence.XStreamPersistenceProvider;
import figurabia.service.FigureCreationService;
import figurabia.ui.figureeditor.FigureEditPerspective;
import figurabia.ui.figureexplorer.FigureExplorerPerspective;
import figurabia.ui.figuremapper.FigureMapperPerspective;
import figurabia.ui.framework.Perspective;
import figurabia.ui.video.engine.MediaPlayer;

@SuppressWarnings("serial")
public class ApplicationFrame extends JFrame {

    private final static int INITIAL_WIDTH = 1000;
    private final static int INITIAL_HEIGHT = 750;

    private PersistenceProvider persistenceProvider;
    private Workspace workspace;

    private CardLayout cardLayout;

    private Perspective activePerspective;
    private FigureEditPerspective editPerspective;
    private FigureExplorerPerspective explorerPerspective;
    private FigureMapperPerspective mapperPerspective;
    private FigureModel figureModel;

    private JMenuBar appMenuBar;
    private JMenu fileMenu;
    private JMenuItem fileMenuImportVideo;
    private JMenu perspectiveMenu;
    private JMenuItem perspectiveMenuFigureEditor;
    private JMenuItem perspectiveMenuFigureExplorer;
    private JMenuItem perspectiveMenuFigureMapper;

    private List<Image> iconImages;

    private File currentDir = null;

    public ApplicationFrame(Workspace workspace_, PersistenceProvider persistenceProvider_) throws IOException {
        this.workspace = workspace_;
        this.persistenceProvider = persistenceProvider_;
        this.persistenceProvider.open();

        setTitle("Figurabia");
        Toolkit tk = Toolkit.getDefaultToolkit();
        iconImages = new ArrayList<Image>();
        iconImages.add(tk.getImage(getClass().getResource("res/figurabia_icon16__.png")));
        iconImages.add(tk.getImage(getClass().getResource("res/figurabia_icon32__.png")));
        iconImages.add(tk.getImage(getClass().getResource("res/figurabia_icon48.png")));
        setIconImages(iconImages);
        cardLayout = new CardLayout(0, 0);
        final Container contentPane = getContentPane();
        contentPane.setLayout(cardLayout); //new MigLayout("", "[fill]", "[fill]")

        final MediaPlayer player = new MediaPlayer();
        figureModel = new FigureModel();
        figureModel.addFigurePositionListener(new FigurePositionListener() {
            @Override
            public void update(Figure figure, int index) {
                // set video of figure
                File videoFile = new File(workspace.getVideoDir().getAbsoluteFile() + "/" + figure.getVideoName());
                player.openVideo(videoFile);
            }
        });

        // create and add FigureExplorerPerspective
        explorerPerspective = new FigureExplorerPerspective(workspace, persistenceProvider, player, figureModel);
        contentPane.add(explorerPerspective, explorerPerspective.getPerspectiveId());

        // create and add FigureEditPerspective
        editPerspective = new FigureEditPerspective(workspace, persistenceProvider, player, figureModel);
        contentPane.add(editPerspective, editPerspective.getPerspectiveId());

        // create and add FigureMapperPerspective
        mapperPerspective = new FigureMapperPerspective(persistenceProvider);
        contentPane.add(mapperPerspective, mapperPerspective.getPerspectiveId());

        // set up menu bar
        appMenuBar = new JMenuBar();
        setJMenuBar(appMenuBar);

        fileMenu = new JMenu("File");
        //fileMenu.getPopupMenu().setLightWeightPopupEnabled(false);
        appMenuBar.add(fileMenu);
        fileMenuImportVideo = new JMenuItem("Import Video...");
        fileMenuImportVideo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                importVideo();
            }
        });
        fileMenu.add(fileMenuImportVideo);

        perspectiveMenu = new JMenu("Perspective");
        //perspectiveMenu.getPopupMenu().setLightWeightPopupEnabled(false);
        appMenuBar.add(perspectiveMenu);

        perspectiveMenuFigureEditor = new JRadioButtonMenuItem("Figure Editor");
        perspectiveMenuFigureEditor.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                switchToPerspective(editPerspective);
            }
        });
        perspectiveMenu.add(perspectiveMenuFigureEditor);

        perspectiveMenuFigureExplorer = new JRadioButtonMenuItem("Figure Explorer");
        perspectiveMenuFigureExplorer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                switchToPerspective(explorerPerspective);
            }
        });
        perspectiveMenu.add(perspectiveMenuFigureExplorer);

        perspectiveMenuFigureMapper = new JRadioButtonMenuItem("Figure Mapper");
        perspectiveMenuFigureMapper.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                switchToPerspective(mapperPerspective);
            }
        });
        perspectiveMenu.add(perspectiveMenuFigureMapper);

        ButtonGroup perspectiveButtonGroup = new ButtonGroup();
        perspectiveButtonGroup.add(perspectiveMenuFigureEditor);
        perspectiveButtonGroup.add(perspectiveMenuFigureExplorer);
        perspectiveButtonGroup.add(perspectiveMenuFigureMapper);
        perspectiveMenuFigureExplorer.setSelected(true);

        // set frame properties
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(INITIAL_WIDTH, INITIAL_HEIGHT);
        setVisible(true);

        // set up automatic saving on closing
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    System.out.println("DEBUG: window closed");
                    ApplicationFrame.this.persistenceProvider.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Problem with persisting data.\n" + ex.getLocalizedMessage());
                }
            }
        });

        Timer timer = new Timer(5 * 60 * 1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    System.out.println("DEBUG: auto saving data...");
                    ApplicationFrame.this.persistenceProvider.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Problem with autosaving of data.\n" + ex.getLocalizedMessage());
                }
            }
        });
        timer.start();
    }

    private void importVideo() {
        // select video file
        JFileChooser fileChooser = new JFileChooser(currentDir);
        fileChooser.setMultiSelectionEnabled(false);
        if (fileChooser.showOpenDialog(ApplicationFrame.this) == JFileChooser.CANCEL_OPTION) {
            return;
        }
        currentDir = fileChooser.getCurrentDirectory();

        /*// open it with the video import view
        VideoImport videoImport = new VideoImport("file:"
                + fileChooser.getSelectedFile().getAbsolutePath(),
                ApplicationFrame.this.persistenceProvider, ApplicationFrame.this.workspace);
        SimplePanelFrame frame = new SimplePanelFrame(videoImport, 800, 600, false);
        frame.setIconImages(iconImages);
        frame.setTitle("Import Video " + fileChooser.getSelectedFile());*/

        // create a new figure
        Figure figure = null;
        try {
            figure = new FigureCreationService(workspace, persistenceProvider).createNewFigure(fileChooser.getSelectedFile());
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, e.getLocalizedMessage());
        }

        switchToPerspective(editPerspective);

        // select the new figure
        if (figure != null) {
            figureModel.setCurrentFigure(figure, -1);
        }
    }

    private void switchToPerspective(Perspective perspective) {
        cardLayout.show(getContentPane(), perspective.getPerspectiveId());
        if (activePerspective != null) {
            activePerspective.updateOnPerspectiveSwitch(false);
        }
        activePerspective = perspective;
        activePerspective.updateOnPerspectiveSwitch(true);
    }

    public static void main(String[] args) {
        boolean black = args.length == 0 ? false : args[0].equals("black");
        Locale.setDefault(Locale.US);
        JFrame.setDefaultLookAndFeelDecorated(true);
        JDialog.setDefaultLookAndFeelDecorated(true);
        try {
            //UIManager.setLookAndFeel(new GTKLookAndFeel());
            if (black) {
                UIManager.setLookAndFeel(new FigurabiaBlackLookAndFeel());
            } else {
                UIManager.setLookAndFeel(new FigurabiaLookAndFeel());
            }
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        final Workspace w = new SimpleWorkspace(new File("figurantdata"));
        final PersistenceProvider pp = new XStreamPersistenceProvider(new File(w.getDatabaseDir() + File.separator
                + "objects.xml"));

        // do UI construction in Swing's Event Dispatch Thread
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                try {
                    new ApplicationFrame(w, pp);
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Problem during startup:\n" + e.getLocalizedMessage());
                }
            }
        });
    }
}
