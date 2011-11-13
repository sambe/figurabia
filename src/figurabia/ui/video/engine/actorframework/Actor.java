/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 31.10.2010
 */
package figurabia.ui.video.engine.actorframework;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.LockSupport;

import figurabia.ui.video.engine.messages.MediaError;

/**
 * A base class for an actor.
 * 
 * @author Samuel Berner
 */
public abstract class Actor implements MessageSendable {

    private final Thread thread;
    private volatile boolean stopped;
    private final ConcurrentLinkedQueue<Object> queue;
    private final Actor errorHandler;
    private final Map<Class, List<MessageSendable>> updateReceivers;

    private final int idleNanos;

    protected Actor(Actor errorHandler, int idleNanos) {
        String className = this.getClass().getSimpleName();
        if (className.equals("")) {
            className = "Anonymous Actor";
        }
        thread = new Thread(new ActorRunnable(), className);
        queue = new ConcurrentLinkedQueue<Object>();
        this.errorHandler = errorHandler;
        updateReceivers = new HashMap<Class, List<MessageSendable>>();
        this.idleNanos = idleNanos;
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
                        if (message instanceof RegisterForUpdates) {
                            handleRegisterForUpdates((RegisterForUpdates) message);
                        } else {
                            act(message);
                        }
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
        if (idleNanos == -1)
            LockSupport.park();
        else
            LockSupport.parkNanos(idleNanos);
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
        LockSupport.unpark(thread);
    }

    /**
     * Sends a message to the actor, which means it will be put into the queue and the method returns immediately.
     * 
     * @param message the message to send
     */
    public final void send(Object message) {
        queue.add(message);
        LockSupport.unpark(thread);
    }

    /**
     * Handles the registration for updates.
     */
    private void handleRegisterForUpdates(RegisterForUpdates message) {
        List<MessageSendable> receivers = updateReceivers.get(message.messageType);
        if (receivers == null) {
            receivers = new ArrayList<MessageSendable>();
            updateReceivers.put(message.messageType, receivers);
        }
        receivers.add(message.receiver);
    }

    /**
     * Sends an update to the registered receivers.
     * 
     * @param message the message to send to the receivers
     */
    protected final void sendUpdate(Object message) {
        Class messageType = message.getClass();
        List<MessageSendable> receivers = updateReceivers.get(messageType);
        if (receivers != null) {
            for (MessageSendable ms : receivers) {
                ms.send(message);
            }
        }
    }
}
