package cx.ath.troja.droidippy;

import android.graphics.*;

public class Poi extends PointF {
    public Poi(float[] c) {
	super(c[0], c[1]);
    }
    public Poi(float x, float y) {
	super(x,y);
    }
    public Poi(PointF p) {
	super(p.x, p.y);
    }
    public boolean equals(Object o) {
	if (o instanceof Poi) {
	    Poi other = (Poi) o;
	    return x == other.x && y == other.y;
	} else {
	    return false;
	}
    }
    public String toString() {
	return getClass().getName() + "x=" + x + " y=" + y;
    }
    public Poi add(Poi p) {
	return new Poi(x + p.x,
		       y + p.y);
    }
    public Poi sub(Poi p) {
	return new Poi(x - p.x,
		       y - p.y);
    }
    public float len() {
	return (float) Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
    }
    public Poi div(float f) {
	return new Poi(x / f,
		       y / f);
    }
    public Poi mul(float f) {
	return new Poi(x * f,
		       y * f);
    }
    public Poi orth() {
	return new Poi(-y, x);
    }
}

