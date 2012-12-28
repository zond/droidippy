
package cx.ath.troja.droidippy;

import yuku.ambilwarna.*;

import android.preference.*;
import android.content.*;
import android.util.*;
import android.app.*;
import android.view.*;

import static cx.ath.troja.droidippy.Util.*;

public class ColorPreference extends Preference {

    private View view;
    private int color;

    protected void onBindView(View view) {
	super.onBindView(view);
	this.view = view;
	view.setBackgroundColor(color);
    }

    public ColorPreference(Context context, AttributeSet attr, int defStyle) {
	super(context, attr, defStyle);
	final String[] parts = getKey().split("_");
	color = getRGBColor(getContext(), parts[0]);
	setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
		public boolean onPreferenceClick(Preference preference) {
		    new AmbilWarnaDialog(getContext(), color, new AmbilWarnaDialog.OnAmbilWarnaListener() {
			    @Override
			    public void onOk(AmbilWarnaDialog dialog, int color) {
				ColorPreference.this.color = color;
				view.setBackgroundColor(color);
				setRGBColor(getContext(), parts[0], color);
			    }
			    @Override
			    public void onCancel(AmbilWarnaDialog dialog) {
			    }
			}).show();
		    return true;
		}
	    });
    }

    public ColorPreference(Context context, AttributeSet attr) {
	this(context, attr, 0);
    }


}