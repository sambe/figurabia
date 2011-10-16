/*
 * Copyright (c) 2011 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 24.09.2011
 */
package figurabia.framework;

import figurabia.domain.Figure;

public interface FigurePositionListener {

    void update(Figure f, int position);
}
