
package cx.ath.troja.droidippy;

import android.preference.*;
import android.os.*;
import android.util.*;
import android.content.*;
import android.text.*;
import android.widget.*;

import static cx.ath.troja.droidippy.Util.*;

public class DippyPrefs extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
	addPreferencesFromResource(R.xml.preferences);
	final String revision = packageInfo(this).versionName + "/" + LocalOptions.MTN_REVISION;
	findPreference("revision").setSummary(revision);
	findPreference("revision").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
		public boolean onPreferenceClick(Preference preference) {
		    ((android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE)).setText(revision);
		    Toast.makeText(getApplicationContext(), R.string.version_code_copied, Toast.LENGTH_LONG).show();
		    finish();
		    return true;
		}
	    });
	findPreference("clear_widget_notifications").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
		public boolean onPreferenceClick(Preference preference) {
		    WidgetProvider.clearNotifications(DippyPrefs.this);
		    return true;
		}
	    });
	findPreference("notifications").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
		public boolean onPreferenceChange(Preference preference, Object newValue) {
		    if (Boolean.TRUE.equals(newValue) && getWidgetIds(DippyPrefs.this).length == 0) {
			GCMIntentService.registerGCM(DippyPrefs.this);
		    } else if (getWidgetIds(DippyPrefs.this).length == 0) {
			GCMIntentService.unregisterGCM(DippyPrefs.this);
		    }
		    return true;
		}
	    });
	findPreference("cx.ath.troja.droidippy.Order.order_help").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
		public boolean onPreferenceChange(Preference preference, Object newValue) {
		    if (Boolean.TRUE.equals(newValue)) {
			for (Class klass : Order.SUBCLASSES) {
			    setOrderHelp(DippyPrefs.this, klass, true);
			}
		    } else {
			for (Class klass : Order.SUBCLASSES) {
			    setOrderHelp(DippyPrefs.this, klass, false);
			}
		    }
		    return true;
		}
	    });
    }

}