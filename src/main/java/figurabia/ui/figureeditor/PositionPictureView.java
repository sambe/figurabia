/*
 * Copyright (c) 2009 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 23.07.2009
 */
package figurabia.ui.figureeditor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.ImageObserver;

import javax.swing.JPanel;

import figurabia.domain.Figure;
import figurabia.framework.Workspace;

@SuppressWarnings("serial")
public class PositionPictureView extends JPanel {

    private Workspace workspace;

    @SuppressWarnings("unused")
    private Figure figure;
    @SuppressWarnings("unused")
    private int index;

    private Image mainImage;
    private Image image2, image3, image4;
    private Rectangle mainRect, rect2, rect3, rect4;
    private int bar;
    private int beat;

    private int beatOffset = 0;

    public PositionPictureView(Workspace workspace) {
        this.workspace = workspace;
        setMinimumSize(new Dimension(200, 200));
        setBackground(Color.BLACK);
        setForeground(Color.WHITE);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // if not yet initialized
                if (mainRect == null)
                    return;
                // determine area where it was clicked
                if (mainRect.contains(e.getPoint())) {
                    setMainPicture(0);
                } else if (rect2.contains(e.getPoint())) {
                    setMainPicture(1);
                } else if (rect3.contains(e.getPoint())) {
                    setMainPicture(2);
                } else if (rect4.contains(e.getPoint())) {
                    setMainPicture(3);
                }
                requestFocusInWindow();
            }
        });
    }

    public void setMainPicture(int beatOffset) {
        this.beatOffset = beatOffset;
        repaint();
    }

    public void setPosition(Figure f, int index) {
        this.figure = f;
        this.index = index;
        this.beatOffset = 0;

        // get pictures
        bar = index / 2 + 1;
        beat = index % 2 == 0 ? 1 : 5;
        mainImage = workspace.getPicture(f.getId(), bar, beat);
        image2 = workspace.getPicture(f.getId(), bar, beat + 1);
        image3 = workspace.getPicture(f.getId(), bar, beat + 2);
        image4 = workspace.getPicture(f.getId(), bar, beat + 3);

        repaint();
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

    @Override
    public void paint(Graphics g) {
        // compute largest square
        Dimension dim = getSize();
        int sideLength, xOffset, yOffset;
        if (dim.height > dim.width) {
            sideLength = dim.width;
            xOffset = 0;
            yOffset = (dim.height - sideLength) / 2;
        } else {
            sideLength = dim.height;
            xOffset = (dim.width - sideLength) / 2;
            yOffset = 0;
        }

        // paint background black
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, dim.width, dim.height);

        // paint pictures
        g.setColor(Color.WHITE);
        int threeQuarter = sideLength * 3 / 4;
        Image bigImage = mainImage;
        int bigImageBeat = beat + beatOffset;
        if (beatOffset == 1)
            bigImage = image2;
        else if (beatOffset == 2)
            bigImage = image3;
        else if (beatOffset == 3)
            bigImage = image4;

        if (bigImage != null) {
            mainRect = new Rectangle(xOffset, yOffset, sideLength, threeQuarter);
            g.drawImage(bigImage, mainRect.x, mainRect.y, mainRect.width, mainRect.height, imageObserver);
            g.drawString(String.valueOf(bigImageBeat), mainRect.x + mainRect.width - 10, mainRect.y + mainRect.height
                    - 4);
        }
        if (image2 != null) {
            rect2 = new Rectangle(xOffset, yOffset + threeQuarter, sideLength / 3, sideLength / 4);
            g.drawImage(image2, rect2.x, rect2.y, rect2.width, rect2.height, imageObserver);
            g.drawString(String.valueOf(beat + 1), rect2.x + rect2.width - 10, rect2.y + rect2.height - 4);
        }
        if (image3 != null) {
            rect3 = new Rectangle(xOffset + sideLength / 3, yOffset + threeQuarter, sideLength / 3, sideLength / 4);
            g.drawImage(image3, rect3.x, rect3.y, rect3.width, rect3.height, imageObserver);
            g.drawString(String.valueOf(beat + 2), rect3.x + rect3.width - 10, rect3.y + rect3.height - 4);
        }
        if (image4 != null) {
            rect4 = new Rectangle(xOffset + sideLength * 2 / 3, yOffset + threeQuarter, sideLength / 3, sideLength / 4);
            g.drawImage(image4, rect4.x, rect4.y, rect4.width, rect4.height, imageObserver);
            g.drawString(String.valueOf(beat + 3), rect4.x + rect4.width - 10, rect4.y + rect4.height - 4);
        }
    }
}
