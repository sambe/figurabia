/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 04.07.2010
 */
package figurabia.ui.figuremapper;

import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import figurabia.framework.PersistenceProvider;
import figurabia.ui.framework.Perspective;

@SuppressWarnings("serial")
public class FigureMapperPerspective extends JPanel implements Perspective {

    FigureMapScreen mapScreen;

    public FigureMapperPerspective(PersistenceProvider pp) {
        mapScreen = new FigureMapScreen(pp);

        setLayout(new MigLayout("ins 0", "[fill]", "[fill]"));
        add(mapScreen, "push");
    }

    /**
     * Does whatever needs to be updated after a perspective switch to this perspective.
     */
    public void updateOnPerspectiveSwitch(boolean active) {
        System.out.println("DEBUG: FigureMapperPerspective active = " + active);
        if (active)
            mapScreen.refreshData();
    }

    @Override
    public String getPerspectiveId() {
        return "mapperPerspective";
    }
}
