/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 24.10.2010
 */
package figurabia.ui.video.access;

/**
 * Thrown when a video file is not properly readable.
 */
@SuppressWarnings("serial")
public class BadVideoException extends RuntimeException {

    public BadVideoException() {
        super();
    }

    public BadVideoException(String message, Throwable cause) {
        super(message, cause);
    }

    public BadVideoException(String message) {
        super(message);
    }

    public BadVideoException(Throwable cause) {
        super(cause);
    }

}
