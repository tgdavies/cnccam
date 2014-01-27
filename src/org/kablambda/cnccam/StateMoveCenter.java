package org.kablambda.cnccam;

import java.awt.*;

/**
 * Created by tomd on 26/01/2014.
 */
public class StateMoveCenter extends CNCCAMState {
    public StateMoveCenter(CNCCAMState state) {
        super(state);
    }

    public StateMoveCenter(Dimension center, double scale, Dimension point1, Dimension point2) {
        super(center, scale, point1, point2);
    }

    public CNCCAMState up() {
        return new StateMoveCenter(up(center), scale, point1, point2);
    }

    public CNCCAMState down() {
        return new StateMoveCenter(down(center), scale, point1, point2);
    }

    public CNCCAMState left() {
        return new StateMoveCenter(left(center), scale, point1, point2);
    }

    public CNCCAMState right() {
        return new StateMoveCenter(right(center), scale, point1, point2);
    }

    public CNCCAMState movePointTo(Dimension d) {
        return new StateMoveCenter(new Dimension((int)d.getWidth(), (int)d.getHeight()), getScale(), getPoint1(), getPoint2());
    }
}
