/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 31.10.2010
 */
package figurabia.ui.video.engine.actorframework;

import java.util.concurrent.ConcurrentLinkedQueue;

import figurabia.ui.video.engine.messages.MediaError;

/**
 * A base class for an actor.
 * 
 * @author Samuel Berner
 */
public abstract class Actor implements MessageSendable {

    private Thread thread;
    private volatile boolean stopped;
    protected ConcurrentLinkedQueue<Object> queue;
    private Actor errorHandler;

    protected Actor(Actor errorHandler) {
        String className = this.getClass().getSimpleName();
        if (className.equals("")) {
            className = "Anonymous Actor";
        }
        thread = new Thread(new ActorRunnable(), className);
        queue = new ConcurrentLinkedQueue<Object>();
        this.errorHandler = errorHandler;
    }

    private class ActorRunnable implements Runnable {
        @Override
        public void run() {
            try {
                init();
            } catch (Exception e) {
                handleException("exception in init()", e);
                return;
            }
            while (!stopped) {
                Object message = queue.poll();

                if (message != null) {
                    try {
                        act(message);
                    } catch (RuntimeException e) {
                        handleException("Unhandled Exception when processing " + message, e);
                    }
                } else {
                    try {
                        idle();
                    } catch (RuntimeException e) {
                        handleException("Unhandled exception in idle()", e);
                    }
                }
            }
            try {
                destruct();
            } catch (RuntimeException e) {
                handleException("Unhandled exception in destruct()", e);
            }
        }
    }

    /**
     * Processing a message. This method has to be implemented by the concrete class extending this class.
     * 
     * @param message the message to process
     */
    protected abstract void act(Object message);

    /**
     * Waiting for a while for a new message. This is called when the actor idles. It is overridable so you can do other
     * processing during the waiting
     */
    protected void idle() {
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            // stop sleeping here
        }
    }

    /**
     * Does whatever initialization needs to be done by the handler thread before starting to handle messages.
     */
    protected void init() throws Exception {

    }

    /**
     * Releasing the state of this actor. This should be overriden by the implementing actor.
     */
    protected void destruct() {

    }

    /**
     * Handling the exception with the built in error handler.
     * 
     * @param message the error message
     * @param e the exception
     */
    protected final void handleException(String message, Exception e) {
        errorHandler.send(new MediaError(this, message, e));
    }

    /**
     * Starts the actor.
     */
    public final void start() {
        thread.start();
    }

    /**
     * Returns immediately and stops the actor after processing the current message.
     */
    public final void stop() {
        stopped = true;
    }

    /**
     * Sends a message to the actor, which means it will be put into the queue and the method returns immediately.
     * 
     * @param message the message to send
     */
    public final void send(Object message) {
        queue.add(message);
    }
}
