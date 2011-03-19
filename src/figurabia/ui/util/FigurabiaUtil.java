/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 20.03.2010
 */
package figurabia.ui.util;

public class FigurabiaUtil {

    public static int indexToBar(int index) {
        return index / 2 + 1;
    }

    public static int indexToBeat(int index) {
        return index % 2 == 0 ? 1 : 5;
    }

    public static int index(int bar, int beat) {
        if (beat == 1) {
            return bar * 2;
        } else if (beat == 5) {
            return bar * 2 + 1;
        }
        throw new IllegalArgumentException("only beats 1 and 5 allowed, but was " + beat);
    }
}
