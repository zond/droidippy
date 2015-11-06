package cx.ath.troja.droidippy;

import android.app.*;
import android.os.*;
import android.content.*;
import android.widget.*;
import android.graphics.*;
import android.view.*;
import android.util.*;
import android.accounts.*;
import android.net.http.*;
import android.content.pm.*;

import org.apache.http.*;
import org.apache.http.params.*;
import org.apache.http.protocol.*;
import org.apache.http.client.protocol.*;
import org.apache.http.impl.client.*;
import org.apache.http.client.methods.*;
import org.apache.http.client.*;
import org.apache.http.impl.cookie.*;

import java.util.*;
import java.net.*;
import java.io.*;
import java.text.*;
import java.math.*;

public class Util {

  public static class MissingTokenException extends RuntimeException {
    public MissingTokenException(String s) {
      super(s);
    }
  }

  protected static boolean DEBUG_MODE = false;    

  protected static final String PACKAGE_NAME = "cx.ath.troja.droidippy";

  /**
   * Talking to our Dippy server
   */
  protected static final String BASE_DOMAIN = LocalOptions.BASE_DOMAIN;
  protected static final String BASE_HOST_WITH_PORT = BASE_DOMAIN + ":" + LocalOptions.BASE_PORT;
  protected static final String BASE_URL = "http://" + BASE_HOST_WITH_PORT + "/android";
  protected static final String REGISTER_URL_FORMAT = BASE_URL + "/register/{0}/{1}";
  protected static final String SET_INTENT_URL_FORMAT = BASE_URL + "/intend/{0}";
  protected static final String LOGIN_URL = BASE_URL + "/login";
  protected static final String SUBSCRIBE_URL_FORMAT = BASE_URL + "/subscribe/{0}/{1}?service=gcm";
  protected static final String UNSUBSCRIBE_URL_FORMAT = BASE_URL + "/unsubscribe/{0}";
  protected static final String GET_GAMES_URL = BASE_URL + "/games";
  protected static final String JOIN_RANDOM_GAME_URL = BASE_URL + "/join/random";
  protected static final String JOIN_PUBLIC_EMAIL_GAME_URL = BASE_URL + "/join/public";
  protected static final String JOIN_GAME_FORMAT = BASE_URL + "/game/join/{0}";
  protected static final String CREATE_GAME_URL = BASE_URL + "/game/create";
  protected static final String LOAD_DETAILS_PATTERN = BASE_URL + "/game/details/{0}";
  protected static final String LOAD_PHASE_PATTERN = BASE_URL + "/game/details/{0}/{1}";
  protected static final String SEND_ORDER_URL = BASE_URL + "/order/send";
  protected static final String COMMIT_ORDERS_FORMAT = BASE_URL + "/orders/commit/{0}/{1}";
  protected static final String UNCOMMIT_ORDERS_FORMAT = BASE_URL + "/orders/uncommit/{0}/{1}";
  protected static final String GET_MESSAGES_FORMAT = BASE_URL + "/messages/{0}/{1}";
  protected static final String UPDATE_END_YEAR_FORMAT = BASE_URL + "/game/update/endYear/{0}/{1}";
  protected static final String UPDATE_POINTS_SYSTEM_FORMAT = BASE_URL + "/game/update/pointsSystem/{0}/{1}";
  protected static final String UPDATE_PRIVATE_CHAT_FORMAT = BASE_URL + "/game/update/private/{0}/{1}";
  protected static final String UPDATE_CONFERENCE_CHAT_FORMAT = BASE_URL + "/game/update/conference/{0}/{1}";
  protected static final String KICK_MEMBER_FORMAT = BASE_URL + "/game/members/kick/{0}";
  protected static final String LEAVE_GAME_FORMAT = BASE_URL + "/game/leave/{0}";
  protected static final String INVITE_MEMBER_FORMAT = BASE_URL + "/game/members/invite/{0}";
  protected static final String SET_PHASE_LENGTH_FORMAT = BASE_URL + "/game/update/phaselength/{0}";
  protected static final String SEND_MESSAGE_URL = BASE_URL + "/message/send";
  protected static final String GET_INVITATIONAL_DETAILS_FORMAT = BASE_URL + "/game/invitational/details/{0}";
  protected static final String GET_INVITATIONAL_META_FORMAT = BASE_URL + "/game/invitational/meta/{0}";
  protected static final String EDIT_GAME_SETTINGS_FORMAT = BASE_URL + "/game/settings/{0}";
  protected static final String VERIFY_MARKET_PURCHASE_URL = BASE_URL + "/purchase";
  protected static final String GET_NONCE_URL = BASE_URL + "/nonce";
  protected static final String UPDATE_ALLOW_INVALID_FORMAT = BASE_URL + "/game/update/allowInvalid/{0}/{1}";
  protected static final String UPDATE_MINIMUM_RELIABILITY_FORMAT = BASE_URL + "/game/update/minimumReliability/{0}/{1}";
  protected static final String UPDATE_PREFERENCE_LIST_FORMAT = BASE_URL + "/game/update/preferenceList/{0}/{1}";
  protected static final String UPDATE_DIAS = BASE_URL + "/game/update/dias/{0}/{1}";
  protected static final String UPDATE_PUBLIC_EMAIL = BASE_URL + "/game/update/publicEmail/{0}/{1}";

