package cx.ath.troja.droidippy;

import android.graphics.*;

public class Vec {
    public Poi p1;
    public Poi p2;
    public Vec(Poi p1, Poi p2) {
	this.p1 = p1;
	this.p2 = p2;
    }
    public float len() {
	return p2.sub(p1).len();
    }
    public Poi dir() {
	return p2.sub(p1).div(len());
    }
    public Poi orth() {
	return dir().orth();
    }
}

