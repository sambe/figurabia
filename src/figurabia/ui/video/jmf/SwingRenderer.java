package figurabia.ui.video.jmf;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.util.Vector;

import javax.media.Buffer;
import javax.media.Format;
import javax.media.ResourceUnavailableException;
import javax.media.format.RGBFormat;
import javax.media.format.VideoFormat;

import com.sun.media.renderer.video.BasicVideoRenderer;
import com.sun.media.renderer.video.Blitter;
import com.sun.media.renderer.video.Java2DRenderer;
import com.sun.media.util.Arch;

import figurabia.ui.video.VideoScreen;

public class SwingRenderer extends BasicVideoRenderer {
    /*************************************************************************
     * Variables and Constants
     *************************************************************************/

    // The descriptive name of this renderer
    private static final String MyName = "Swing Renderer";

    private transient Vector cacheInputData = null;
    private transient Vector cacheInputImage = null;
    private transient Vector cacheOutputImage = null;
    private transient Image lastImage = null;

    private RGBFormat supportedRGB = null;
    private RGBFormat supportedOther = null;

    private transient int lastWidth = 1;
    private transient int lastHeight = 1;
    private Blitter blitter = null;

    static public String vendor = null;
    static public boolean runningOnMac = false;

    static {
        try {
            vendor = System.getProperty("java.vendor");
            if (vendor != null) {
                vendor = vendor.toUpperCase();
                if (vendor.startsWith("APPLE")) {
                    runningOnMac = true;
                }
            }
        } catch (Throwable e) {
            // Non-fatal error.  No need to do anything.
        }
    }

    private static SwingRenderer activeInstance;

    public static SwingRenderer getNewestInstance() {
        return activeInstance;
    }

    /*************************************************************************
     * Constructor
     *************************************************************************/

    public SwingRenderer() {
        this(MyName);
    }

    public SwingRenderer(String name) {
        super(name);
        activeInstance = this;

        // Prepare supported input formats and preferred format
        int rMask, gMask, bMask;
        if ((Arch.getArch() & Arch.SOLARIS) != 0 && !runningOnMac) {
            rMask = 0x000000FF;
            gMask = 0x0000FF00;
            bMask = 0x00FF0000;
        } else {
            bMask = 0x000000FF;
            gMask = 0x0000FF00;
            rMask = 0x00FF0000;
        }

        supportedRGB = new RGBFormat(null, Format.NOT_SPECIFIED,
                Format.intArray,
                Format.NOT_SPECIFIED, // frame rate
                32,
                rMask, gMask, bMask,
                1, Format.NOT_SPECIFIED,
                Format.FALSE, // flipped
                Format.NOT_SPECIFIED // endian
        );
        supportedOther = new RGBFormat(null, Format.NOT_SPECIFIED,
                Format.intArray,
                Format.NOT_SPECIFIED, // frame rate
                32,
                bMask, gMask, rMask,
                1, Format.NOT_SPECIFIED,
                Format.FALSE, // flipped
                Format.NOT_SPECIFIED // endian
        );

        supportedFormats = new VideoFormat[2];
        supportedFormats[0] = supportedRGB;
        supportedFormats[1] = supportedOther;

        // Setting ColorModel does not really work on the Mac.
        // So there's only one format supported by the Mac.
        if (runningOnMac)
            supportedFormats[1] = supportedRGB;

        blitter = new Java2DRenderer();
        //blitter.setRenderer(this);
    }

    public boolean isLightWeight() {
        return false;
    }

    /*************************************************************************
     * PlugIn methods
     *************************************************************************/

    /**
     * Opens the plug-in software or hardware component and acquires necessary resources. If all the needed resources
     * could not be acquired, it throws a ResourceUnavailableException. Data should not be passed into the plug-in
     * without first calling this method.
     */
    @Override
    public void open() throws ResourceUnavailableException {
        cacheInputData = new Vector();
        cacheInputImage = new Vector();
        cacheOutputImage = new Vector();
    }

    /**
     * Resets the state of the plug-in. Typically at end of media or when media is repositioned.
     */
    @Override
    public synchronized void reset() {
        cacheInputData = new Vector();
        cacheInputImage = new Vector();
        cacheOutputImage = new Vector();
    }

    /*************************************************************************
     * Renderer methods
     *************************************************************************/

    /**
     * Set the data input format.
     * 
     * @return false if the format is not supported.
     */
    @Override
    public Format setInputFormat(Format format) {
        if (super.setInputFormat(format) != null) {
            reset();
            return format;
        } else
            return null;
    }

