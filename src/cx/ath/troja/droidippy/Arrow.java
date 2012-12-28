package cx.ath.troja.droidippy;  

import android.graphics.*;
import android.graphics.drawable.*;
import android.util.*;

public class Arrow extends Drawable {

    private Poi start;
    private Poi middle;
    private Poi end;
    private int color;
    private boolean failed;

    public Arrow(Poi start, Poi middle, Poi end, int color) {
	if (start.equals(end)) {
	    this.start = start;
	    this.middle = start.add(middle.sub(start).div(2f));
	    this.end = middle;
	    this.color = color;
	} else {
	    this.start = start;
	    this.middle = middle;
	    this.end = end;
	    this.color = color;
	}
    }
    public Arrow(Poi start, Poi end, int color) {
	this(start, start.add(end.sub(start).div(2f)), end, color);
    }
    public Arrow failed(boolean failed) {
	this.failed = failed;
	return this;
    }
    public void draw(Canvas canvas) {
	if (!start.equals(end)) {
	    float scaleF = 0.002f;
	    float headF1 = 5f;
	    float headF2 = 10f;
	    float bounds = (float) Math.sqrt(Math.pow(getBounds().width(), 2) + Math.pow(getBounds().height(), 2));
	    float boundF = bounds * scaleF;
	    float spacer = 2 * boundF;
	    float boundFDiag = (float) Math.sqrt(Math.pow(boundF, 2) + Math.pow(boundF, 2));

	    Vec part1 = new Vec(start, middle);
	    Vec part2 = new Vec(middle, end);
	    Vec all = new Vec(start, end);

	    Poi start0 = start.add(part1.dir().mul(spacer)).add(part1.orth().mul(boundF));
	    Poi start1 = start.add(part1.dir().mul(spacer)).sub(part1.orth().mul(boundF));
	
	    Poi sumOrth = part1.orth().add(part2.orth());
	    Poi avgOrth = sumOrth.div(sumOrth.len());

	    Poi control0 = middle.add(avgOrth.mul(boundF));
	    Poi control1 = middle.sub(avgOrth.mul(boundF));

	    Poi end0 = end.sub(part2.dir().mul(spacer + headF2)).add(part2.orth().mul(boundF));
	    Poi end1 = end.sub(part2.dir().mul(spacer + headF2)).sub(part2.orth().mul(boundF));
	    Poi end3 = end.sub(part2.dir().mul(spacer));

	    Poi head0 = end0.add(part2.orth().mul(headF1));
	    Poi head1 = end1.sub(part2.orth().mul(headF1));

	    Path outline = new Path();
	    outline.moveTo(start0.x, start0.y);
	    outline.cubicTo(control0.x, control0.y, control0.x, control0.y, end0.x, end0.y);
	    outline.lineTo(head0.x, head0.y);
	    outline.lineTo(end3.x, end3.y);
	    outline.lineTo(head1.x, head1.y);
	    outline.lineTo(end1.x, end1.y);
	    outline.cubicTo(control1.x, control1.y, control1.x, control1.y, start1.x, start1.y);
	    outline.close();

	    float[] hsv = new float[3];
	    Color.colorToHSV(color, hsv);
	    hsv[2] *= 1.5;
	    hsv[1] *= 1.5;

	    Paint paint = new Paint();
	    paint.setAntiAlias(true);
	    paint.setStyle(Paint.Style.FILL);
	    paint.setColor(Color.HSVToColor(hsv));
	    paint.setAlpha((int) (255 * 0.75));
	    canvas.drawPath(outline, paint);
	
	    Paint paint2 = new Paint();
	    paint2.setAntiAlias(true);
	    paint2.setStyle(Paint.Style.STROKE);
	    if (failed) {
		paint2.setStrokeWidth(1 * boundF);
		paint2.setColor(Color.RED);
		paint2.setAlpha(255);
	    } else {
		hsv[2] *= 0.5;
		paint2.setColor(Color.HSVToColor(hsv));
		paint2.setAlpha((int) (255 * 0.75));
	    }
	    canvas.drawPath(outline, paint2);
	}
    }
    public int getOpacity() {
      return PixelFormat.OPAQUE;
    }
    public void setAlpha(int alpha) {
    }
    public void setColorFilter(ColorFilter cf) {
    }
  }
