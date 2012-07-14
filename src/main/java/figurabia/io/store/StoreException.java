/*
 * Copyright (c) 2012 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on Jun 24, 2012
 */
package figurabia.io.store;

/**
 * Exception that is thrown in case of problems persisting in the store.
 * 
 * @author Samuel Berner
 */
public class StoreException extends RuntimeException {

    public StoreException() {
        super();
        // TODO Auto-generated constructor stub
    }

    public StoreException(String message, Throwable cause) {
        super(message, cause);
        // TODO Auto-generated constructor stub
    }

    public StoreException(String message) {
        super(message);
        // TODO Auto-generated constructor stub
    }

    public StoreException(Throwable cause) {
        super(cause);
        // TODO Auto-generated constructor stub
    }

}
