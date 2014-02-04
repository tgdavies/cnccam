package org.kablambda.cnccam;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;

import javax.imageio.ImageIO;
import javax.swing.*;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamEvent;
import com.github.sarxos.webcam.WebcamListener;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamPicker;


public class CNCCAM extends JFrame implements
        Runnable, WebcamListener, WindowListener, UncaughtExceptionHandler, ItemListener,
        MouseListener, MouseMotionListener, ClipboardOwner {

    private static final long serialVersionUID = 1L;

    private Webcam webcam = null;
    private WebcamPanel panel = null;
    private WebcamPicker picker = null;
    private JToolBar toolbar = null;
    private JTextField p1p2distField = null;
    private JTextField rField = null;
    private JLabel scaleLabel = null;
    // show a circle of this mm radius at center
    private double r = 0.0;

    // factor to convert from screen pixels to camera pixels -- i.e. screen_pixel * zoomFactor * scale = mm
    private double zoomFactor = 1.0;
    private JCheckBox mergeCheckbox = null;
    private JCheckBox centerLock = null;
    private JCheckBox minMax = null;

    @Override
    public void mouseDragged(MouseEvent e) {
        state = state.movePointTo(toStateCoords(e.getPoint()));
        onStateChanged();
    }

    private void onStateChanged() {
        // no zoom factor -- state distaces are in camera pixels
        p1p2distField.setText(String.format("%.3f", state.getP1P2Distance() * state.getScale()));
        scaleLabel.setText(scaleLabel());
        CNCCAMState.writeToPrefs(state);
    }

    @Override
    public void mouseMoved(MouseEvent e) {

    }

    @Override
    public void lostOwnership(Clipboard clipboard, Transferable contents) {

    }

    private enum Selection {
        NONE {
            @Override
            public CNCCAMState newState(CNCCAMState state) {
                return new CNCCAMState(state);
            }
        },
        CENTER {
            @Override
            public CNCCAMState newState(CNCCAMState state) {
                return new StateMoveCenter(state);
            }

        },
        P1 {
            @Override
            public CNCCAMState newState(CNCCAMState state) {
                return new StateMoveP1(state);
            }
        },
        P2 {
            @Override
            public CNCCAMState newState(CNCCAMState state) {
                return new StateMoveP2(state);
            }
        };

        public abstract CNCCAMState newState(CNCCAMState state);
    }

    private Selection selection = Selection.NONE;

    private CNCCAMState state = CNCCAMState.readFromPrefs();

    private final Action UP_ACTION = new AbstractAction("up") {
        @Override
        public void actionPerformed(ActionEvent e) {
            state = state.up();
            onStateChanged();
        }
    };

    private final Action DOWN_ACTION = new AbstractAction("down") {
        @Override
        public void actionPerformed(ActionEvent e) {
            state = state.down();
            onStateChanged();
        }
    };

    private final Action LEFT_ACTION = new AbstractAction("left") {
        @Override
        public void actionPerformed(ActionEvent e) {
            state = state.left();
        }
    };

    private final Action RIGHT_ACTION = new AbstractAction("right") {
        @Override
        public void actionPerformed(ActionEvent e) {
            state = state.right();
            onStateChanged();
        }
    };

    private final Action SETSCALE_ACTION = new AbstractAction("setscale") {
        @Override
        public void actionPerformed(ActionEvent e) {
            state = state.setScale(Double.parseDouble(p1p2distField.getText()));
            onStateChanged();
        }
    };

    private final Action SETRADIUS_ACTION = new AbstractAction("setr") {
        @Override
        public void actionPerformed(ActionEvent e) {
            String s = rField.getText();
            if (s.length() > 0) {
                r = Double.parseDouble(s);
            } else {
                r = 0.0;
            }
        }
    };

    private boolean copyToClipboard = false;

    private final Action COPY_ACTION = new AbstractAction("copy") {
        @Override
        public void actionPerformed(ActionEvent e) {
            copyToClipboard = true;
        }
    };

    private final Action CLEAR_ACTION = new AbstractAction("clear") {
        @Override
        public void actionPerformed(ActionEvent e) {
            currentImage = null;
        }
    };

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    private double distance(Dimension d, Point p) {
        return Math.sqrt(Math.pow(d.getWidth() - p.getX(), 2) + Math.pow(d.getHeight() - p.getY(), 2));
    }

    private double distance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }

    @Override
    public void mousePressed(MouseEvent e) {
        panel.requestFocusInWindow();
        double p1d = distance(toImageCoords(state.getPoint1()), e.getPoint());
        double p2d = distance(toImageCoords(state.getPoint2()), e.getPoint());
        double centerd = distance(toImageCoords(state.getCenter()), e.getPoint());
        if (!centerLock.isSelected() && centerd < p1d && centerd < p2d) {
            selection = Selection.CENTER;
        } else if (p1d < p2d) {
            selection = Selection.P1;
        } else {
            selection = Selection.P2;
        }
        state = selection.newState(state);
        // no need to fire onStateChanged
    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    private BufferedImage currentImage = null;


    private class CNCPainter implements WebcamPanel.Painter {
        private final int PMARKER_R = 10;

        private CNCPainter(WebcamPanel.Painter delegate) {
            this.delegate = delegate;
        }

        private final WebcamPanel.Painter delegate;

        @Override
        public void paintPanel(WebcamPanel panel, Graphics2D g2) {
            delegate.paintPanel(panel, g2);
        }


        @Override
        public void paintImage(WebcamPanel panel, BufferedImage image, Graphics2D g2) {
            image = deepCopy(image); // the old image will continue to be updated by the webcam, and may overwrite our annotations
            if (mergeCheckbox != null && mergeCheckbox.isSelected()) {
                if (currentImage != null) {
                    // combine the previous image on top of the new one
                    Raster oldRaster = currentImage.getRaster();
                    WritableRaster newRaster = image.getRaster();
                    int[] oldPixel = new int[4];
                    int[] newPixel = new int[4];
                    boolean max = minMax.isSelected();
                    for (int x = 0; x < image.getWidth(); ++x) {
                        for (int y = 0; y < image.getHeight(); ++y) {
                            oldRaster.getPixel(x, y, oldPixel);
                            newRaster.getPixel(x, y, newPixel);
                            for (int i = 0; i < 3; ++i) {
                                newPixel[i] = max ? Math.max(newPixel[i], oldPixel[i]) : Math.min(newPixel[i], oldPixel[i]);
                            }
                            newRaster.setPixel(x, y, newPixel);
                        }
                    }
                    image.setData(newRaster);
                }
                currentImage = deepCopy(image);
            }

            image = resizeImage(image); // we resize the image so super doesn't need to
            Graphics2D g = (Graphics2D) image.getGraphics();
            Dimension center = toImageCoords(state.getCenter());
            g.setColor(new Color(255, 0, 0, 128));
            g.drawLine((int) center.getWidth(), 0, (int) center.getWidth(), image.getHeight());
            g.drawLine(0, (int) center.getHeight(), image.getWidth(), (int) center.getHeight());
            if (selection == Selection.CENTER) {
                g.draw(new Ellipse2D.Double(center.getWidth() - 4, center.getHeight() - 4, 9, 9));
            }
            if (r > 0.0) {
                double rPixels = r / state.getScale() * zoomFactor;
                g.draw(new Ellipse2D.Double(center.getWidth() - rPixels + 0.5, center.getHeight() - rPixels + 0.5, rPixels * 2, rPixels * 2));
            }
            drawPoint(g, toImageCoords(state.getPoint1()), selection == Selection.P1);
            drawPoint(g, toImageCoords(state.getPoint2()), selection == Selection.P2);
            if (copyToClipboard) {
                writeImage(image);
//                TransferableImage trans = new TransferableImage(deepCopy(image));
//                Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
//                c.setContents(trans, null);
                copyToClipboard = false;
            }
            g2.drawImage(image, 0, 0, null);
            //delegate.paintImage(panel, image, g2);
        }

        private BufferedImage resizeImage(BufferedImage image) {
            int w = panel.getWidth();
            int h = panel.getHeight();

            if (image.getWidth() != w && image.getHeight() != h) {
                zoomFactor = ((double) w) / image.getWidth();
                BufferedImage resized = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
                Graphics2D gr = resized.createGraphics();
                gr.setComposite(AlphaComposite.Src);
                gr.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                gr.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                gr.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                gr.drawImage(image, 0, 0, w, h, null);
                gr.dispose();
                resized.flush();

                return resized;
            } else {
                zoomFactor = 1.0;
                return image;
            }
        }

        private BufferedImage deepCopy(BufferedImage bi) {
            ColorModel cm = bi.getColorModel();
            boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
            WritableRaster raster = bi.copyData(null);
            return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
        }

        private void drawPoint(Graphics2D g, Dimension d, boolean selected) {
            if (selected) {
                g.setStroke(new BasicStroke(2));
            }
            g.draw(new Ellipse2D.Double(d.getWidth() - PMARKER_R, d.getHeight() - PMARKER_R, PMARKER_R * 2, PMARKER_R * 2));
            if (selected) {
                g.setStroke(new BasicStroke(1));
            }
            g.draw(new Rectangle2D.Double(d.getWidth() - 0.5, d.getHeight() - 0.5, 1, 1));

        }

    }

    private void writeImage(BufferedImage image) {
        File f = getImageFile();
        try {
            ImageIO.write(image, "png", f);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File getImageFile() {
        int i = 1;
        File f;
        do {
            f = new File("cnccam-" + i++ + ".png");
        } while (f.exists());
        return f;
    }

    private Dimension toImageCoords(Dimension d) {
        return new Dimension(
                (int) ((d.getWidth() + webcam.getViewSize().getWidth() / 2) * zoomFactor),
                (int) ((d.getHeight() + webcam.getViewSize().getHeight() / 2) * zoomFactor));
    }

    private Dimension toStateCoords(Point d) {
        return new Dimension((int) ((d.getX() / zoomFactor) - webcam.getViewSize().getWidth() / 2), (int) ((d.getY() / zoomFactor) - webcam.getViewSize().getHeight() / 2));
    }


    @Override
    public void run() {

        setTitle("CNCCAM");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        addWindowListener(this);

        picker = new WebcamPicker();
        picker.addItemListener(this);

        webcam = picker.getSelectedWebcam();

        if (webcam == null) {
            System.out.println("No webcams found...");
            System.exit(1);
        }

        createPanel(webcam);
        toolbar = createToolbar();
        onStateChanged();
        add(picker, BorderLayout.NORTH);
        add(panel, BorderLayout.CENTER);
        add(toolbar, BorderLayout.SOUTH);
        panel.requestFocus();
    }

    private JToolBar createToolbar() {
        JToolBar t = new JToolBar();
        /*t.add(LEFT_ACTION);
        t.add(UP_ACTION);
        t.add(DOWN_ACTION);
        t.add(RIGHT_ACTION);*/
        centerLock = new JCheckBox("lock");
        centerLock.setSelected(true);
        t.add(centerLock);
        scaleLabel = new JLabel("");
        t.add(scaleLabel);
        t.add(new JLabel(" P1 -> P2"));
        p1p2distField = new JTextField("", 6);
        p1p2distField.setMaximumSize(new Dimension(60, 20));
        p1p2distField.addKeyListener(new KeyListener() {

            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == '\n') {
                    SETSCALE_ACTION.actionPerformed(null);
                }
            }

            @Override
            public void keyPressed(KeyEvent e) {

            }

            @Override
            public void keyReleased(KeyEvent e) {

            }
        });
        t.add(p1p2distField);
        t.add(new JLabel("mm "));
        //t.add(SETSCALE_ACTION);
        t.add(new JLabel(" r"));
        rField = new JTextField("", 3);
        rField.setMaximumSize(new Dimension(60, 20));
        rField.addKeyListener(new KeyListener() {

            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == '\n') {
                    SETRADIUS_ACTION.actionPerformed(null);
                }
            }

            @Override
            public void keyPressed(KeyEvent e) {

            }

            @Override
            public void keyReleased(KeyEvent e) {

            }
        });
        t.add(rField);
        t.add(new JLabel("mm "));
        //t.add(SETRADIUS_ACTION);
        t.add(COPY_ACTION);
        mergeCheckbox = new JCheckBox("merge");
        t.add(mergeCheckbox);
        t.add(CLEAR_ACTION);
        minMax = new JCheckBox("max");
        minMax.setSelected(false);
        t.add(minMax);

        return t;
    }

    private String scaleLabel() {
        return String.format(" %.3f mm/px ", state.getScale());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new CNCCAM());
    }

    @Override
    public void webcamOpen(WebcamEvent we) {
        System.out.println("webcam open");
    }

    @Override
    public void webcamClosed(WebcamEvent we) {
        System.out.println("webcam closed");
    }

    @Override
    public void webcamDisposed(WebcamEvent we) {
        System.out.println("webcam disposed");
    }

    @Override
    public void webcamImageObtained(WebcamEvent we) {
    }

    @Override
    public void windowActivated(WindowEvent e) {
    }

    @Override
    public void windowClosed(WindowEvent e) {
        webcam.close();
    }

    @Override
    public void windowClosing(WindowEvent e) {
    }

    @Override
    public void windowOpened(WindowEvent e) {
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
        System.out.println("webcam viewer resumed");
        panel.resume();
    }

    @Override
    public void windowIconified(WindowEvent e) {
        System.out.println("webcam viewer paused");
        panel.pause();
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        System.err.println(String.format("Exception in thread %s", t.getName()));
        e.printStackTrace();
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        if (e.getItem() != webcam) {
            if (webcam != null) {

                createPanel((Webcam) e.getItem());
                onStateChanged();
            }
        }
    }

    private void createPanel(Webcam newWebCam) {
        if (panel != null) {
            panel.stop();
            remove(panel);

            webcam.removeWebcamListener(this);
            webcam.close();
        }

        webcam = newWebCam;
        Dimension[] sizes = webcam.getViewSizes();
        Dimension largestSize = null;
        for (Dimension d : sizes) {
            if (largestSize == null || largestSize.getWidth() < d.getWidth()) {
                largestSize = d;
            }
        }
        webcam.setViewSize(largestSize);
        webcam.addWebcamListener(this);

        System.out.println("selected " + webcam.getName());

        panel = new WebcamPanel(webcam, false);
        panel.setPainter(new CNCPainter(panel.getDefaultPainter()));
        panel.setFocusable(true);
        panel.requestFocus();
        panel.addMouseListener(this);
        panel.addMouseMotionListener(this);
        panel.addComponentListener(new ComponentListener() {

            @Override
            public void componentResized(ComponentEvent e) {
                Dimension newSize = e.getComponent().getSize();
                double widthRatio = newSize.getWidth() / webcam.getViewSize().getWidth();
                double heightRatio = newSize.getHeight() / webcam.getViewSize().getHeight();

                double newRatio = Math.min(widthRatio, heightRatio);
                e.getComponent().setSize((int) (newRatio * webcam.getViewSize().getWidth()), (int) (newRatio * webcam.getViewSize().getHeight()));
            }

            @Override
            public void componentMoved(ComponentEvent e) {

            }

            @Override
            public void componentShown(ComponentEvent e) {

            }

            @Override
            public void componentHidden(ComponentEvent e) {

            }
        });
        InputMap inputMap = panel.getInputMap();
        ActionMap actionMap = panel.getActionMap();
        addAction(inputMap, actionMap, KeyStroke.getKeyStroke('w'), UP_ACTION);
        addAction(inputMap, actionMap, KeyStroke.getKeyStroke('s'), DOWN_ACTION);
        addAction(inputMap, actionMap, KeyStroke.getKeyStroke('a'), LEFT_ACTION);
        addAction(inputMap, actionMap, KeyStroke.getKeyStroke('d'), RIGHT_ACTION);
        addAction(inputMap, actionMap, KeyStroke.getKeyStroke(' '), CLEAR_ACTION);

        add(panel, BorderLayout.CENTER);
        pack();
        setVisible(true);
        Thread t = new Thread() {

            @Override
            public void run() {
                panel.start();
            }
        };
        t.setName("example-stopper");
        t.setDaemon(true);
        t.setUncaughtExceptionHandler(this);
        t.start();
    }

    private void addAction(InputMap inputMap, ActionMap actionMap, KeyStroke keyStroke, Action action) {
        inputMap.put(keyStroke, action.getValue(Action.NAME));
        actionMap.put(action.getValue(Action.NAME), action);
    }
}
