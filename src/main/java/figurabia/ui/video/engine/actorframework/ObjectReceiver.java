/*
 * Copyright (c) 2011 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 20.03.2011
 */
package figurabia.ui.video.engine.actorframework;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

public class ObjectReceiver implements MessageSendable {

    private ArrayBlockingQueue<Object> objectQueue;
    private int n;

    public ObjectReceiver() {
        this(1);
    }

    public ObjectReceiver(int n) {
        this.n = n;
        objectQueue = new ArrayBlockingQueue<Object>(n);
    }

    @Override
    public void send(Object message) {
        objectQueue.add(message);
    }

    public Object waitForMessage() {
        try {
            return objectQueue.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> List<T> waitForAllMessages(Class<T> messageType) {
        try {
            List<T> list = new ArrayList<T>();
            for (int i = 0; i < n; i++) {
                list.add(messageType.cast(objectQueue.take()));
            }
            return list;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