    /**
     * Processes the data and renders it to the output device represented by this renderer.
     */
    @Override
    protected synchronized int doProcess(Buffer buffer) {
        if (component == null)
            return BUFFER_PROCESSED_OK;
        if (!buffer.getFormat().equals(inputFormat)) {
            Format in = buffer.getFormat();
            if (matches(in, supportedFormats) == null)
                return BUFFER_PROCESSED_FAILED;
            else {
                inputFormat = (RGBFormat) in;
            }
        }
        Object data = buffer.getData();
        if (!(data instanceof int[]))
            return BUFFER_PROCESSED_FAILED;

        int cacheSize = cacheInputData.size();
        int i;
        boolean found = false;
        for (i = 0; i < cacheSize; ++i) {
            Object bufKnown = cacheInputData.elementAt(i);
            if (bufKnown == data) {
                found = true;
                break;
            }
        }

        // Data not cached? Add to the cache and create a new Image wrapper.
        if (!found)
            i = blitter.newData(buffer,
                    cacheInputImage,
                    cacheOutputImage,
                    cacheInputData);

        // Couldn't create Image
        if (i < 0)
            return BUFFER_PROCESSED_FAILED;
        RGBFormat format = (RGBFormat) buffer.getFormat();
        Dimension size = format.getSize();
        inWidth = size.width;
        inHeight = size.height;
        if (outWidth == -1)
            outWidth = size.width;
        if (outHeight == -1)
            outHeight = size.height;

        lastImage = blitter.process(buffer,
                cacheInputImage.elementAt(i),
                cacheOutputImage.elementAt(i),
                size);

        lastWidth = size.width;
        lastHeight = size.height;

        if (!isLightWeight()) {
            Graphics g = component.getGraphics();
            if (g != null) {
                blitter.draw(g, component, lastImage, 0, 0, outWidth, outHeight,
                        0, 0, size.width, size.height);
            }
        } else {
            component.repaint();
        }
        return BUFFER_PROCESSED_OK;
    }

    @Override
    protected void repaint() {
        if (!isStarted() && lastImage != null) {
            Graphics g = component.getGraphics();
            blitter.draw(g, component, lastImage, 0, 0, outWidth, outHeight,
                    0, 0, lastWidth, lastHeight);
        }
    }

    /**
     * Returns an AWT component that it will render to. Returns null if it is not rendering to an AWT component.
     */
    @Override
    public java.awt.Component getComponent() {
        /*if (component == null) {
            if (isLightWeight()) {
                component = new LightComponent();
                component.setBackground(getPreferredBackground());
                if (compListener == null)
                    compListener = new CompListener();
                component.addComponentListener(compListener);
            } else
                component = super.getComponent();
        }*/
        return component;
    }

    public void setCurrentScreen(VideoScreen screen) {
        if (this.component != screen) {
            if (this.component != null) {
                ((VideoScreen) component).setActive(false);
            }
            this.component = screen;
            screen.setActive(true);
            blitter.resized(component);
        }
    }

    /*public synchronized void resized(Component c) {
        super.resized(c);
        if (blitter != this)
            blitter.resized(c);
    }*/

    /****************************************************************
     * Blitter methods
     ****************************************************************/

    /*public Image process(Buffer buffer, Object cacheInputImage,
            Object cacheOutputImage, Dimension size) {

        MemoryImageSource sourceImage = (MemoryImageSource) cacheInputImage;
        Image lastImage = (Image) cacheOutputImage;
        sourceImage.newPixels(0, 0, size.width, size.height);
        return lastImage;
    }

    public void draw(Graphics g, Component component, Image lastImage,
            int dx, int dy, int dw, int dh,
            int sx, int sy, int sw, int sh) {
        if (g != null)
            g.drawImage(lastImage, dx, dy, dw, dh,
                    sx, sy, sw, sh, component);
    }*/

    public void paint(Graphics g) {
        if (g != null && lastImage != null)
            blitter.draw(g, component, lastImage, 0, 0, outWidth, outHeight,
                    0, 0, lastWidth, lastHeight);
    }

    /*public int newData(Buffer buffer, Vector cacheInputImage,
            Vector cacheOutputImage, Vector cacheInputData) {
        Object data = buffer.getData();

        if (!(data instanceof int[]))
            return -1;
        RGBFormat format = (RGBFormat) buffer.getFormat();

        DirectColorModel dcm = new DirectColorModel(format.getBitsPerPixel(),
                format.getRedMask(),
                format.getGreenMask(),
                format.getBlueMask());

        MemoryImageSource sourceImage = new MemoryImageSource(format.getLineStride(),
                format.getSize().height,
                dcm,
                (int[]) data, 0,
                format.getLineStride());
        sourceImage.setAnimated(true);
        sourceImage.setFullBufferUpdates(true);
        Image destImage = null;
        if (component != null) {
            destImage = component.createImage(sourceImage);
            component.prepareImage(destImage, component);
        }
        cacheOutputImage.addElement(destImage);
        cacheInputData.addElement(data);
        cacheInputImage.addElement(sourceImage);
        return cacheInputImage.size() - 1;
    }*/

    @SuppressWarnings("serial")
    public class LightComponent extends Component {
        @Override
        public synchronized void paint(Graphics g) {
            SwingRenderer.this.paint(g);
        }

        @Override
        public synchronized void update(Graphics g) {
        }

        @Override
        public Dimension getMinimumSize() {
            return new Dimension(1, 1);
        }

        @Override
        public Dimension getPreferredSize() {
            return myPreferredSize();
        }

        @Override
        public synchronized void addNotify() {
            super.addNotify();
            setAvailable(true);
        }

        @Override
        public synchronized void removeNotify() {
            setAvailable(false);
            super.removeNotify();
        }
    }
}
