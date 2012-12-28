package cx.ath.troja.droidippy;  

import android.graphics.*;
import android.graphics.drawable.*;
import android.util.*;

public class Box extends Drawable {
    private float[] loc;
    private int color;
    private int corners;
    public Box(float[] loc, int corners, int color) {
	this.loc = loc;
	this.corners = corners;
	this.color = color;
    }
    public void draw(Canvas canvas) {
	float scaleF = 0.013f;
	float bounds = (float) Math.sqrt(Math.pow(getBounds().width(), 2) + Math.pow(getBounds().height(), 2));
	float boundF = bounds * scaleF * 1.30f;

	float all = (float) (2f * Math.PI);
	float step = all / corners;

	float startAngle = (float) (Math.PI * 1.5f);
	if (corners % 2 == 0) 
	    startAngle += step / 2;

	float angle = startAngle;
	Path path = new Path();
	path.moveTo((float) (loc[0] + Math.cos(angle) * boundF), (float) (loc[1] + Math.sin(angle) * boundF));
	for (int i = 1; i < corners; i++) {
	    angle += step;
	    path.lineTo((float) (loc[0] + Math.cos(angle) * boundF), (float) (loc[1] + Math.sin(angle) * boundF));
	}
	path.close();

	boundF *= 0.7f;
	angle = startAngle;
	path.moveTo((float) (loc[0] + Math.cos(angle) * boundF), (float) (loc[1] + Math.sin(angle) * boundF));
	for (int i = 1; i < corners; i++) {
	    angle += step;
	    path.lineTo((float) (loc[0] + Math.cos(angle) * boundF), (float) (loc[1] + Math.sin(angle) * boundF));
	}
	path.close();
	path.setFillType(Path.FillType.EVEN_ODD);

	float[] hsv = new float[3];
	Color.colorToHSV(color, hsv);
	hsv[2] *= 1.5;
	hsv[1] *= 1.5;

	Paint paint = new Paint();
	paint.setAntiAlias(true);	
	paint.setStyle(Paint.Style.FILL);
	paint.setColor(Color.HSVToColor(hsv));
	paint.setStrokeWidth(2);
	paint.setAlpha((int) (255 * 0.75));
	canvas.drawPath(path, paint);

	Paint paint2 = new Paint();
	paint2.setAntiAlias(true);
	paint2.setStyle(Paint.Style.STROKE);
	hsv[2] *= 0.5;
	paint2.setColor(Color.HSVToColor(hsv));
	paint2.setAlpha((int) (255 * 0.75));
	canvas.drawPath(path, paint2);
    }
    public int getOpacity() {
      return PixelFormat.OPAQUE;
    }
    public void setAlpha(int alpha) {
    }
    public void setColorFilter(ColorFilter cf) {
    }
  }
