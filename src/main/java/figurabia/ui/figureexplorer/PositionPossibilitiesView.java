/*
 * Copyright (c) 2009 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 19.10.2009
 */
package figurabia.ui.figureexplorer;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JPanel;

import figurabia.domain.Element;
import figurabia.domain.Figure;
import figurabia.domain.PuertoOffset;
import figurabia.domain.PuertoPosition;
import figurabia.framework.FigurabiaModel;
import figurabia.framework.ViewSetListener;
import figurabia.io.BeatPictureCache;
import figurabia.io.FigureStore;
import figurabia.io.ProxyImage;
import figurabia.io.ProxyImage.ImageUpdateListener;
import figurabia.service.FiguresByPositionService;
import figurabia.service.FiguresByPositionService.Result;
import figurabia.ui.positionviewer.PositionPainter;

@SuppressWarnings("serial")
public class PositionPossibilitiesView extends JPanel {

    private final FigureStore figureStore;
    private final BeatPictureCache beatPictureCache;
    private final FiguresByPositionService service;

    private PuertoPosition currentPosition;
    private PuertoOffset currentOffset;

    public interface FigureLinkActionListener {
        void linkActivated(Figure figure, int index);
    }

    private List<FigureLinkActionListener> actionListeners = new ArrayList<FigureLinkActionListener>();

    public PositionPossibilitiesView(FigureStore fs, BeatPictureCache bpc, final FigurabiaModel figurabiaModel) {
        figureStore = fs;
        beatPictureCache = bpc;
        service = new FiguresByPositionService(figurabiaModel);
        figurabiaModel.addViewSetListener(new ViewSetListener() {
            @Override
            public void update(ChangeType type, List<Figure> changed) {
                service.init(figurabiaModel.getViewSet());
                refreshRelatedFigures();
            }
        });

        //setLayout(new MigLayout("nogrid")); // problem: puts everything in one line
        setLayout(new FlowLayout());
    }

    /**
     * This is overriden to prevent the outer layout manager from making the component too large.
     * 
     * @see javax.swing.JComponent#getPreferredSize()
     */
    @Override
    public Dimension getPreferredSize() {
        Dimension pref = super.getPreferredSize();
        int maxWidth = RelatedFiguresPanel.MAX_WIDTH;
        if (pref.width > maxWidth) {
            pref.width = maxWidth;
            int gap = ((FlowLayout) getLayout()).getVgap();
            int extraRows = (pref.width - maxWidth) / maxWidth + 1;
            pref.height += extraRows * (RelatedFiguresPanel.SQUARE_SIDE + gap);
        }
        return pref;
    }

    public void setPosition(PuertoPosition p, PuertoOffset offset) {
        if (p.equals(currentPosition) && offset.equals(currentOffset))
            return; // nothing to do
        currentPosition = p;
        currentOffset = offset;

        refreshRelatedFigures();
    }

    private void refreshRelatedFigures() {
        // retrieve figures for position
        Map<Element, List<Result>> figures = service.retrieveFiguresByPosition(currentPosition);

        // remove and dispose all previous panels (to cleanup listeners)
        for (java.awt.Component c : getComponents()) {
            if (c instanceof RelatedFiguresPanel) {
                RelatedFiguresPanel p = (RelatedFiguresPanel) c;
                p.dispose();
            }
        }
        removeAll();
        // add a panel for each figure
        for (Element element : figures.keySet()) {
            List<Result> results = figures.get(element);

            RelatedFiguresPanel panel = new RelatedFiguresPanel(element, results);

            add(panel);
        }

        // relayout
        getParent().validate(); // validates parent because scroll bars need to be updated too
        repaint();
    }

    protected void notifyFigureLinkActionListeners(Figure figure, int index) {
        for (FigureLinkActionListener l : actionListeners) {
            try {
                l.linkActivated(figure, index);
            } catch (RuntimeException e) {
                // catch exceptions here to avoid unnecessary effects
                System.err.println("Exception from a FigureLinkActionListener. Figure: " + figure + " position: "
                        + index);
                e.printStackTrace();
            }
        }

    }

    public void addFigureLinkActionListener(FigureLinkActionListener listener) {
        actionListeners.add(listener);
    }

