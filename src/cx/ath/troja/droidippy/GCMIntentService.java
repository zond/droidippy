

package cx.ath.troja.droidippy;

import android.content.*;
import android.util.*;
import android.os.*;
import android.accounts.*;
import android.app.*;
import android.net.*;
import android.widget.*;
import android.provider.*;

import java.util.*;
import java.util.concurrent.*;
import java.math.*;
import java.text.*;

import com.google.android.gcm.*;

import static cx.ath.troja.droidippy.Util.*;

public class GCMIntentService extends GCMBaseIntentService {
    
    private static final String SENDER_ID = "212006352346";

    private static final String FROM = "from";

    private static final String ERROR_SERVICE_NOT_AVAILABLE = "SERVICE_NOT_AVAILABLE";
    private static final String ERROR_ACCOUNT_MISSING = "ACCOUNT_MISSING";
    private static final String ERROR_AUTHENTICATION_FAILED = "AUTHENTICATION_FAILED";
    private static final String ERROR_INVALID_SENDER = "INVALID_SENDER";
    private static final String ERROR_INVALID_PARAMETERS = "INVALID_PARAMETERS";
    private static final String ERROR_PHONE_REGISTRATION_ERROR = "PHONE_REGISTRATION_ERROR";

    public static final int MESSAGE_ID = 0;
    public static final int PHASE_ID = 1;
    public static final int INVITATION_ID = 2;

    public static final Map<ViewGame, Object> viewGames = new ConcurrentHashMap<ViewGame, Object>();
    public static final Map<Droidippy, Object> gameLists = new ConcurrentHashMap<Droidippy, Object>();
    
    public GCMIntentService() {
        super(SENDER_ID);
    }

