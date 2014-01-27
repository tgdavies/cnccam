package org.kablambda.cnccam;


import java.awt.*;
import java.io.*;
import java.util.prefs.Preferences;

public class CNCCAMState implements Serializable {
    // the point the user has set as the pixel location that represents the center of rotation of their spindle
    protected final Dimension center;

    protected final Dimension point1;

    protected final Dimension point2;

    // the scale in mm per pixel
    protected final double scale;

    public CNCCAMState(Dimension center, double scale, Dimension point1, Dimension point2) {
        this.center = center;
        this.scale = scale;
        this.point1 = point1;
        this.point2 = point2;
    }

    public CNCCAMState(CNCCAMState state) {
        this(state.center, state.scale, state.point1, state.point2);
    }

    public Dimension getCenter() {
        return center;
    }

    public double getScale() {
        return scale;
    }

    public Dimension getPoint1() {
        return point1;
    }

    public Dimension getPoint2() {
        return point2;
    }

    public CNCCAMState up() {
        return new CNCCAMState(this);
    }

    public CNCCAMState down() {
        return new CNCCAMState(this);
    }

    public CNCCAMState left() {
        return new CNCCAMState(this);
    }

    public CNCCAMState right() {
        return new CNCCAMState(this);
    }

    public CNCCAMState movePointTo(Dimension d) {
        return this;
    }

    public CNCCAMState setScale(double dp1p2d) {
        return new CNCCAMState(getCenter(), dp1p2d / getP1P2Distance(), getPoint1(), getPoint2());
    }

    public double getP1P2Distance() {
        return distance(getPoint1().getWidth(), getPoint1().getHeight(), getPoint2().getWidth(), getPoint2().getHeight());
    }

    private double distance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }


    protected Dimension up(Dimension d) {
        return new Dimension(d.width, d.height - 1);
    }

    protected Dimension down(Dimension d) {
        return new Dimension(d.width, d.height + 1);
    }

    protected Dimension left(Dimension d) {
        return new Dimension(d.width - 1, d.height);
    }

    protected Dimension right(Dimension d) {
        return new Dimension(d.width + 1, d.height);
    }

    public static void writeToPrefs(CNCCAMState state) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(state);
            oos.close();

            Preferences.userNodeForPackage(CNCCAMState.class).putByteArray("state", baos.toByteArray());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static CNCCAMState readFromPrefs() {
        byte[] bytes = Preferences.userNodeForPackage(CNCCAMState.class).getByteArray("state", null);
        if (bytes == null) {
            return getDefaultState();
        } else {
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            try {
                ObjectInputStream ois = new ObjectInputStream(bais);
                return (CNCCAMState) ois.readObject();
            } catch (Exception e) {
                return getDefaultState();
            }
        }
    }

    private static CNCCAMState getDefaultState() {
        return new CNCCAMState(new Dimension(0, 0), 1.0, new Dimension(-30, -30), new Dimension(30, 30));
    }
}
