/*
 * Copyright (c) 2009 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 18.08.2009
 */
package figurabia.experiment;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

import javax.swing.JPanel;

import figurabia.ui.util.SimplePanelFrame;

@SuppressWarnings("serial")
public class Java2D extends JPanel {

    @Override
    protected void paintComponent(Graphics g) {

        Graphics2D g2 = (Graphics2D) g;

        Rectangle2D outerRectangle = new Rectangle2D.Double(40, 40, 320, 320);

        Rectangle2D innerRectangle1 = new Rectangle2D.Double(80, 80, 320, 100);
        Rectangle2D innerRectangle2 = new Rectangle2D.Double(80, 220, 240, 100);

        Path2D path = new Path2D.Double(Path2D.WIND_EVEN_ODD);
        /*path.moveTo(40, 40);
        path.lineTo(40, 320);
        path.lineTo(320, 320);
        path.lineTo(320, 40);
        path.closePath();

        path.moveTo(20, 80);
        path.lineTo(20, 100);
        path.lineTo(320, 100);
        path.lineTo(320, 80);
        path.closePath();*/
        path.append(outerRectangle, false);
        path.append(innerRectangle1, false);
        path.append(innerRectangle2, false);

        g2.fill(path);

        g2.clip(path);

        Rectangle2D verticalRectangle = new Rectangle2D.Double(160, 20, 20, 320);

        g2.setColor(Color.RED);
        g2.fill(verticalRectangle);
    }

    public static void main(String[] args) {
        new SimplePanelFrame(new Java2D(), 400, 400);
    }
}
