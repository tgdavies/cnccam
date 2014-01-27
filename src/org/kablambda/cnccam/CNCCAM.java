package org.kablambda.cnccam;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.lang.Thread.UncaughtExceptionHandler;

import javax.swing.*;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamEvent;
import com.github.sarxos.webcam.WebcamListener;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamPicker;
import com.github.sarxos.webcam.WebcamResolution;


public class CNCCAM extends JFrame implements Runnable, WebcamListener, WindowListener, UncaughtExceptionHandler, ItemListener, MouseListener, MouseMotionListener {

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

    @Override
    public void mouseDragged(MouseEvent e) {
        state = state.movePointTo(toStateCoords(e.getPoint()));
        onStateChanged();
    }

    private void onStateChanged() {
        p1p2distField.setText(String.format("%.3f", state.getP1P2Distance() * state.getScale()));
        scaleLabel.setText(scaleLabel());
        CNCCAMState.writeToPrefs(state);
    }

    @Override
    public void mouseMoved(MouseEvent e) {

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
        if (centerd < p1d && centerd < p2d) {
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


    private class CNCPainter implements WebcamPanel.Painter {

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
            Graphics2D g = (Graphics2D) image.getGraphics();
            Dimension center = toImageCoords(state.getCenter());
            g.setColor(new Color(255, 255, 255, 64));
            g.drawLine((int) center.getWidth(), 0, (int) center.getWidth(), image.getHeight());
            g.drawLine(0, (int) center.getHeight(), image.getWidth(), (int) center.getHeight());
            if (selection == Selection.CENTER) {
                g.draw(new Ellipse2D.Double(center.getWidth() - 4, center.getHeight() - 4, 9, 9));
            }
            if (r > 0.0) {
                double rPixels = r / state.getScale();
                g.draw(new Ellipse2D.Double(center.getWidth() - rPixels/2 + 0.5, center.getHeight() - rPixels/2 + 0.5, rPixels, rPixels));
            }
            drawPoint(g, toImageCoords(state.getPoint1()), selection == Selection.P1);
            drawPoint(g, toImageCoords(state.getPoint2()), selection == Selection.P2);
            delegate.paintImage(panel, image, g2);
        }

        private void drawPoint(Graphics2D g, Dimension d, boolean selected) {
            if (selected) {
                g.draw(new Ellipse2D.Double(d.getWidth() - 4.5, d.getHeight() - 4.5, 9, 9));
            }
            g.draw(new Rectangle2D.Double(d.getWidth() - 0.5, d.getHeight() - 0.5, 1, 1));
        }

    }

    private Dimension toImageCoords(Dimension d) {
        return new Dimension((int) (d.getWidth() + webcam.getViewSize().getWidth() / 2), (int) (d.getHeight() + webcam.getViewSize().getHeight() / 2));
    }

    private Dimension toStateCoords(Point d) {
        return new Dimension((int) (d.getX() - webcam.getViewSize().getWidth() / 2), (int) (d.getY() - webcam.getViewSize().getHeight() / 2));
    }


    @Override
    public void run() {

        setTitle("Java Webcam Capture POC");
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
        scaleLabel = new JLabel("");
        t.add(scaleLabel);
        t.add(new JLabel(" P1 -> P2"));
        p1p2distField = new JTextField("", 6);
        p1p2distField.setMaximumSize(new Dimension(60, 20));
        t.add(p1p2distField);
        t.add(new JLabel("mm "));
        t.add(SETSCALE_ACTION);
        t.add(new JLabel(" r"));
        rField = new JTextField("",3);
        rField.setMaximumSize(new Dimension(30, 20));
        t.add(rField);
        t.add(new JLabel("mm "));
        t.add(SETRADIUS_ACTION);
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
        webcam.setViewSize(WebcamResolution.VGA.getSize());
        webcam.addWebcamListener(this);

        System.out.println("selected " + webcam.getName());

        panel = new WebcamPanel(webcam, false);
        panel.setPainter(new CNCPainter(panel.getDefaultPainter()));
        panel.setFocusable(true);
        panel.requestFocus();
        panel.addMouseListener(this);
        panel.addMouseMotionListener(this);
        InputMap inputMap = panel.getInputMap();
        ActionMap actionMap = panel.getActionMap();
        addAction(inputMap, actionMap, KeyStroke.getKeyStroke('w'), UP_ACTION);
        addAction(inputMap, actionMap, KeyStroke.getKeyStroke('s'), DOWN_ACTION);
        addAction(inputMap, actionMap, KeyStroke.getKeyStroke('a'), LEFT_ACTION);
        addAction(inputMap, actionMap, KeyStroke.getKeyStroke('d'), RIGHT_ACTION);

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
