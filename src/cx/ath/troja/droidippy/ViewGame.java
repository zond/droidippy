
package cx.ath.troja.droidippy;

import android.app.*;
import android.os.*;
import android.widget.*;
import android.graphics.*;
import android.content.*;
import android.content.res.*;
import android.graphics.drawable.*;
import android.graphics.drawable.shapes.*;
import android.view.*;
import android.net.*;
import android.util.*;
import android.text.*;
import android.text.util.*;
import android.text.method.*;

import java.util.*;
import java.text.*;

import static cx.ath.troja.droidippy.Util.*;

public class ViewGame extends BaseActivity {

    private static final int DIALOG_DRAW_POWERS = 0;

    private class MessageAdapter extends ArrayAdapter<Map<String, String>> {
	public MessageAdapter(List<Map<String, String>> messages) {
	    super(ViewGame.this, android.R.layout.simple_list_item_1, messages);
	}
	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
	    Map<String, String> map = getItem(position);
	    View returnValue = null;
	    if (PHASE.equals(map.get(TYPE))) {
		returnValue = ((LayoutInflater) ViewGame.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.chat_phase, null);
		((TextView) returnValue.findViewById(R.id.chat_phase_name)).setText(map.get(NAME));
	    } else if (MESSAGE.equals(map.get(TYPE))) {
		returnValue = ((LayoutInflater) ViewGame.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.chat_message, null);
		ImageView flag = (ImageView) returnValue.findViewById(R.id.chat_message_image);
		int imageResource = 0;
		if (DROIDIPPY.equals(getItem(position).get(SENDER))) {
		    imageResource = R.drawable.otto;
		} else if (ANONYMOUS.equals(getItem(position).get(SENDER))) {
		    imageResource = R.drawable.anonymous;
		} else {
		    imageResource = Game.POWER_FLAGS_MAP.get(getItem(position).get(SENDER));
		}
		if (DROIDIPPY.equals(getIntent().getData().getFragment())) {
		    float[] hsv = new float[3];
		    Color.colorToHSV(getColor(getContext(), game.power), hsv);
		    hsv[2] *= 0.85;
		    hsv[1] *= 1.5;
		    ((RelativeLayout) returnValue.findViewById(R.id.chat_message_background)).setBackgroundColor(Color.HSVToColor(0x33, hsv));
		} else if (!CONFERENCE.equals(getIntent().getData().getFragment()) && !ANONYMOUS.equals(getIntent().getData().getFragment())) {
		    float[] hsv = new float[3];
		    Color.colorToHSV(getColor(getContext(), getIntent().getData().getFragment()), hsv);
		    hsv[2] *= 0.85;
		    hsv[1] *= 1.5;
		    ((RelativeLayout) returnValue.findViewById(R.id.chat_message_background)).setBackgroundColor(Color.HSVToColor(0x33, hsv));
		}
		flag.setImageResource(imageResource);
		flag.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
			    Toast.makeText(ViewGame.this, getItem(position).get(SENDER), Toast.LENGTH_SHORT).show();
			}
		    });
                TextView messageView = (TextView) returnValue.findViewById(R.id.chat_message_message);
                if (isPre4()) {
                    messageView.setVisibility(View.GONE);
                    messageView = ((TextView) returnValue.findViewById(R.id.chat_message_message_old));
                    messageView.setVisibility(View.VISIBLE);
                }
		messageView.setText(getItem(position).get(MESSAGE));
	    }
	    String text = null;
	    if (BaseActivity.absoluteTimes) {
		text = toAbsoluteTimeString(-1 * new Long(map.get(AGE)));
	    } else {
		text = MessageFormat.format(ViewGame.this.getString(R.string.time_ago), toRelativeTimeString(new Long(map.get(AGE))));
	    }
	    ((TextView) returnValue.findViewById(R.id.chat_timestamp)).setText(text);
	    adjustTextViews(returnValue);
	    return returnValue;
	}
    }

    private class ChatItem {
	public String channel;
	public ChatItem(String channel) {
	    this.channel = channel;
	}
	public Long getUnread() {
	    return game.eachUnreadMessages.get(channel);
	}
	public Long getMessages() {
	    return game.eachMessages.get(channel);
	}
    }

    private class ChannelAdapter extends ArrayAdapter<ChatItem> {
	public ChannelAdapter() {
	    super(ViewGame.this, android.R.layout.simple_list_item_1, getChannels());
	}
	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
	    View returnValue = ((LayoutInflater) ViewGame.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.chat_channel, null);
	    ChatItem item = getItem(position);
	    if (item != null) {
		((TextView) returnValue.findViewById(R.id.chat_channel_name)).setText(item.channel);
		Long unread = item.getUnread();
		Long messages = item.getMessages();
		TextView email = (TextView) returnValue.findViewById(R.id.chat_channel_email);
		if (game.publicEmail &&
		    !DROIDIPPY.equals(item.channel) && 
		    !ANONYMOUS.equals(item.channel) &&
		    (screenLayout == Configuration.SCREENLAYOUT_SIZE_XLARGE ||
		     orientation == Configuration.ORIENTATION_LANDSCAPE)) {
		    email.setText(game.emailByPower.get(item.channel));
		    email.setVisibility(View.VISIBLE);
		} else {
		    email.setVisibility(View.GONE);
		}
		if (messages != null && messages > 0) {
		    if (unread != null && unread > 0) {
			((TextView) returnValue.findViewById(R.id.chat_channel_messages)).setText("" + unread + "/" + messages);
		    } else {
			((TextView) returnValue.findViewById(R.id.chat_channel_messages)).setText("" + messages);
		    }
		} else {
		    ((TextView) returnValue.findViewById(R.id.chat_channel_messages)).setText("");
		}
	    }
	    adjustTextViews(returnValue);
	    return returnValue;
	}
    }

    private class ZoomListener implements View.OnTouchListener {

	private static final int NONE = 0;
	private static final int DRAG = 1;
	private static final int ZOOM = 2;

	private static final int LONG_CLICK_DELAY = 500;
	private static final int SHORT_CLICK_DELAY = 50;

	public float maxScale = 0;
	public float minScale = 0;
	private float[] f = new float[9];

	public Matrix matrix = new Matrix();
	private Matrix savedMatrix = new Matrix();
	private int mode = NONE;
	private PointF start = new PointF();
	private float zoomDistance = 0;
	private PointF zoomCenter = new PointF();

	private Matrix pointerMatrix = new Matrix();
	private float[] pointerClick = new float[2];
	private boolean fastClick = false;
	private boolean orderClick = false;

	private Poi longClickMaxPan = null;
	private Runnable orderClickRunnable = new Runnable() {
		public void run() {
		    try {
			vibrate(30);
			waitingOrder.setNext(pointerClick);
			processOrder(waitingOrder);
		    } catch (OrderParamException e) {
			waitingOrder = null;
			toast(e.messageResource);
		    }
		}
	    };
	private Runnable longClickRunnable = new Runnable() {
		public void run() {
		    fastClick = false;
		    displayOrderOptions(game.getOrders(pointerClick[0], pointerClick[1]));
		}
	    };
	public Order waitingOrder = null;

	private Poi getLongClickMaxPan() {
	    if (longClickMaxPan == null) {
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		longClickMaxPan = new Poi(metrics.xdpi * ((longClickSensitivity + 1) / 20.0f),
					  metrics.ydpi * ((longClickSensitivity + 1) / 20.0f));
	    } 
	    return longClickMaxPan;
	}
	
	public void scale(float f) {
	    Display display = getWindowManager().getDefaultDisplay();
	    matrix.postScale(f, f, display.getWidth() / 2f, display.getHeight() / 2f);
 	    ((ImageView) findViewById(R.id.view_game_map)).setImageMatrix(matrix);	    
	}

	@Override
	public boolean onTouch(View view, MotionEvent event) {
	    switch (event.getAction() & MotionEvent.ACTION_MASK) {
	    case MotionEvent.ACTION_DOWN:
		if (orderDescriptionCanceller != null) {
		    orderDescriptionCanceller.cancel();
		    orderDescriptionCanceller = null;
		}
		pointerClick[0] = event.getX();
		pointerClick[1] = event.getY();
		matrix.invert(pointerMatrix);
		pointerMatrix.mapPoints(pointerClick);
		if (waitingOrder != null && waitingOrder.nextType() == Order.PROVINCE) {
		    orderClick = true;
		} else {
		    handler.postDelayed(longClickRunnable, LONG_CLICK_DELAY);		    
		    fastClick = true;
		}			    
		savedMatrix.set(matrix);
		start.set(event.getX(), event.getY());
		mode = DRAG;
		break;
	    case MotionEvent.ACTION_UP:		
	    case MotionEvent.ACTION_POINTER_UP:
		mode = NONE;
		handler.removeCallbacks(longClickRunnable);
		if (fastClick) {
		    longClickRunnable.run();
		    fastClick = false;
		} else if (orderClick) {
		    orderClickRunnable.run();
		    orderClick = false;
		}
		break;
	    case MotionEvent.ACTION_POINTER_DOWN:
		fastClick = false;
		orderClick = false;
		handler.removeCallbacks(longClickRunnable);
		zoomDistance = new PointF(event.getX(0) - event.getX(1),
					  event.getY(0) - event.getY(1)).length();
		if (zoomDistance > 10f) {
		    savedMatrix.set(matrix);
		    zoomCenter = new PointF((event.getX(0) + event.getX(1)) / 2,
					    (event.getY(0) + event.getY(1)) / 2);
		    mode = ZOOM;
		}
		break;
	    case MotionEvent.ACTION_MOVE:
		if (Math.abs(event.getX(0) - start.x) > getLongClickMaxPan().x ||
		    Math.abs(event.getY(0) - start.y) > getLongClickMaxPan().y) {

		    fastClick = false;
		    orderClick = false;
		    handler.removeCallbacks(longClickRunnable);
		}
		if (mode == DRAG) {
		    matrix.set(savedMatrix);
		    matrix.postTranslate(event.getX() - start.x,
					 event.getY() - start.y);

		    RectF mapRect = new RectF(0, 0, mapDimensions.x, mapDimensions.y);
		    matrix.mapRect(mapRect);
		    Poi correction = new Poi(0,0);
		    ViewGroup layout = (ViewGroup) findViewById(R.id.view_game_map).getParent();
		    if (mapRect.bottom < (layout.getHeight() / 2)) {
			correction.y += (layout.getHeight() / 2) - mapRect.bottom;
		    }
		    if (mapRect.top > layout.getHeight() / 2) {
			correction.y -= mapRect.top - (layout.getHeight() / 2);
		    }
		    if (mapRect.right < (layout.getWidth() / 2)) {
			correction.x += (layout.getWidth() / 2) - mapRect.right;
		    }
		    if (mapRect.left > layout.getWidth() / 2) {
			correction.x -= mapRect.left - (layout.getWidth() / 2);
		    }
		    matrix.postTranslate(correction.x, correction.y);
		} else if (mode == ZOOM) {
		    float newDistance = new PointF(event.getX(0) - event.getX(1),
						   event.getY(0) - event.getY(1)).length();
		    if (newDistance > 10f) {
			matrix.set(savedMatrix);
			float scale = newDistance / zoomDistance;
			matrix.getValues(f);
			if (scale * f[Matrix.MSCALE_X] > maxScale) {
			    scale = maxScale / f[Matrix.MSCALE_X];
			} else if (scale * f[Matrix.MSCALE_X] < minScale) {
			    scale = minScale / f[Matrix.MSCALE_X];
			}
			matrix.postScale(scale, scale, zoomCenter.x, zoomCenter.y);
		    }
		}
		break;
	    }
	    ImageView mapView = (ImageView) findViewById(R.id.view_game_map);
	    if (mapView != null) {
		mapView.setImageMatrix(matrix);
	    }
	    
	    return true;
	}
    }

    private class Render extends HandlerDoable<Object> {
	private boolean nil = false;
	private int view = -1;
	private boolean keepMap = false;
	private boolean forceLayout = false;
	private boolean keepMessages = false;
	public Render nil() {
	    nil = true;
	    return this;
	}
	public Render forceLayout() {
	    forceLayout = true;
	    return this;
	}
	public Render keepMap() {
	    keepMap = true;
	    return this;
	}
	public Render keepMessages() {
	    keepMessages = true;
	    return this;
	}
	public Render view(int view) {
	    this.view = view;
	    return this;
	}
	private void possiblyLayout(int checkId, int layout) {
	    if (forceLayout || 
		findViewById(checkId) == null) {
		setContentView(layout);
	    }
	}
	private void layout() {
	    if (view == -1) {
		if (findViewById(R.id.view_chat_layout) != null ||
		    findViewById(R.id.view_results_layout) != null) {

		    if (game.lastPhase()) {
			possiblyLayout(R.id.view_chat_layout, R.layout.view_chat);
		    } else {
			possiblyLayout(R.id.view_results_layout, R.layout.view_results);
		    }
		    
		} else if (findViewById(R.id.clear_layout) != null) {
		    if (fragment(getIntent()) == null) {
			if (screenLayout == Configuration.SCREENLAYOUT_SIZE_XLARGE) {
			    if (game.lastPhase()) {
				setContentView(R.layout.view_chat);
			    } else {
				setContentView(R.layout.view_results);
			    }
			} else {
			    setContentView(R.layout.view_game);
			}
		    } else {
			setContentView(R.layout.view_messages);
		    }
		} else if (forceLayout) {
		    if (findViewById(R.id.view_messages_layout) != null) {
			setContentView(R.layout.view_messages);
		    } else if (findViewById(R.id.view_orders_layout) != null) {
			setContentView(R.layout.view_orders);
		    } else if (findViewById(R.id.view_results_layout) != null) {
			setContentView(R.layout.view_orders);
		    } else if (findViewById(R.id.view_chat_layout) != null) {
			setContentView(R.layout.view_chat);
		    } else if (findViewById(R.id.view_game_layout) != null) {
			setContentView(R.layout.view_game);
		    }
		}

	    } else {
		if (view == R.layout.view_game) {
		    possiblyLayout(R.id.view_game_layout, view);
		} else if (view == R.layout.view_chat) {
		    possiblyLayout(R.id.view_chat_layout, view);
		} else if (view == R.layout.view_results) {
		    possiblyLayout(R.id.view_results_layout, view);
		} else if (view == R.layout.view_orders) {
		    possiblyLayout(R.id.view_orders_layout, view);
		} else if (view == R.layout.view_messages) {
		    possiblyLayout(R.id.view_messages_layout, view);
		}
	    }
	}
	public void run() {
	    if (!nil) {
		cancelNotifications();
		setTitle(game.toShortString() + (DEBUG_MODE ? " [debug]" : ""));
		if (!keepMap) {
		    clearMap();
		}
		ListAdapter adapter = null;
		ListView listView = (ListView) findViewById(R.id.chat_messages);
		if (keepMessages && listView != null) {
		    adapter = listView.getAdapter();
		}
		layout();
		listView = (ListView) findViewById(R.id.chat_messages);
		if (keepMessages && listView != null && adapter != null) {
		    listView.setAdapter(adapter);
		}
		prepare();
		fetchMessages();
		checkHelp();
		checkRules();
		checkPhaseMessage();
		checkProbation();
	    }
	}
	public void handle(Object o) {
	    run();
	} 
    }

    private Game game;
    private List<Drawable> layersWithOrders = null;
    private List<Drawable> layersWithoutOrders = null;
    private Drawable mapImage = null;
    private ZoomListener zoomListener = new ZoomListener();
    private Poi mapDimensions = null;
    private Menu menu;
    private int longClickSensitivity;
    private int orientation;
    private int screenLayout;
    private boolean showedPhaseMessage = false;
    private Cancellable orderDescriptionCanceller = null;
    private boolean splitscreen;
    private int lastSplitscreen;

    private List<ChatItem> getChannels() {
	ArrayList<ChatItem> returnValue = new ArrayList<ChatItem>();
	for (int i = 0; i < Game.POWERS.length; i++) {
	    if (Game.POWERS[i].equals(game.power)) {
		returnValue.add(new ChatItem(DROIDIPPY));
	    } else if (!CHAT_OFF.equals(game.privateChatSetting) || FINISHED.equals(game.gameState)) {
		returnValue.add(new ChatItem(Game.POWERS[i]));
	    }
	}
	returnValue.add(new ChatItem(getApplicationContext().getResources().getString(R.string.conference)));
	if (!CHAT_OFF.equals(game.privateChatSetting) || FINISHED.equals(game.gameState)) {
	    returnValue.add(new ChatItem(getApplicationContext().getResources().getString(R.string.anonymous)));
	}
	return returnValue;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
	super.onConfigurationChanged(newConfig);
	if (game != null && game.detailed) {
	    if (newConfig.orientation != orientation) {
		orientation = newConfig.orientation;
		new Render().keepMap().keepMessages().forceLayout().run();
	    }
	}
    }

    private void visualizeOrder(Order order) {
	List<Drawable> newLayers = new ArrayList<Drawable>(layersWithOrders);
	order.addToLayers(this, newLayers, mapDimensions);
	setMap(new LayerDrawable(newLayers.toArray(new Drawable[0])));
    }

    private void setMap(Drawable drawable) {
	ImageView map = (ImageView) findViewById(R.id.view_game_map);
	if (map != null) {
	    map.setImageDrawable(drawable);
	}
    }

    private void handleChatData(List<Map<String, String>> messages) {
	hideProgress();
	if (findViewById(R.id.chat_messages) != null) {
	    Collections.reverse(messages);
	    ((ListView) findViewById(R.id.chat_messages)).setAdapter(new MessageAdapter(messages));
	    ((ListView) findViewById(R.id.chat_messages)).setSelection(((ListView) findViewById(R.id.chat_messages)).getAdapter().getCount() - 1);
	}
    }

    private void fetchMessages() {
	if (findViewById(R.id.view_messages_layout) != null &&
	    game != null &&
	    getIntent().getData() != null &&
	    getIntent().getData().getFragment() != null) {

	    final Intent finalIntent = getIntent();
	    new Getter(this, MessageFormat.format(GET_MESSAGES_FORMAT, "" + game.id, getIntent().getData().getFragment())).onResult(new HandlerDoable<List<Map<String, String>>>() {
		    public void handle(List<Map<String, String>> messages) {
			if (finalIntent.filterEquals(getIntent())) {
			    game.eachUnreadMessages.put(getIntent().getData().getFragment(), 0l);
			    cancelChatNotifications();
			    handleChatData(messages);
			}
		    }
	    }).onError(STD_ERROR_HANDLER).start();
	} else {
	    hideProgress();
	}
    }

    private void processOrder(final Order order) {
	visualizeOrder(order);
	if (order.done()) {
	    zoomListener.waitingOrder = null;
	    game.execute(this, order, new HandlerDoable<Object>() {
		    public void handle(Object o) {
			layersWithOrders = null;
			mapImage = null;
			setMap(generateMap());
		    }
		}, STD_ERROR_HANDLER, 
		new HandlerDoable<String>() {
		    public void handle(String s) {
			if (s != null) {
			    toast(capitalize(game.provincify(s)));
			}
			layersWithOrders = null;
			mapImage = null;
			setMap(generateMap());
		    }
		});
	} else {
	    zoomListener.waitingOrder = order;
	    if (order.nextType() == Order.UNIT_TYPE) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.select_unit_type);
		builder.setSingleChoiceItems(Game.unitTypes(order.getSource()), -1, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
			    dialog.dismiss();
			    vibrate(30);
			    order.setNext(Game.UNIT_TYPES[which]);
			    processOrder(order);
			}
		    });
		builder.show();
	    } else {
		if (getOrderPrompts(this)) {
		    orderDescriptionCanceller = toast(order.nextDescription());
		}
	    }
	}
    }

    private void displayOrderOptions(final List<Order> orders) {
	if (!orders.isEmpty()) {
	    vibrate(30);
	    AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    builder.setTitle(MessageFormat.format(getResources().getString(R.string.select_order_type), Game.PROVINCE_NAMES.get(orders.get(0).source)));
	    String[] items = new String[orders.size()];
	    for (int i = 0; i < items.length; i++) {
		items[i] = getResources().getString(orders.get(i).getTypeResource());
	    }
	    builder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int which) {
			dialog.dismiss();
			vibrate(30);
			Order order = orders.get(which);
			if (order.getHelpText() != null && 
			    getOrderHelp(ViewGame.this, order.getClass())) {
			    
			    showOrderHelp(order);
			}
			processOrder(order);
		    }
		});
	    builder.show();
	}
    }

    private void showOrderHelp(final Order order) {
	final Set<Object> showAgain = new HashSet<Object>();
	showAgain.add(new Object());
	setOrderHelp(ViewGame.this, order.getClass(), false);
	setOrderHelp(ViewGame.this, Order.class, false);
	View helpLayout = getLayoutInflater().inflate(R.layout.order_help, null);
	((TextView) helpLayout.findViewById(R.id.order_help_message)).setText(order.getHelpText());
	((CheckBox) helpLayout.findViewById(R.id.order_help_show)).setChecked(true);
	((CheckBox) helpLayout.findViewById(R.id.order_help_show)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
		public void onCheckedChanged(CompoundButton button, boolean isChecked) {
		    if (isChecked) {
			showAgain.add(new Object());
		    } else {
			showAgain.clear();
		    }
		}
	    });
	AlertDialog alertDialog = new AlertDialog.Builder(ViewGame.this).setTitle(R.string.help).setView(helpLayout).setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int which) {
		    dialog.dismiss();
		    if (showAgain.isEmpty()) {
			for (Class klass : Order.SUBCLASSES) {
			    setOrderHelp(ViewGame.this, klass, false);
			}
		    }
		}
	    }).show();
    }

    private void loadNextPhase() {
	showProgress(R.string.loading_game_details);
	game.loadNextPhase(this, new Render(), STD_ERROR_HANDLER);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
	switch (item.getItemId()) {
	case R.id.view_game_all_games:
	    Intent allGamesIntent = new Intent(getApplicationContext(), Droidippy.class);
	    startActivity(allGamesIntent);
	    return true;
	case R.id.view_game_previous_phase:
	    showProgress(R.string.loading_game_details);
	    game.loadPreviousPhase(this, new Render(), STD_ERROR_HANDLER);
	    return true;
	case R.id.view_game_next_phase:
	    loadNextPhase();
	    return true;
	case R.id.view_game_current_phase:
	    showProgress(R.string.loading_game_details);
	    game.loadDetails(this, new Render(), STD_ERROR_HANDLER);
	    return true;
	case R.id.view_game_phase_list:
	    if (game != null && game.phaseList != null) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.history);
		final String[] items = new String[game.phaseList.size()];
		for (int i = 0; i < items.length; i++) {
		    items[i] = "" + game.phaseList.get(i).get(NAME);
		}
		builder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
			    dialog.dismiss();
			    game.phaseOrdinal = items.length - 1 - (long) which;
			    showProgress(R.string.loading_game_details);
			    game.reloadPhase(ViewGame.this, new Render(), STD_ERROR_HANDLER);
			}
		    });
		builder.show();
	    }
	    return true;
	default:
	    return super.onOptionsItemSelected(item);
	}
    }

    private void clearMap() {
	layersWithOrders = null;
	layersWithoutOrders = null;
	mapImage = null;
    }

    private void checkHelp() {
	if (!getHelpShowed(this)) {
	    AlertDialog.Builder builder = new AlertDialog.Builder(ViewGame.this);
	    builder.setTitle(R.string.help);
	    builder.setMessage(MessageFormat.format(getResources().getString(R.string.if_you_have_any_problems), game.power));
	    builder.setNeutralButton(R.string.ok, new OkClickable());
	    builder.show();
	    showedHelp(this);
	}
    }

    private void checkRules() {
	if (!getRulesShowed(this)) {
	    SpannableString message = new SpannableString(getResources().getString(R.string.abide_by_these_rules));
	    Linkify.addLinks(message, Linkify.ALL);
	    AlertDialog dialog = new AlertDialog.Builder(this).setTitle(R.string.behaviour).setMessage(message).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int which) {
			dialog.dismiss();
		    }
		}).create();
	    dialog.show();
	    ((TextView) dialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
	    showedRules(this);
	}
    }

    private void checkPhaseMessage() {
	if (!showedPhaseMessage) {
	    showedPhaseMessage = true;
	    if (game != null && !FINISHED.equals(game.gameState) && game.phaseMessage != null) {
		toast(game.phaseMessage);
	    }
	}
    }

    private void cancelChatNotifications() {
	if (findViewById(R.id.view_messages_layout) != null) {
	    ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel("" + game.id + ":" + getIntent().getData().getFragment(), GCMIntentService.MESSAGE_ID);
	    WidgetProvider.clearMessages(this, "" + game.id, "" + getIntent().getData().getFragment());
	}
    }

    private void cancelNotifications() {
	((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel("" + game.id, GCMIntentService.PHASE_ID);
	WidgetProvider.clearPhases(this, "" + game.id);
	cancelChatNotifications();
    }

    private void checkProbation() {
	if (!FINISHED.equals(game.gameState) && PROBATION.equals(game.memberState)) {
	    toast(R.string.you_are_on_probation);
	}
    }

    private void prepare() {
	prepareMessages();
	prepareOrders();
	prepareChat();
	prepareMap();
	prepareResults();
	connectIt();
    }

    private void prepareMessages() {
	if (findViewById(R.id.chat_title) != null) {
	    boolean identitySend = FINISHED.equals(game.gameState) || (CONFERENCE.equals(getIntent().getData().getFragment()) && CHAT_ON.equals(game.conferenceChatSetting)) || (!CONFERENCE.equals(getIntent().getData().getFragment()) && CHAT_ON.equals(game.privateChatSetting));
	    boolean anonSend = FINISHED.equals(game.gameState) || (CONFERENCE.equals(getIntent().getData().getFragment()) && !CHAT_OFF.equals(game.conferenceChatSetting)) || (!CONFERENCE.equals(getIntent().getData().getFragment()) && !CHAT_OFF.equals(game.privateChatSetting));
	    if (DROIDIPPY.equals(getIntent().getData().getFragment()) || 
		ANONYMOUS.equals(getIntent().getData().getFragment()) ||
		(!identitySend && !anonSend)) {
		
		((View) findViewById(R.id.chat_edit)).setVisibility(View.GONE);
		((View) findViewById(R.id.chat_send)).setVisibility(View.GONE);
		((View) findViewById(R.id.chat_send_anonymously)).setVisibility(View.GONE);
	    } else {
		if (!anonSend) ((View) findViewById(R.id.chat_send_anonymously)).setVisibility(View.GONE);
		if (!identitySend) ((View) findViewById(R.id.chat_send)).setVisibility(View.GONE);
                ((EditText) findViewById(R.id.chat_edit) ).setText(getDraft(getApplicationContext(), game, getIntent().getData().getFragment()));
                ((EditText) findViewById(R.id.chat_edit) ).addTextChangedListener(new TextWatcher() {
                        public void afterTextChanged(Editable e) {
                            Util.storeDraft(getApplicationContext(), 
                                            game, 
                                            getIntent().getData().getFragment(), 
                                            e.toString());
                        }
                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                        }
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                        }
                    });
		((EditText) findViewById(R.id.chat_edit) ).requestFocus();
	    }
	    if (getIntent().getData().getFragment().equals(getResources().getString(R.string.conference))) {
		((TextView) findViewById(R.id.chat_title)).setText(R.string.conference);
		((TextView) findViewById(R.id.chat_email)).setVisibility(View.GONE);
	    } else if (getIntent().getData().getFragment().equals(getResources().getString(R.string.anonymous))) {
		((TextView) findViewById(R.id.chat_title)).setText(R.string.anonymous);
		((TextView) findViewById(R.id.chat_email)).setVisibility(View.GONE);
	    } else {
		((TextView) findViewById(R.id.chat_title)).setText(MessageFormat.format(getResources().getString(R.string.negotiating_with), getIntent().getData().getFragment()));
		TextView email = (TextView) findViewById(R.id.chat_email);
		if (game.publicEmail) {
		    email.setText(game.emailByPower.get(getIntent().getData().getFragment()));
		    email.setVisibility(View.VISIBLE);
		} else {
		    email.setVisibility(View.GONE);
		}
	    }	
	    if (((ListView) findViewById(R.id.chat_messages)).getAdapter() == null) {
		((ListView) findViewById(R.id.chat_messages)).setAdapter(new MessageAdapter(new ArrayList<Map<String, String>>()));
	    }
	    ((Button) findViewById(R.id.chat_send)).setOnClickListener(new View.OnClickListener() {
		    public void onClick(View v) {
			sendMessage(false);
		    }
		});
	    ((Button) findViewById(R.id.chat_send_anonymously)).setOnClickListener(new View.OnClickListener() {
		    public void onClick(View v) {
			sendMessage(true);
		    }
		});
	    ((EditText) findViewById(R.id.chat_edit)).setFocusable(true);
	    ((EditText) findViewById(R.id.chat_edit)).setFocusableInTouchMode(true);
	}
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
	if (keyCode == KeyEvent.KEYCODE_BACK && findViewById(R.id.chat_title) != null) {
            getIntent().setData(getIntent().getData().buildUpon().fragment(null).build());
	    new Render().view(R.layout.view_chat).keepMap().run();
	    return true;
	} else {
	    return super.onKeyDown(keyCode, event);
	}
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
	getMenuInflater().inflate(R.menu.view_game_menu, menu);
	return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	initializeFrom(null, getIntent());
    }

    private String scheme(Intent intent) {
	if (intent == null || intent.getData() == null) {
	    return null;
	} else {
	    return intent.getData().getSchemeSpecificPart();
	}
    }

    private String fragment(Intent intent) {
	if (intent == null || intent.getData() == null) {
	    return null;
	} else {
	    return intent.getData().getFragment();
	}
    }

    private void initializeFrom(Intent oldIntent, Intent intent) {
	if (!intent.filterEquals(oldIntent)) {
	    if (eq(scheme(intent), scheme(oldIntent))) {
		if (!eq(fragment(intent), fragment(oldIntent))) {
		    showProgress(R.string.fetching_messages);
		    setContentView(R.layout.clear);
		}
	    } else {
		game = new Game(Long.parseLong(scheme(intent)));
		showedPhaseMessage = false;
		setTitle(DROIDIPPY);
		setContentView(R.layout.clear);
	    }
	}
    }

    @Override
    protected void onNewIntent(Intent intent) {
	initializeFrom(getIntent(), intent);
	super.onNewIntent(intent);
    }
    
    @Override
    protected void onPause() {
	GCMIntentService.viewGames.remove(this);
	setMatrix(this, zoomListener.matrix);
	super.onPause();
    }

    private void setup() {
	screenLayout = getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
	orientation = getResources().getConfiguration().orientation;
	longClickSensitivity = getLongClickSensitivity(this);
    }

    @Override
    protected void onResume() {
	super.onResume();
	setup();
	GCMIntentService.viewGames.put(this, this);
	if (game.detailed) {
	    game.reloadPhase(this, new Render(), STD_ERROR_HANDLER);
	} else {
	    showProgress(R.string.loading_game_details);
	    game.loadDetails(this, new Render(), STD_ERROR_HANDLER);
	}
    }

    private boolean deliverChatMessage(final Map<String, String> message) {
	if (findViewById(R.id.chat_messages) != null) {
	    message.put(TYPE, MESSAGE);
	    message.put(AGE, "0");
	    if (CONFERENCE.equals(getIntent().getData().getFragment()) && CONFERENCE.equals(message.get(RECIPIENT))) {
                handler.post(new Runnable() {
                        public void run() {
                            ((MessageAdapter) ((ListView) findViewById(R.id.chat_messages)).getAdapter()).add(message);
                            fetchMessages();
                        }
                    });
		return true;
	    } else if (eq(getIntent().getData().getFragment(), message.get(SENDER)) && game.power.equals(message.get(RECIPIENT))) {
                handler.post(new Runnable() {
                        public void run() {
                            ((MessageAdapter) ((ListView) findViewById(R.id.chat_messages)).getAdapter()).add(message);
                            fetchMessages();
                        }
                    });
		return true;
	    } else {
                handler.post(new Runnable() {
                        public void run() {
                            game.reloadPhase(ViewGame.this, new Render().nil(), STD_ERROR_HANDLER);
                        }
                    });
		return false;
	    }
	} else {
            handler.post(new Runnable() {
                    public void run() {
                        game.reloadPhase(ViewGame.this, new Render().keepMap(), STD_ERROR_HANDLER);
                    }
                });
	    return (findViewById(R.id.view_chat_layout) != null);
	}
    }

    public boolean deliver(final Map<String, String> data) {
	if (("" + game.id).equals(data.get(GAME_ID))) {
	    if (GCMIntentService.CHAT_MESSAGE.equals(data.get(TYPE))) {
		return deliverChatMessage(data);
	    } else if (GCMIntentService.GAME_PHASE.equals(data.get(TYPE)) && Long.parseLong("" + data.get(PHASE_ORDINAL)) > game.phaseOrdinal) {
                handler.post(new Runnable() {
                        public void run() {
                            zoomListener.waitingOrder = null;
                            if (FINISHED.equals(data.get(GAME_STATE))) {
                                gameFinished();
                            } else {
                                phaseResolved();
                            }
                        }
                    });
		return true;
	    } else {
		return false;
	    }
	} else {
	    return false;
	}
    }

    private void connectIt() {
	int rightMode = -1;
	int leftMode = -1;
	if (screenLayout == Configuration.SCREENLAYOUT_SIZE_XLARGE) {
	    if (findViewById(R.id.view_chat_layout) != null ||
		findViewById(R.id.view_results_layout) != null) {
		rightMode = R.layout.view_orders;
		leftMode = R.layout.view_orders;
	    } else if (findViewById(R.id.view_orders_layout) != null) {
		if (game.lastPhase()) {
		    rightMode = R.layout.view_chat;
		    leftMode = R.layout.view_chat;
		} else {
		    rightMode = R.layout.view_results;
		    leftMode = R.layout.view_results;
		}
	    }
	} else {
	    if (findViewById(R.id.view_game_layout) != null) {
		if (game.lastPhase()) {
		    rightMode = R.layout.view_chat;
		} else {
		    rightMode = R.layout.view_results;
		}
		leftMode = R.layout.view_orders;
	    } else if (findViewById(R.id.view_chat_layout) != null ||
		       findViewById(R.id.view_results_layout) != null) {
		rightMode = R.layout.view_orders;
		leftMode = R.layout.view_game;
	    } else if (findViewById(R.id.view_orders_layout) != null) {
		rightMode = R.layout.view_game;
		if (game.lastPhase()) {
		    leftMode = R.layout.view_chat;
		} else {
		    leftMode = R.layout.view_results;
		}
	    }
	}
	if (leftMode != -1 && findViewById(R.id.view_game_left_arrow) != null) {
	    final int finalLeftMode = leftMode;
	    ((ImageButton) findViewById(R.id.view_game_left_arrow)).setOnClickListener(new View.OnClickListener() {
		    public void onClick(View v) {
			vibrate(30);
			new Render().view(finalLeftMode).keepMap().run();
		    }
		});
	}
	if (rightMode != -1 && findViewById(R.id.view_game_right_arrow) != null) {
	    final int finalRightMode = rightMode;
	    ((ImageButton) findViewById(R.id.view_game_right_arrow)).setOnClickListener(new View.OnClickListener() {
		    public void onClick(View v) {
			vibrate(30);
			new Render().view(finalRightMode).keepMap().run();
		    }
		});
	}
    }

    private void gameFinished() {
	AlertDialog.Builder builder = new AlertDialog.Builder(ViewGame.this);
	builder.setTitle(R.string.game_finished);
	builder.setMessage(R.string.phase_resolved_to_finished_game);
	builder.setNeutralButton(R.string.ok, new OkClickable());
	builder.show();
	loadNextPhase();
    }

    private void phaseResolved() {
	showedPhaseMessage = false;
	AlertDialog.Builder builder = new AlertDialog.Builder(ViewGame.this);
	builder.setTitle(R.string.phase_resolved);
	builder.setMessage(R.string.phase_resolved_new_phase_created);
	builder.setNeutralButton(R.string.ok, new OkClickable());
	builder.show();
	loadNextPhase();
    }

    private void prepareOrders() {
	TextView orderView = ((TextView) findViewById(R.id.view_orders_text));
	if (orderView != null) {
            if (isPre4()) {
                orderView.setVisibility(View.GONE);
                orderView = ((TextView) findViewById(R.id.view_orders_text_old));
                orderView.setVisibility(View.VISIBLE);
            }
	    TextView phaseMessage = (TextView) findViewById(R.id.view_orders_phase_message);
	    if (game != null && 
		!FINISHED.equals(game.gameState) && 
		game.phaseMessage != null && 
		game.phaseMessage.trim().length() > 0) {
		
		phaseMessage.setText(game.phaseMessage);
		phaseMessage.setVisibility(View.VISIBLE);
	    } else {
		phaseMessage.setVisibility(View.GONE);
	    }
	    orderView.setText(game.renderOrders(this));
	    final Button commitButton = (Button) findViewById(R.id.view_orders_commit);
	    if (game.needsOrders) {
		if (game.resolved || FINISHED.equals(game.gameState)) {
		    commitButton.setEnabled(false);
		    commitButton.setText(R.string.no_orders);
		} else {
		    commitButton.setEnabled(true);
		    if (COMMITTED.equals(game.memberState)) {
			commitButton.setText(R.string.uncommit);
		    } else {
			commitButton.setText(R.string.commit);
		    }
		    commitButton.setOnClickListener(new View.OnClickListener() {
			    public void onClick(View v) {
				commitButton.setEnabled(false);
				if (getResources().getString(R.string.commit).equals(commitButton.getText().toString())) {
				    showProgress(R.string.committing_orders);
				    game.commit(getApplicationContext(), new HandlerDoable<String>() {
					    public void handle(String result) {
						hideProgress();		
						commitButton.setText(R.string.uncommit);
						commitButton.setEnabled(true);
					    }
					}, STD_ERROR_HANDLER);
				} else if (getResources().getString(R.string.uncommit).equals(commitButton.getText().toString())) {
				    showProgress(R.string.uncommitting_orders);
				    game.uncommit(getApplicationContext(), new HandlerDoable<String>() {
					    public void handle(String result) {
						hideProgress();		
						commitButton.setText(R.string.commit);
						commitButton.setEnabled(true);
					    }
					}, STD_ERROR_HANDLER);
				} else {
				    throw new RuntimeException("Button should always show commit or uncommit in an active phase where the member needs orders!");
				}
			    }
			});
		}
	    } else {
		commitButton.setEnabled(false);
		commitButton.setText(R.string.no_orders);
	    }
	}
    }

    private void prepareResults() {
	if (findViewById(R.id.view_results_layout) != null) {
	    ((TextView) findViewById(R.id.view_results_text)).setText(game.renderResults(this));
	}
    }

    @Override
    protected Dialog onCreateDialog(int id) {
	Dialog dialog;
	switch(id) {
	case DIALOG_DRAW_POWERS:
	    dialog = new Dialog(this);
	    dialog.setTitle(R.string.select_draw_powers);
	    dialog.setContentView(R.layout.select_draw_powers_dialog);
	    final Set<String> drawPowers = new HashSet<String>();
	    ((Button) dialog.findViewById(R.id.select_draw_powers_dialog_cancel)).setOnClickListener(new View.OnClickListener() {
		    public void onClick(View view) {
			Button intents = (Button) findViewById(R.id.view_chat_intent);
			if (intents != null) {
			    intents.setText(game.intent);
			}
			removeDialog(DIALOG_DRAW_POWERS);
		    }
		});
	    ((Button) dialog.findViewById(R.id.select_draw_powers_dialog_ok)).setOnClickListener(new View.OnClickListener() {
		    public void onClick(View view) {
			StringBuffer extras = new StringBuffer();
			Iterator<String> iterator = drawPowers.iterator();
			while (iterator.hasNext()) {
			    extras.append(iterator.next());
			    if (iterator.hasNext()) {
				extras.append(",");
			    }
			}
			setIntent(DRAW, extras.toString());
			removeDialog(DIALOG_DRAW_POWERS);
		    }
		});
	    ListView powersList = (ListView) dialog.findViewById(R.id.select_draw_powers_dialog_powers);
	    powersList.setAdapter(new ArrayAdapter<String>(this, R.layout.small_list_item_multiple_choice, Game.POWERS));
	    powersList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
	    powersList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
		    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			String power = Game.POWERS[position];
			if (drawPowers.contains(power)) {
			    drawPowers.remove(power);
			} else {
			    drawPowers.add(power);
			}
		    }
		});
	    break;
	default:
	    dialog = null;
	}
	return dialog;
    }

    private void setIntentExtraView() {
	TextView extrasView = (TextView) findViewById(R.id.view_chat_intent_extras);
	if (extrasView != null) {
	    if (game.intentExtras != null && game.intentExtras.length() > 0) {
		List<String> drawPowers = new ArrayList<String>(Arrays.asList(game.intentExtras.split(",")));
		if (drawPowers.contains(game.power)) {
		    drawPowers.remove(game.power);
		    extrasView.setText("Draw with " + annotate(drawPowers));
		} else {
		    extrasView.setText("Surrender to " + annotate(drawPowers));
		}
		extrasView.setVisibility(View.VISIBLE);
	    } else {
		extrasView.setVisibility(View.GONE);
	    }
	}
    }

    private void setIntent(String intent, final String extras) {
	showProgress(R.string.setting_intent);
	game.setIntent(getApplicationContext(), intent, extras, new HandlerDoable<Object>() {
		public void handle(Object o) {
		    setIntentExtraView();
		    Button intents = (Button) findViewById(R.id.view_chat_intent);
		    if (intents != null) {
			intents.setText(game.intent);
		    }
		    hideProgress();
		}
	    }, STD_ERROR_HANDLER);
    }

    private void prepareChat() {
	if (findViewById(R.id.view_chat_layout) != null) {
	    final ListView powers = (ListView) findViewById(R.id.view_chat_powers);
	    final ChannelAdapter adapter = new ChannelAdapter();
	    powers.setAdapter(adapter);
	    powers.setOnItemClickListener(new AdapterView.OnItemClickListener() {
		    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			getIntent().setData(getIntent().getData().buildUpon().fragment(adapter.getItem(position).channel).build());
			new Render().view(R.layout.view_messages).keepMap().run();
			showProgress(R.string.fetching_messages);
			fetchMessages();
		    }
		});
	    Button intents = (Button) findViewById(R.id.view_chat_intent);
	    intents.setText(game.intent);
	    intents.setOnClickListener(new View.OnClickListener() {
		    public void onClick(View view) {
			List<String> choices = new ArrayList<String>(INTENTS);
			if (game.dias) {
			    choices.remove(SURRENDER);
			}
			new AlertDialog.Builder(ViewGame.this).
			    setTitle(R.string.select_intent).
			    setSingleChoiceItems(choices.toArray(new String[0]), INTENTS.indexOf(game.intent), new DialogInterface.OnClickListener() {
				    public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					String intent = INTENTS.get(which);
					if (!game.dias && DRAW.equals(intent)) {
					    showDialog(DIALOG_DRAW_POWERS);
					} else {
					    setIntent(intent, null);
					}
				    }
				}).
			    show();
		    }
		});
	    setIntentExtraView();
	}
    }

    private Drawable generateMap() {
	if (mapImage == null) {
	    if (layersWithoutOrders == null) {
		Drawable standardMap = game.getMap(this);
		mapDimensions = new Poi(standardMap.getIntrinsicWidth(), standardMap.getIntrinsicHeight());
		ShapeDrawable background = new ShapeDrawable();
		background.getPaint().setColor(0xfffef2c1);
		background.getPaint().setStyle(Paint.Style.FILL);
		background.setIntrinsicWidth((int) mapDimensions.x);
		background.setIntrinsicHeight((int) mapDimensions.y);
		layersWithoutOrders = new ArrayList<Drawable>();
		layersWithoutOrders.add(background);
		layersWithoutOrders.add(standardMap);
		for (Drawable unit : game.getUnits(this, mapDimensions.x, mapDimensions.y)) {
		    layersWithoutOrders.add(unit);
		}
	    }
	    if (layersWithOrders == null) {
		layersWithOrders = new ArrayList<Drawable>(layersWithoutOrders);
		if (game.orders != null) {
		    for (Order order : game.orders.values()) {
			if (!order.failed) {
			    try {
				order.addToLayers(this, layersWithOrders, mapDimensions);
			    } catch (Exception e) {
				Log.w(getPackageName(), "Unable to draw '" + order + "'", e);
			    }
			}
		    }
		    for (Order order : game.orders.values()) {
			if (order.failed) {
			    try {
				order.addToLayers(this, layersWithOrders, mapDimensions);
			    } catch (Exception e) {
				Log.w(getPackageName(), "Unable to draw '" + order + "'", e);
			    }
			}
		    }
		}
	    }
	    if (game.resolved) {
		ShapeDrawable overlay = new ShapeDrawable();
		overlay.getPaint().setColor(0x88ffffff);
		overlay.getPaint().setStyle(Paint.Style.FILL);
		overlay.setIntrinsicWidth((int) mapDimensions.x);
		overlay.setIntrinsicHeight((int) mapDimensions.y);
		layersWithOrders.add(overlay);
	    }
	    mapImage = new LayerDrawable(layersWithOrders.toArray(new Drawable[0]));
	}
	return mapImage;
    }

    private void sendMessage(boolean anon) {
	final Intent finalIntent = getIntent();
	EditText edit = (EditText) findViewById(R.id.chat_edit);
	String message = edit.getText().toString().trim();
	if (message.length() > 0) {
	    Map<String, String> data = new HashMap<String, String>();
	    data.put(MESSAGE, message);
	    if (anon) {
		data.put(SENDER, ANONYMOUS);
	    } else {
		data.put(SENDER, game.power);
	    }
	    data.put(TYPE, MESSAGE);
	    data.put(AGE, "0");
	    edit.setText("");
	    ((MessageAdapter) ((ListView) findViewById(R.id.chat_messages)).getAdapter()).add(new HashMap<String, String>(data));
	    data.put(GAME_ID, "" + game.id);
	    data.put(RECIPIENT, getIntent().getData().getFragment());
	    new Poster(this, SEND_MESSAGE_URL, data).onResult(new HandlerDoable<List<Map<String, String>>>() {
		    public void handle(List<Map<String, String>> messages) {
			clearDraft(getApplicationContext(), game, getIntent().getData().getFragment());
			if (finalIntent.filterEquals(getIntent())) {
			    handleChatData(messages);
			}
		    }
	    }).onError(STD_ERROR_HANDLER).start();
	}
    }

    private void setupZoomListener() {
	Display display = getWindowManager().getDefaultDisplay();
	float minScaleX = display.getWidth() / mapDimensions.x;
	float minScaleY = display.getHeight() / mapDimensions.y;
	if (screenLayout == Configuration.SCREENLAYOUT_SIZE_XLARGE) {
	    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
		minScaleX /= 2.0;
	    } else if (orientation == Configuration.ORIENTATION_PORTRAIT) {
		minScaleY /= 2.0;
	    }
	}
	if (minScaleY > minScaleX) {
	    zoomListener.minScale = minScaleX;
	} else {
	    zoomListener.minScale = minScaleY;
	}
	zoomListener.maxScale = 30f;
    }

    private void prepareMap() {
	ImageView map = (ImageView) findViewById(R.id.view_game_map);
	if (map != null) {
	    setMap(generateMap());
	    map.setOnTouchListener(zoomListener);
	    setupZoomListener();
	    if (zoomListener.matrix.equals(new Matrix())) {
		float[] values = getMatrix(this);
		if (values == null) {
		    zoomListener.matrix.setScale(zoomListener.minScale, zoomListener.minScale);
		} else {
		    zoomListener.matrix.setValues(values);
		}
	    }
	    map.setImageMatrix(zoomListener.matrix);
	    if (getZoomButtons(this)) {
		ImageButton plus = ((ImageButton) findViewById(R.id.view_game_plus));
		plus.setVisibility(View.VISIBLE);
		ImageButton minus = ((ImageButton) findViewById(R.id.view_game_minus));
		minus.setVisibility(View.VISIBLE);
		plus.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
			    vibrate(30);
			    zoomListener.scale(1.25f);
			}
		    });
		minus.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
			    vibrate(30);
			    zoomListener.scale(0.8f);
			}
		    });
	    }
	    ImageButton splitscreenButton = (ImageButton) findViewById(R.id.view_game_split_screen);
	    if (splitscreenButton != null) {
		splitscreenButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
			    if (splitscreen) {
                                if (findViewById(R.id.view_messages_layout) != null) {
				    lastSplitscreen = R.layout.view_messages;
				} else if (findViewById(R.id.view_orders_layout) != null) {
				    lastSplitscreen = R.layout.view_orders;
				} else if (findViewById(R.id.view_results_layout) != null) {
				    lastSplitscreen = R.layout.view_orders;
				} else if (findViewById(R.id.view_chat_layout) != null) {
				    lastSplitscreen = R.layout.view_chat;
				} else if (findViewById(R.id.view_game_layout) != null) {
				    lastSplitscreen = R.layout.view_game;
				}
				splitscreen = false;
				new Render().view(R.layout.view_game).forceLayout().keepMap().run();
			    } else {
				splitscreen = true;
				new Render().view(lastSplitscreen).forceLayout().keepMap().run();
			    }
			}
		    });
	    }
	}
    }
 
}
