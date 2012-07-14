/*
 * Copyright (c) 2012 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on Jul 8, 2012
 */
package figurabia.io.workspace;

/**
 * Thrown in case there is a problem on the workspace layer.
 * 
 * @author Samuel Berner
 */
public class WorkspaceException extends RuntimeException {

    public WorkspaceException() {
        super();
    }

    public WorkspaceException(String message, Throwable cause) {
        super(message, cause);
    }

    public WorkspaceException(String message) {
        super(message);
    }

    public WorkspaceException(Throwable cause) {
        super(cause);
    }

}
