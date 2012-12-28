
package cx.ath.troja.droidippy;

import android.appwidget.*;
import android.content.*;
import android.util.*;
import android.app.*;
import android.widget.*;
import android.view.*;
import android.net.*;
import android.database.sqlite.*;
import android.database.*;

import static cx.ath.troja.droidippy.Util.*;

public class WidgetProvider extends AppWidgetProvider {
    
    public static class Notifications extends SQLiteOpenHelper {

	Notifications(Context context) {
	    super(context, "notifications", null, 1);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
	    db.execSQL("CREATE TABLE NOTIFICATIONS (ID INTEGER PRIMARY KEY AUTOINCREMENT, GAME_ID TEXT, CHANNEL TEXT);");
	    db.execSQL("CREATE INDEX NOTIFICATIONS_GAME_ID ON NOTIFICATIONS(GAME_ID);");
	    db.execSQL("CREATE INDEX NOTIFICATIONS_CHANNEL ON NOTIFICATIONS(CHANNEL);");
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int v1, int v2) {
	    
	}
    }
    
    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
	removeWidgetIds(context, appWidgetIds);
	if (!androidNotifications(context)) {
	    GCMIntentService.unregisterGCM(context);
	}
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
	int[] currentWids = getWidgetIds(context);
        for (int i = 0; i < appWidgetIds.length; i++) {
            int appWidgetId = appWidgetIds[i];

	    if (!addWidgetId(context, appWidgetId)) {
		if (!androidNotifications(context)) {
		    GCMIntentService.registerGCM(context);
		}
	    }

            Intent intent = new Intent(context, Droidippy.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.appwidget);
            views.setOnClickPendingIntent(R.id.appwidget_icon, pendingIntent);

	    long newMessages = newMessages(context);
	    if (newMessages > 0) {
		views.setTextViewText(R.id.appwidget_messages, "" + newMessages);
		views.setViewVisibility(R.id.appwidget_message, View.VISIBLE);
	    } else {
		views.setViewVisibility(R.id.appwidget_message, View.GONE);
	    }

	    long newPhases = newPhases(context);
	    if (newPhases > 0) {
		views.setTextViewText(R.id.appwidget_phases, "" + newPhases);
		views.setViewVisibility(R.id.appwidget_phase, View.VISIBLE);
	    } else {
		views.setViewVisibility(R.id.appwidget_phase, View.GONE);
	    }

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    public static void clearNotifications(Context context) {
	SQLiteDatabase db = new Notifications(context).getWritableDatabase();
	db.delete("NOTIFICATIONS", null, new String[0]);
	db.close();
	context.sendBroadcast(getWidgetUpdateIntent(context));
    }

    public static void clearMessages(Context context, String gameId, String channel) {
	SQLiteDatabase db = new Notifications(context).getWritableDatabase();
	db.delete("NOTIFICATIONS", "GAME_ID = ? AND CHANNEL = ?", new String[] { gameId, channel });
	db.close();
	context.sendBroadcast(getWidgetUpdateIntent(context));
    }

    public static void clearPhases(Context context, String gameId) {
	SQLiteDatabase db = new Notifications(context).getWritableDatabase();
	db.delete("NOTIFICATIONS", "GAME_ID = ? AND CHANNEL IS NULL", new String[] { gameId });
	db.close();
	context.sendBroadcast(getWidgetUpdateIntent(context));
    }

    private long newMessages(Context context) {
	SQLiteDatabase db = new Notifications(context).getReadableDatabase();
	Cursor c = db.rawQuery("SELECT COUNT(ID) FROM NOTIFICATIONS WHERE CHANNEL IS NOT NULL", new String[0]);
	c.moveToFirst();
	long returnValue = c.getLong(0);
	c.close();
	db.close();
	return returnValue;
    }

    private long newPhases(Context context) {
	SQLiteDatabase db = new Notifications(context).getReadableDatabase();
	Cursor c = db.rawQuery("SELECT COUNT(ID) FROM NOTIFICATIONS WHERE CHANNEL IS NULL", new String[0]);
	c.moveToFirst();
	long returnValue = c.getLong(0);
	c.close();
	db.close();
	return returnValue;
    }

    public static void onMessage(Context context, String gameId, String channel) {
	SQLiteDatabase db = new Notifications(context).getWritableDatabase();
	ContentValues values = new ContentValues();
	values.put("GAME_ID", gameId);
	values.put("CHANNEL", channel);
	db.insertOrThrow("NOTIFICATIONS", null, values);
	db.close();
	context.sendBroadcast(getWidgetUpdateIntent(context));
    }

    public static void onNewPhase(Context context, String gameId) {
	SQLiteDatabase db = new Notifications(context).getWritableDatabase();
	ContentValues values = new ContentValues();
	values.put("GAME_ID", gameId);
	db.insertOrThrow("NOTIFICATIONS", "CHANNEL", values);
	db.close();
	context.sendBroadcast(getWidgetUpdateIntent(context));
    }

    protected static Intent getWidgetUpdateIntent(Context context) {
	Intent widgetUpdate = new Intent();
	widgetUpdate.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
	widgetUpdate.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, getWidgetIds(context));
	widgetUpdate.setData(Uri.parse("droidippy_widget://update"));
	return widgetUpdate;
    }

}