  /**
   * Understanding our Dippy server
   */
  protected static final String DEFAULT_END_YEAR = "1936";

  protected static final int MAX_PLAYERS = 7;

  protected static final String DRAW = "Draw";
  protected static final String SURRENDER = "Surrender";
  protected static final List<String> INTENTS = Arrays.asList(new String[] { "Victory", DRAW, SURRENDER });

  protected static final String RECOMMENDED_VERSION = "recommended_version";
  protected static final String POINTS_SYSTEMS = "pointsSystems";
  protected static final String DROIDIPPY = "Droidippy";
  protected static final String UNCOMMITTED_PLAYERS = "uncommittedPlayers";
  protected static final String PREMIUM_SERVICE_UNTIL = "premium_service_until";
  protected static final String NOTIFICATION_ID = "notificationId";
  protected static final String RESULT = "result";
  protected static final String PRODUCT_NAME = "productName";
  protected static final String PURCHASE_OK = "purchase_ok";
  protected static final String CANCEL_OK = "cancel_ok";
  protected static final String ERROR = "error";
  protected static final String CONFERENCE = "Conference";
  protected static final String ANONYMOUS = "Anonymous";
  protected static final String TYPE = "type";
  protected static final String MEMBER_GAME_ALIAS = "member.gameAlias";
  protected static final String GAME_ALIAS = "game.alias";
  protected static final String MEMBER_INTENT = "member.intent";
  protected static final String MEMBER_INTENT_EXTRAS = "member.intentExtras";
  protected static final String MEMBER_NEEDS_ORDERS = "member.needsOrders";
  protected static final String UNCOMMITTED = "UNCOMMITTED";
  protected static final String COMMITTED = "COMMITTED";
  protected static final String NEW = "NEW";
  protected static final String CREATED = "CREATED";
  protected static final String HAS_NEW_PHASE = "HAS_NEW_PHASE";
  protected static final String FINISHED = "FINISHED";
  protected static final String STARTED = "STARTED";
  protected static final String INVITING = "INVITING";
  protected static final String FORMING = "FORMING";
  protected static final String INVITATION_CODE = "invitationCode";
  protected static final String GAME_INVITATION_CODE = "game.invitationCode";
  protected static final String PROBATION = "PROBATION";
  protected static final String ID = "id";
  protected static final String MOVEMENT = "Movement";
  protected static final String GAME_CREATOR = "game.creator";
  protected static final String ADJUSTMENT = "Adjustment";
  protected static final String ADJUSTMENT_BALANCE = "adjustment.balance";
  protected static final String RETREAT = "Retreat";
  protected static final String GAME_STATE = "game.state";
  protected static final String GAME_MINIMUM_RELIABILITY = "game.minimumReliability";
  protected static final String GAME_PREFERENCE_LIST = "game.preferenceList";
  protected static final String GAME_POINTS_SYSTEM = "game.pointsSystem";
  protected static final String GAME_END_YEAR = "game.endYear";
  protected static final String GAME_ALLOW_INVALID_ORDERS = "game.allowInvalidOrders";
  protected static final String GAME_DIAS = "game.dias";
  protected static final String GAME_PUBLIC_EMAIL = "game.publicEmail";
  protected static final String GAME_RELIABILITY_STEP = "game.reliabilityStep";
  protected static final String GAME_EMAIL_BY_POWER = "game.emailByPower";
  protected static final String CHAT_OFF = "OFF";
  protected static final String CHAT_ON = "ON";
  protected static final String RELIABILITY = "reliability";
  protected static final String CHAT_ANON = "ANON";
  protected static final String GAME_PRIVATE_CHAT_SETTING = "game.privateChatSetting";
  protected static final String GAME_CONFERENCE_CHAT_SETTING = "game.conferenceChatSetting";
  protected static final String GAME_MEMBERS = "game.members";
  protected static final String GAME_MEMBER_INTENTS = "game.memberIntents";
  protected static final String GAME_PHASE_COUNT = "game.phaseCount";
  protected static final String GAME_PHASE_LIST = "game.phaseList";
  protected static final String GAME_MEMBER_COUNT = "game.memberCount";
  protected static final String MEMBER_STRANDED = "member.stranded";
  protected static final String GAME_PHASE_LENGTH = "game.phaseLength";
  protected static final String PHASE_ORDINAL = "phase.ordinal";
  protected static final String PHASE_MESSAGE = "phase.message";
  protected static final String PHASE_FAILED_ORDERS = "phase.failedOrders";
  protected static final String PHASE_RESOLVED = "phase.resolved";
  protected static final String PHASE_RESULT = "phase.result";
  protected static final String TYPE_ARMY = "army";
  protected static final String TYPE_FLEET = "fleet";
  protected static final String TYPE_ARMY_DIS = "army/dislodged";
  protected static final String TYPE_FLEET_DIS = "fleet/dislodged";
  protected static final String TYPE_SUPPLY = "supply";
  protected static final String GAME_ID = "game.id";
  protected static final String PHASE_DEADLINE = "phase.deadline";
  protected static final String GAME_UPDATED_AT = "game.updatedAt";
  protected static final String GAME_CREATED_AT = "game.createdAt";
  protected static final String PHASE_NAME = "phase.name";
  protected static final String LAST_PHASE_NAME = "lastPhase.name";
  protected static final String PHASE_TYPE = "phase.type";
  protected static final String PHASE_POSITION = "phase.position";
  protected static final String PHASE_ORDER = "phase.order";
  protected static final String PHASE = "phase";
  protected static final String NAME = "name";
  protected static final String MESSAGE = "message";
  protected static final String POWER = "power";
  protected static final String SENDER = "sender";
  protected static final String AGE = "age";
  protected static final String MESSAGES = "messages";
  protected static final String RECIPIENT = "recipient";
  protected static final String MEMBER_POWER = "member.power";
  protected static final String MEMBER_ELIMINATED = "member.eliminated";
  protected static final String MEMBER_WANTS_NOTIFICATIONS = "member.wantsNotifications";
  protected static final String MEMBER_ALL_UNREAD = "member.allUnread";
  protected static final String MEMBER_EACH_UNREAD = "member.eachUnread";
  protected static final String MEMBER_EACH_MESSAGES = "member.eachMessages";
  protected static final String MEMBER_STATE = "member.state";
  protected static final String ORDER_SOURCE_PROVINCE = "order.sourceProvince";
  protected static final String ORDER_TEXT = "order.text";
  protected static final String ORDER_POWER = "order.power";    
  protected static final String CHANNEL = "channel";    
  protected static final String INVITER = "inviter";

