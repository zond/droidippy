package cx.ath.troja.droidippy;  

import android.graphics.*;
import android.graphics.drawable.*;
import android.util.*;

public class Cross extends Drawable {
    private float[] loc;
    private int color;
    public Cross(float[] loc, int color) {
	this.loc = loc;
	this.color = color;
    }
    public void draw(Canvas canvas) {
	float scaleF = 0.002f;
	float bounds = (float) Math.sqrt(Math.pow(getBounds().width(), 2) + Math.pow(getBounds().height(), 2));
	float boundF = bounds * scaleF;
	Paint paint = new Paint();
	paint.setAntiAlias(true);
	paint.setStyle(Paint.Style.STROKE);
	paint.setColor(color);
	paint.setStrokeWidth(4);
	canvas.drawLine(loc[0] - boundF * 4, loc[1] - boundF * 4, loc[0] + boundF * 4, loc[1] + boundF * 4, paint);
	canvas.drawLine(loc[0] - boundF * 4, loc[1] + boundF * 4, loc[0] + boundF * 4, loc[1] - boundF * 4, paint);
    }
    public int getOpacity() {
      return PixelFormat.OPAQUE;
    }
    public void setAlpha(int alpha) {
    }
    public void setColorFilter(ColorFilter cf) {
    }
  }
