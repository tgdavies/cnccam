package org.kablambda.cnccam;


import java.awt.*;

public class CNCCAMState {
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
}
