/*
 * Copyright (c) 2011 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 18.09.2011
 */
package figurabia.ui.video.engine.messages;

public class Explicit<T> {

    public final T _;

    public <U extends T> Explicit(U _) {
        this._ = _;
    }
}
