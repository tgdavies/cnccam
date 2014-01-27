package org.kablambda.cnccam;

import java.awt.*;

/**
 * Created by tomd on 26/01/2014.
 */
public class StateMoveP1 extends CNCCAMState {
    public StateMoveP1(CNCCAMState state) {
        super(state);
    }

    public StateMoveP1(Dimension center, double scale, Dimension point1, Dimension point2) {
        super(center, scale, point1, point2);
    }

    public CNCCAMState up() {
        return new StateMoveP1(center, scale, up(point1), point2);
    }

    public CNCCAMState down() {
        return new StateMoveP1(center, scale, down(point1), point2);
    }

    public CNCCAMState left() {
        return new StateMoveP1(center, scale, left(point1), point2);
    }

    public CNCCAMState right() {
        return new StateMoveP1(center, scale, right(point1), point2);
    }

    public CNCCAMState movePointTo(Dimension d) {
        return new StateMoveP1(getCenter(), getScale(), new Dimension((int)d.getWidth(), (int)d.getHeight()), getPoint2());
    }

    public CNCCAMState setScale(double dp1p2d) {
        return new StateMoveP1(getCenter(), dp1p2d / getP1P2Distance(), getPoint1(), getPoint2());
    }
}
