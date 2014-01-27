package org.kablambda.cnccam;

import java.awt.*;

/**
 * Created by tomd on 26/01/2014.
 */
public class StateMoveP2 extends CNCCAMState {
    public StateMoveP2(CNCCAMState state) {
        super(state);
    }

    public StateMoveP2(Dimension center, double scale, Dimension point1, Dimension point2) {
        super(center, scale, point1, point2);
    }

    public CNCCAMState up() {
        return new StateMoveP2(center, scale, point1, up(point2));
    }

    public CNCCAMState down() {
        return new StateMoveP2(center, scale, point1, down(point2));
    }

    public CNCCAMState left() {
        return new StateMoveP2(center, scale, point1, left(point2));
    }

    public CNCCAMState right() {
        return new StateMoveP2(center, scale, point1, right(point2));
    }

    public CNCCAMState movePointTo(Dimension d) {
        return new StateMoveP2(getCenter(), getScale(), getPoint1(), new Dimension((int)d.getWidth(), (int)d.getHeight()));
    }

    public CNCCAMState setScale(double dp1p2d) {
        return new StateMoveP2(getCenter(), dp1p2d / getP1P2Distance(), getPoint1(), getPoint2());
    }

}