    public void onRegistered(Context context, String regId) {
        if (hasToken(context)) {
            new Getter(context, MessageFormat.format(SUBSCRIBE_URL_FORMAT, regId, Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID))).onError(new Doable<RuntimeException>() {
                    public void doit(RuntimeException e) {
                        Log.w(PACKAGE_NAME, "While trying to subscribe", e);
                    }
            }).start();
        } else {
            Log.w(PACKAGE_NAME, "Unable to subscribe regId " + regId + " since I have no dippy token.");
        }
    }

    public void onUnregistered(Context context, String regId) {
        if (hasToken(context)) {
            new Getter(context, MessageFormat.format(UNSUBSCRIBE_URL_FORMAT, Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID))).onError(new Doable<RuntimeException>() {
                    public void doit(RuntimeException e) {
                        Log.w(PACKAGE_NAME, "While trying to unsubscribe", e);
                    }
            }).start();
        } else {
            Log.w(PACKAGE_NAME, "Unable to unsubscribe regId " + regId + " since I have no dippy token.");
        }
    }

    public void onError(Context context, String error) {
        if (error.equals(ERROR_SERVICE_NOT_AVAILABLE)) {
            Log.w(PACKAGE_NAME, "Got " + ERROR_SERVICE_NOT_AVAILABLE + " when registering to C2DM.");
        } else if (error.equals(ERROR_ACCOUNT_MISSING)) {
            Toast.makeText(context, R.string.error_account_missing, Toast.LENGTH_LONG);
        } else if (error.equals(ERROR_AUTHENTICATION_FAILED)) {
            Toast.makeText(context, R.string.error_authentication_failed, Toast.LENGTH_LONG);
        } else if (error.equals(ERROR_INVALID_SENDER)) {
            Log.w(PACKAGE_NAME, "Durr, the sender is invalid.");
        } else if (error.equals(ERROR_PHONE_REGISTRATION_ERROR)) {
            Toast.makeText(context, R.string.error_phone_registration_error, Toast.LENGTH_LONG);
        } else if (error.equals(ERROR_INVALID_PARAMETERS)) {
            Toast.makeText(context, R.string.error_phone_registration_error, Toast.LENGTH_LONG);
        } else {
            throw new RuntimeException("unknown error " + error);
        }
    }

    public static final String GAME = "app.models.Game";
    public static final String GAME_MEMBER = "app.models.GameMember";
    public static final String GAME_PHASE = "app.models.GamePhase";
    public static final String CHAT_MESSAGE = "app.models.ChatMessage";

    public void onMessage(Context context, Intent intent) {
	Bundle extras = intent.getExtras();
	if (SENDER_ID.equals(extras.getString(FROM))) {
	    HashMap<String, String> message = new HashMap<String, String>();
	    for (String key : extras.keySet()) {
                message.put(key, extras.getString(key));
	    }
            Log.w(PACKAGE_NAME, "got " + message);
	    if (GAME_MEMBER.equals(message.get(TYPE))) {
		for (Droidippy activity : gameLists.keySet()) {
		    activity.deliver(message);
		}
	    } else if (GAME.equals(message.get(TYPE))) {
		String noteMessage = context.getResources().getString(R.string.you_are_invited);
		Notification notification = getNotification(context,
							    R.drawable.icon,
							    noteMessage,
							    System.currentTimeMillis(),
							    null);
		Intent viewInvitationIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("code:" + message.get(INVITATION_CODE)), context, ViewInvitation.class);
		viewInvitationIntent.putExtra(INVITER, message.get(INVITER));
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, viewInvitationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		notification.setLatestEventInfo(context, noteMessage, MessageFormat.format(context.getResources().getString(R.string.invites_you_to_a_game), message.get(INVITER)), pendingIntent);
		((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).notify("" + message.get(INVITATION_CODE), INVITATION_ID, notification);
	    } else if (GAME_PHASE.equals(message.get(TYPE))) {
		boolean delivered = false;
		for (ViewGame activity : viewGames.keySet()) {
		    try {
			if (activity.deliver(message)) {
			    delivered = true;
			}
		    } catch (Exception e) {
			Log.w(PACKAGE_NAME, "When trying to deliver " + message + " to " + activity, e);
		    }
		}
		if (!delivered) {
		    for (Droidippy activity : gameLists.keySet()) {
			try {
			    if (activity.deliver(message)) {
				delivered = true;
			    }
			} catch (Exception e) {
			    Log.w(PACKAGE_NAME, "When trying to deliver " + message + " to " + activity, e);
			}
		    }
		}
		if (!delivered) {
		    WidgetProvider.onNewPhase(context, message.get(GAME_ID));
		    if (androidNotifications(context)) {
			String noteMessage = context.getResources().getString(FINISHED.equals(message.get(GAME_STATE)) ? R.string.game_is_finished : R.string.new_phase);
			Notification notification = getNotification(context, 
								    R.drawable.icon,
								    noteMessage,
								    System.currentTimeMillis(),
								    null);
			Intent viewGameIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("game:" + message.get(GAME_ID)), context, ViewGame.class);		    
			PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, viewGameIntent, PendingIntent.FLAG_UPDATE_CURRENT);
			String text = "" + message.get(PHASE_NAME);
			if (FINISHED.equals(message.get(GAME_STATE))) {
			    text = MessageFormat.format(context.getResources().getString(R.string.is_finished), text);
			}
			notification.setLatestEventInfo(context, noteMessage, text, pendingIntent);
			((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).notify("" + message.get(GAME_ID), PHASE_ID, notification);
		    }
		}
	    } else if (CHAT_MESSAGE.equals(message.get(TYPE))) {
		boolean delivered = false;
		for (ViewGame activity : viewGames.keySet()) {
		    try {
			if (activity.deliver(message)) {
			    delivered = true;
			}
		    } catch (Exception e) {
			Log.w(PACKAGE_NAME, "When trying to deliver " + message + " to " + activity, e);
		    }
		}
		if (!delivered) {
		    for (Droidippy activity : gameLists.keySet()) {
			try {
			    if (activity.deliver(message)) {
				delivered = true;
			    }
			} catch (Exception e) {
			    Log.w(PACKAGE_NAME, "When trying to deliver " + message + " to " + activity, e);
			}
		    }
		}
		if (!delivered) {
		    WidgetProvider.onMessage(context, message.get(GAME_ID), message.get(CHANNEL));
		    if (androidNotifications(context)) {
			String noteMessage = null;
			if (message.get(GAME_ALIAS) != null) {
			    noteMessage = MessageFormat.format(context.getResources().getString(R.string.alias_message_from_to), message.get(GAME_ALIAS), message.get(SENDER), message.get(RECIPIENT));
			} else {
			    noteMessage = MessageFormat.format(context.getResources().getString(R.string.message_from_to), message.get(SENDER), message.get(RECIPIENT));
			}
			int iconResource = 0;
			if (DROIDIPPY.equals(message.get(SENDER))) {
			    iconResource = R.drawable.icon;
			} else if (ANONYMOUS.equals(message.get(SENDER))) {
			    iconResource = R.drawable.anonymous;
			} else {
			    iconResource = Game.POWER_FLAGS_MAP.get(message.get(SENDER));
			}
			Notification notification = getNotification(context,
								    iconResource,
								    noteMessage,
								    System.currentTimeMillis(),
								    message.get(SENDER));
			Intent chatIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("game:" + message.get(GAME_ID) + "#" + message.get(CHANNEL)), context, ViewGame.class);
			PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, chatIntent, PendingIntent.FLAG_UPDATE_CURRENT);
			notification.setLatestEventInfo(context, noteMessage, message.get(MESSAGE), pendingIntent);
			((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).notify("" + message.get(GAME_ID) + ":" + message.get(CHANNEL), MESSAGE_ID, notification);
		    }
		}
	    }
	}
    }

    private Notification getNotification(Context context, int icon, CharSequence text, long when, String vibrationCode) {
	Notification notification = new Notification(icon, text, when);
	if (getAudioNotification(context)) {
	    if (NOTIFICATION_SOUND_DEFAULT.equals(getNotificationSound(context))) {
		notification.defaults |= Notification.DEFAULT_SOUND;
	    } else {
		notification.sound = Uri.parse(getNotificationSound(context));
	    }
	}
	if (getVibrate(context)) {
	    if (vibrationCode == null || !getCustomVibration(context)) {
		notification.defaults |= Notification.DEFAULT_VIBRATE;
	    } else {
		notification.vibrate = getVibration(vibrationCode);
	    }
	}
	notification.defaults |= Notification.DEFAULT_LIGHTS;
	notification.flags |= Notification.FLAG_AUTO_CANCEL;
	notification.flags |= Notification.FLAG_SHOW_LIGHTS;
	return notification;
    }

    private long[] getVibration(String code) {
	List<Long> list = new ArrayList<Long>();
	byte[] bytes = code.getBytes();
	for (int i = 0; i < bytes.length; i++) {
	    if (bytes[i] < 91) {
		list.add((long) (bytes[i] - 55) * 10);
	    } else {
		list.add((long) (bytes[i] - 97) * 10);
	    }
	    list.add(100l);
	}
	long[] returnValue = new long[list.size()];
	for (int i = 0; i < list.size(); i++) {
	    returnValue[i] = list.get(i);
	}
	return returnValue;
    }

    public static void registerGCM(Context context) {
        if (hasToken(context)) {
            GCMRegistrar.checkDevice(context);
            GCMRegistrar.checkManifest(context);
            final String regId = GCMRegistrar.getRegistrationId(context);
            if (regId.equals("")) {
                GCMRegistrar.register(context, SENDER_ID);
            }    
        }
    }

    public static void unregisterGCM(Context context) {
        if (hasToken(context)) {
            Intent unregIntent = new Intent("com.google.android.c2dm.intent.UNREGISTER");
            unregIntent.putExtra("app", PendingIntent.getBroadcast(context, 0, new Intent(), 0));
            context.startService(unregIntent);
        }
    }
}