
package cx.ath.troja.droidippy;

import java.util.*;
import java.math.*;
import android.content.*;
import android.util.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.graphics.drawable.shapes.*;

public class Unit {

    public static Drawable createFleet(Context context, float x, float y, int color, boolean dislodged, float viewPortWidth, float viewPortHeight, int outlineColor) {
	x -= 28;
	y -= 10;
	if (dislodged) {
	    x += 5;
	    y += 5;
	}

	float[] hsv = new float[3];
	Color.colorToHSV(color, hsv);
	
	Map<String, Object> fleetConfig = new HashMap<String, Object>();
	if (hsv[2] > 0.9) {
	    hsv[2] *= 0.8;
	} else {
	    hsv[2] *= 1.4;
	}
	hsv[1] *= 0.9;
	color = Color.HSVToColor(hsv);
	if (dislodged) {
	    color |= 0xbb000000;
	    color &= 0xbbffffff;
	} else {
	    color |= 0xff000000;
	} 
	fleetConfig.put("hullFill", color);
	fleetConfig.put("hullStroke", outlineColor);
	return Fleet.create(context, new float[] { x, y }, fleetConfig, (int) viewPortWidth, (int) viewPortHeight);
    }

    public static Drawable createArmy(Context context, float x, float y, int color, boolean dislodged, float viewPortWidth, float viewPortHeight, int outlineColor) {
	x -= 17;
	y -= 12;
	if (dislodged) {
	    x += 5;
	    y += 5;
	}

	float[] hsv = new float[3];
	Color.colorToHSV(color, hsv);

	Map<String, Object> armyConfig = new HashMap<String, Object>();
	if (hsv[2] > 0.9) {
	    hsv[2] *= 0.8;
	} else {
	    hsv[2] *= 1.4;
	}
	hsv[1] *= 0.9;
	color = Color.HSVToColor(hsv);
	if (dislodged) {
	    color |= 0xbb000000;
	    color &= 0xbbffffff;
	} else {
	    color |= 0xff000000;
	} 
	armyConfig.put("bodyFill", color);
	armyConfig.put("bodyStroke", outlineColor);
	return Army.create(context, new float[] { x, y }, armyConfig, (int) viewPortWidth, (int) viewPortHeight);
    }    

}