  /**
   * Getting the authToken to register with our Dippy server
   */
  protected static final String AUTH_TOKEN_TYPE = "ah";
  protected static final String GOOGLE_ACCOUNT_TYPE = "com.google";

  /**
   * Misc. constants
   */
  protected static final String STORED_DRAFT_FORMAT = "Draft:{0}:{1}";
  protected static final String ARMY = "Army";
  protected static final String FLEET = "Fleet";
  protected static final String POWER_PREFERENCES = "power_preferences";
  protected static final String DIPPY_TOKEN = "dippy_token";
  protected static final String DIPPY_VERSION = "dippy_version";
  protected static final String ACCOUNT_NAME = "account_name";
  protected static final String BACKOFF = "backoff";
  protected static final String GAME = "game";
  protected static final String HIDE = "hide";
  protected static final String OTHER = "other";
  protected static final String VIBRATE = "vibrate";
  protected static final String CUSTOM_VIBRATION = "custom_vibration";
  protected static final String MATRIX = "matrix";
  protected static final String PENDING_MESSAGES = "pending_messages";
  protected static final String NEW_PHASES = "new_phases";
  protected static final String WIDGET_IDS = "widget_ids";
  protected static final String RULES_SHOWED = "rules_showed";
  protected static final String HELP_SHOWED = "help_showed";
  protected static final String ORDER_HELP = ".order_help";
  protected static final String LONG_GAME_DATA = "long_game_data";
  protected static final String LONG_CLICK_SENSITIVITY = "long_click_sensitivity";
  protected static final String LONG_CLICK_SENSITIVITY_DEFAULT = "2";
  protected static final String AUDIO_NOTIFICATION = "audio_notification";
  protected static final String ORDER_PROMPTS = "order_prompts";
  protected static final String NOTIFICATIONS = "notifications";
  protected static final String NOTIFICATION_SOUND = "notification_sound";
  protected static final String SHOW_FINISHED_GAMES = "show_finished_games";
  protected static final String SHOW_INVITATIONAL_GAMES = "show_invitational_games";
  protected static final String SHOW_CREATED_GAMES = "show_created_games";
  protected static final String NOTIFICATION_SOUND_DEFAULT = "Default";
  protected static final String HELP_URI = "http://www.oort.se/2011/04/droidippy-help.html?m=1";
  protected static final String FORUM_URI = "http://groups.google.com/group/droidippy";
  protected static final String INVITATION_GAMES_URI = "http://groups.google.com/group/droidippy-invitations";
  protected static final String STATS_URI = "http://droidippy.oort.se/m/you";
  protected static final String PAYMENT_INFO_URI = "http://www.oort.se/2011/08/droidippy-gameplay-faq.html#payment";
  protected static final String MEMBERS_OF_MAX_FORMAT = "({0}/{1} members)";
  protected static final String ZOOM_BUTTONS = "zoom_buttons";
  protected static final String ABSOLUTE_TIMES = "absolute_times";
  protected static final String CLASSICAL_ORDERS = "classical_order_format";
  protected static final String FONT_SIZES = "font_sizes";