    public void removeFigureLinkActionListener(FigureLinkActionListener listener) {
        actionListeners.remove(listener);
    }

    private class RelatedFiguresPanel extends JPanel {

        private final static int SQUARE_SIDE = 120;
        private final static int OFFSET = SQUARE_SIDE / 6;
        private final static int MAX_THUMBS = 8;
        private final static int MAX_WIDTH = (MAX_THUMBS + 1) * SQUARE_SIDE;

        private List<ProxyImage> thumbs;
        private PositionPainter painter;
        private JComponent positionPicture = new JComponent() {
            @Override
            public void paint(Graphics g) {
                // paint the position into the first square
                painter.paintPosition((Graphics2D) g, 0, 0, SQUARE_SIDE, SQUARE_SIDE);
            };
        };

        public RelatedFiguresPanel(Element element, List<Result> figures) {
            // truncate number of results in case there are too many of them
            if (figures.size() > MAX_THUMBS) {
                figures = figures.subList(0, MAX_THUMBS);
                // TODO make sure that first one is selected from every different figure present to prevent one figure from occupying all the spots
            }

            painter = new PositionPainter();
            painter.setPosition(element.getFinalPosition());
            painter.setOffset(element.getOffsetChange());
            painter.setBaseOffset(currentOffset);

            // obtain thumbnails
            thumbs = new ArrayList<ProxyImage>();
            for (int i = 0; i < figures.size(); i++) {
                int index = figures.get(i).index + 1;
                String pictureName = beatPictureCache.getPictureName(figures.get(i).figure.getId(), index / 2 + 1,
                        (index % 2) * 4 + 1);
                ProxyImage thumb = beatPictureCache.getScaledPicture(pictureName, SQUARE_SIDE * 4 / 3, SQUARE_SIDE);
                thumbs.add(thumb);
            }
            registerImageListeners(thumbs);

            setLayout(null);
            positionPicture.setBounds(0, 0, SQUARE_SIDE, SQUARE_SIDE);
            add(positionPicture);

            for (int i = 0; i < figures.size(); i++) {
                final Figure figure = figures.get(i).figure;
                final int index = figures.get(i).index;
                String elementName = figures.get(i).getElement().getName();
                if (elementName == null)
                    elementName = "Element " + (index + 1);
                String figureName = figure.getName();
                if (figureName == null)
                    figureName = figure.getVideoName();
                FigureThumbnail thumbnailComponent = new FigureThumbnail(thumbs.get(i), elementName + " (in "
                        + figureName + ")");
                thumbnailComponent.setBounds((i + 1) * SQUARE_SIDE, 0, SQUARE_SIDE, SQUARE_SIDE);
                thumbnailComponent.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (e.getButton() == MouseEvent.BUTTON1) {
                            // follow link, after left mouse click
                            notifyFigureLinkActionListeners(figure, index);
                        }
                    }
                });

                add(thumbnailComponent);
            }

            Dimension dim = new Dimension((figures.size() + 1) * SQUARE_SIDE, SQUARE_SIDE);
            setMinimumSize(dim);
            setPreferredSize(dim);
            setMaximumSize(dim);
        }

        public void dispose() {
            unregisterImageListeners(thumbs);
        }

        private class FigureThumbnail extends JComponent {

            private ProxyImage thumbnail;

            public FigureThumbnail(ProxyImage thumbnail, String elementName) {
                this.thumbnail = thumbnail;
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                setToolTipText(elementName);
            }

            @Override
            public void paint(Graphics g) {
                thumbnail.draw(g, 0, 0, SQUARE_SIDE, SQUARE_SIDE, OFFSET, 0, OFFSET + SQUARE_SIDE, SQUARE_SIDE);
            }
        }

        private void registerImageListeners(List<ProxyImage> images) {
            for (ProxyImage img : images) {
                img.addImageUpdateListener(imageUpdateListener);
            }
        }

        private void unregisterImageListeners(List<ProxyImage> images) {
            for (ProxyImage img : images) {
                img.removeImageUpdateListener(imageUpdateListener);
            }
        }

        private ImageUpdateListener imageUpdateListener = new ImageUpdateListener() {
            @Override
            public void imageUpdated(ProxyImage img) {
                repaint();
            }
        };
    }
}
