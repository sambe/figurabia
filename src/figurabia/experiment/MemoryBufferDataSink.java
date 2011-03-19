/*
 * Copyright (c) 2009 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 03.05.2009
 */
package figurabia.experiment;

import java.io.IOException;

import javax.media.DataSink;
import javax.media.IncompatibleSourceException;
import javax.media.MediaLocator;
import javax.media.datasink.DataSinkListener;
import javax.media.protocol.DataSource;

public class MemoryBufferDataSink implements DataSink {

    @Override
    public void addDataSinkListener(DataSinkListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void close() {
        // TODO Auto-generated method stub

    }

    @Override
    public String getContentType() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public MediaLocator getOutputLocator() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void open() throws IOException, SecurityException {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeDataSinkListener(DataSinkListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setOutputLocator(MediaLocator output) {
        // TODO Auto-generated method stub

    }

    @Override
    public void start() throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void stop() throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void setSource(DataSource source) throws IOException,
            IncompatibleSourceException {
        // TODO Auto-generated method stub

    }

    @Override
    public Object getControl(String controlType) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object[] getControls() {
        // TODO Auto-generated method stub
        return null;
    }

}