  /**
   * Number of tries when doing HTTP
   */
  protected static final int MAX_RETRIES = 10;

  private static final String PREFERENCES_NAME = "cx.ath.troja.droidippy_preferences";

  private static SharedPreferences preferences = null;

  protected static String toAbsoluteTimeString(long msDelta) {
    Calendar cal = Calendar.getInstance();
    int days = (int) (msDelta / (1000 * 60 * 60 * 24));
    cal.add(Calendar.DAY_OF_YEAR, days);
    msDelta = msDelta - (days * 1000 * 60 * 60 * 24);
    cal.add(Calendar.MILLISECOND, (int) msDelta);
    DateFormat dateFormat = DateFormat.getDateInstance();
    DateFormat format = null;
    if (dateFormat.format(cal.getTime()).equals(dateFormat.format(Calendar.getInstance().getTime()))) {
      format = DateFormat.getTimeInstance();
    } else {
      format = DateFormat.getDateTimeInstance();
    }
    return format.format(cal.getTime());
  }

  protected static String annotate(List<? extends Object> items) {
    if (items == null || items.size() == 0) {
      return "";
    } else if (items.size() == 1) {
      return "" + items.get(0);
    } else {
      StringBuffer rval = new StringBuffer();
      for (int i = 0; i < items.size(); i++) {
	rval.append("" + items.get(i));
	if (i < items.size() - 2) {
	  rval.append(", ");
	} else if (i < items.size() - 1) {
	  rval.append(" and ");
	}
      }
      return rval.toString();
    }
  }

  protected static String toRelativeTimeString(long msDelta) {
    if (msDelta > 1000 * 60 * 60) {
      return "" + (msDelta / (1000 * 60 * 60)) + "h";
    } else if (msDelta > 1000 * 60) {
      return "" + (msDelta / (1000 * 60)) + "m";
    } else {
      return "" + (msDelta / 1000) + "s";
    }
  }

  protected static boolean hasToken(Context context) {
    return getDippyToken(context) != null;
  }

  protected static void storeDraft(Context context, Game game, String other, String draft) {
    SharedPreferences.Editor edit = getEditor(context);
    edit.putString(MessageFormat.format(STORED_DRAFT_FORMAT, "" + game.id, other), draft);
    edit.commit();
  }

  protected static String getDraft(Context context, Game game, String other) {
    return getPreferences(context).getString(MessageFormat.format(STORED_DRAFT_FORMAT, "" + game.id, other), "");
  }

  protected static void clearDraft(Context context, Game game, String other) {
    SharedPreferences.Editor edit = getEditor(context);
    edit.remove(MessageFormat.format(STORED_DRAFT_FORMAT, "" + game.id, other));
    edit.commit();
  }

  protected static synchronized SharedPreferences getPreferences(Context context) {
    if (preferences == null) {
      preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }
    return preferences;
  }

  protected static synchronized SharedPreferences.Editor getEditor(Context context) {
    return getPreferences(context).edit();
  }

  protected static void setMatrix(Context context, Matrix matrix) {
    float[] values = new float[9];
    matrix.getValues(values);
    StringBuffer buffer = new StringBuffer();
    for (int i = 0; i < values.length; i++) {
      buffer.append("" + values[i]);
      if (i < values.length - 1) {
	buffer.append(",");
      }
    }
    SharedPreferences.Editor edit = getEditor(context);
    edit.putString(MATRIX, buffer.toString());
    edit.commit();
  }

  protected static float[] getMatrix(Context context) {
    String joined = getPreferences(context).getString(MATRIX, null);
    if (joined == null) {
      return null;
    } else {
      String[] split = joined.split(",");
      float[] returnValue = new float[9];
      for (int i = 0; i < split.length; i++) {
	returnValue[i] = Float.parseFloat(split[i]);
      }
      return returnValue;
    }
  }

  protected static int getRGBColor(Context context, String power) {
    Integer defaultColor = Game.DEFAULT_POWER_COLORS.get(power);
    if (defaultColor == null) {
      defaultColor = 0xffffffff;
    }
    return getPreferences(context).getInt(power + "_color", defaultColor);
  }

  protected static int getColor(Context context, String power) {
    int color = getRGBColor(context, power);
    color = color | 0xcc000000;
    color = color & 0xccffffff;
    return color;
  }

  protected static void setRGBColor(Context context, String power, int color) {
    SharedPreferences.Editor edit = getEditor(context);
    edit.putInt(power + "_color", color);
    edit.commit();
  }

  protected static int getLongClickSensitivity(Context context) {
    return Integer.parseInt(getPreferences(context).getString(LONG_CLICK_SENSITIVITY, LONG_CLICK_SENSITIVITY_DEFAULT));
  }

  protected static Boolean getShowFinishedGames(Context context) {
    return getPreferences(context).getBoolean(SHOW_FINISHED_GAMES, true);
  }

  protected static float getFontSizes(Context context) {
    return Float.parseFloat(getPreferences(context).getString(FONT_SIZES, "1.0"));
  }

  protected static int[] getWidgetIds(Context context) {
    String idString = getPreferences(context).getString(WIDGET_IDS, "");
    if (idString != null && !idString.equals("")) {
      String[] ids = idString.split(",");
      int[] returnValue = new int[ids.length];
      for (int i = 0; i < ids.length; i++) {
	returnValue[i] = Integer.parseInt(ids[i]);
      }
      return returnValue;
    } else {
      return new int[0];
    }
  }

  protected static boolean androidNotifications(Context context) {
    return getPreferences(context).getBoolean(NOTIFICATIONS, true);
  }

  /**
   * Returns whether we had any widgets before this one.
   */
  protected static boolean addWidgetId(Context context, int wid) {
    int[] currentIds = getWidgetIds(context);
    StringBuffer newIds = new StringBuffer();
    for (int i = 0; i < currentIds.length; i++) {
      if (currentIds[i] == wid) {
	return currentIds.length > 0;
      }
      newIds.append("" + currentIds[i]).append(",");
    }
    newIds.append("" + wid);
    SharedPreferences.Editor edit = getEditor(context);
    edit.putString(WIDGET_IDS, newIds.toString());
    edit.commit();
    return currentIds.length > 0;
  }

  protected static void removeWidgetIds(Context context, int[] wids) {
    Set<Integer> toRemove = new HashSet<Integer>();
    for (int i = 0; i < wids.length; i++) {
      toRemove.add(wids[i]);
    }
    int[] currentIds = getWidgetIds(context);
    List<Integer> idSet = new ArrayList<Integer>();
    for (int i = 0; i < currentIds.length; i++) {
      if (!toRemove.contains(currentIds[i])) {
	idSet.add(currentIds[i]);
      }
    }
    StringBuffer newIds = new StringBuffer();
    Iterator<Integer> idIter = idSet.iterator();
    while (idIter.hasNext()) {
      newIds.append("" + idIter.next());
      if (idIter.hasNext()) {
	newIds.append(",");
      }
    }
    SharedPreferences.Editor edit = getEditor(context);
    edit.putString(WIDGET_IDS, newIds.toString());
    edit.commit();
  }

  protected static Boolean getShowInvitationalGames(Context context) {
    return getPreferences(context).getBoolean(SHOW_INVITATIONAL_GAMES, true);
  }

  protected static Boolean getShowCreatedGames(Context context) {
    return getPreferences(context).getBoolean(SHOW_CREATED_GAMES, true);
  }

  protected static Boolean getClassicalOrders(Context context) {
    return getPreferences(context).getBoolean(CLASSICAL_ORDERS, false);
  }

  protected static Boolean getAbsoluteTimes(Context context) {
    return getPreferences(context).getBoolean(ABSOLUTE_TIMES, false);
  }

  protected static Boolean getOrderPrompts(Context context) {
    return getPreferences(context).getBoolean(ORDER_PROMPTS, false);
  }

  protected static Boolean getLongGameData(Context context) {
    return getPreferences(context).getBoolean(LONG_GAME_DATA, true);
  }

  protected static Boolean getZoomButtons(Context context) {
    return getPreferences(context).getBoolean(ZOOM_BUTTONS, false);
  }

  protected static Boolean getVibrate(Context context) {
    return getPreferences(context).getBoolean(VIBRATE, true);
  }

  protected static Boolean getCustomVibration(Context context) {
    return getPreferences(context).getBoolean(CUSTOM_VIBRATION, true);
  }

  protected static Boolean getOrderHelp(Context context, Class klass) {
    return getPreferences(context).getBoolean(klass.getName() + ORDER_HELP, true);
  }

  protected static void setOrderHelp(Context context, Class klass, Boolean orderHelp) {
    SharedPreferences.Editor edit = getEditor(context);
    edit.putBoolean(klass.getName() + ORDER_HELP, orderHelp);
    edit.commit();
  }

  protected static Boolean getAudioNotification(Context context) {
    return getPreferences(context).getBoolean(AUDIO_NOTIFICATION, true);
  }

  protected static String getNotificationSound(Context context) {
    return getPreferences(context).getString(NOTIFICATION_SOUND, NOTIFICATION_SOUND_DEFAULT);
  }

  protected static long getBackoff(Context context) {
    return getPreferences(context).getLong(BACKOFF, 1000);
  }

  protected static void setBackoff(Context context, long backoff) {
    SharedPreferences.Editor edit = getEditor(context);
    edit.putLong(BACKOFF, backoff);
    edit.commit();
  }

  protected static boolean getHelpShowed(Context context) {
    return getPreferences(context).getBoolean(HELP_SHOWED, false);
  }

  protected static void showedHelp(Context context) {
    SharedPreferences.Editor edit = getEditor(context);
    edit.putBoolean(HELP_SHOWED, true);
    edit.commit();
  }

  protected static boolean getRulesShowed(Context context) {
    return getPreferences(context).getBoolean(RULES_SHOWED, false);
  }

  protected static void showedRules(Context context) {
    SharedPreferences.Editor edit = getEditor(context);
    edit.putBoolean(RULES_SHOWED, true);
    edit.commit();
  }

  protected static String getDippyToken(Context context) {
    return getPreferences(context).getString(DIPPY_TOKEN, null);
  }

  protected static String getAccountName(Context context) {
    return getPreferences(context).getString(ACCOUNT_NAME, null).toLowerCase();
  }

  protected static Account getAccount(Context context) {
    return getAccount(context, getAccountName(context));
  }

  protected static Account getAccount(Context context, String accountName) {
    Account[] googleAccounts = AccountManager.get(context).getAccountsByType(GOOGLE_ACCOUNT_TYPE);
    for (int i = 0; i < googleAccounts.length; i++) {
      if (accountName.equalsIgnoreCase(googleAccounts[i].name)) {
	return googleAccounts[i];
      }
    }
    throw new RuntimeException("No account " + accountName + " found!");
  }

  protected static String joinCollection(Collection<?> collection) {
    StringBuffer buffer = new StringBuffer();
    Iterator<?> iterator = collection.iterator();
    while (iterator.hasNext()) {
      buffer.append(iterator.next().toString());
      if (iterator.hasNext()) {
	buffer.append(",");
      }
    }
    return buffer.toString();
  }

  protected static List<String> getPowerPreferences(Context context) {
    String def = joinCollection(Arrays.asList(Game.POWERS));
    String value = getPreferences(context).getString(POWER_PREFERENCES, def);
    return new ArrayList<String>(Arrays.asList(value.split(",")));
  }

  protected static void setPowerPreferences(Context context, List<String> powers) {
    SharedPreferences.Editor edit = getEditor(context);
    edit.putString(POWER_PREFERENCES, joinCollection(powers));
    edit.commit();
  }

  protected static void setDippyToken(Context context, String s) {
    SharedPreferences.Editor edit = getEditor(context);
    edit.putString(DIPPY_TOKEN, s);
    edit.commit();
  }

  protected static void setAccountName(Context context, String s) {
    SharedPreferences.Editor edit = getEditor(context);
    edit.putString(ACCOUNT_NAME, s.toLowerCase());
    edit.commit();
  }

  protected static boolean eq(Object o1, Object o2) {
    return ((o1 != null && o1.equals(o2)) ||
	(o1 == null && o2 == null));
  }

  protected static String capitalize(String s) {
    return s.substring(0,1).toUpperCase() + s.substring(1);
  }

  protected static PackageInfo packageInfo(Context context) {
    try {
      return context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected static DefaultHttpClient getAuthorizedClient(Context context) {
    String dippyToken = getDippyToken(context);
    if (dippyToken == null) {
      throw new MissingTokenException("Can not get authorized client, has no dippy token!");
    } else {
      DefaultHttpClient returnValue = new HttpClient(context);
      HttpParams params = new BasicHttpParams();
      params.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
      returnValue.setParams(params);
      BasicClientCookie tokenCookie = new BasicClientCookie(DIPPY_TOKEN, dippyToken);
      tokenCookie.setDomain(BASE_DOMAIN);
      tokenCookie.setPath("/");
      returnValue.getCookieStore().addCookie(tokenCookie);
      BasicClientCookie versionCookie = new BasicClientCookie(DIPPY_VERSION, "" + packageInfo(context).versionCode);
      versionCookie.setDomain(BASE_DOMAIN);
      versionCookie.setPath("/");
      returnValue.getCookieStore().addCookie(versionCookie);
      return returnValue;
    }
  }

  protected static boolean isPre4() {
    return android.os.Build.VERSION.SDK_INT < new android.os.Build.VERSION_CODES().ICE_CREAM_SANDWICH;
  }

}
