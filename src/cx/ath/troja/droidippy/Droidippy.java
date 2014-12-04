package cx.ath.troja.droidippy;

import android.app.*;
import android.os.*;
import android.widget.*;
import android.view.*;
import android.util.*;
import android.accounts.*;
import android.content.*;
import android.net.*;
import android.text.*;
import android.text.util.*;
import android.text.method.*;

import java.util.regex.*;
import java.util.*;
import java.net.*;
import java.text.*;

import static cx.ath.troja.droidippy.Util.*;

public class Droidippy extends BaseActivity {

    private class GameListClickListener implements AdapterView.OnItemLongClickListener, AdapterView.OnItemClickListener {
	private Game getGame(AdapterView<?> parent, int position) {
	    GameListAdapter adapter = (GameListAdapter) parent.getAdapter();
	    return adapter.gameList.get(position);
	}
	private String getType(AdapterView<?> parent, int position) {
	    GameListAdapter adapter = (GameListAdapter) parent.getAdapter();
	    return adapter.gameTypes.get(position);
	}
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
	    final Game game = getGame(parent, position);
	    if (game == null) {
		return false;
	    } else {
		if (NEW.equals(game.gameState)) {
		    DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int which) {
				showProgress(R.string.leaving_game);
				new Getter<Object>(getApplicationContext(), MessageFormat.format(LEAVE_GAME_FORMAT, "" + game.id)).
				    onResult(new HandlerDoable<Object>() {
					    public void handle(Object o) {
						hideProgress();
					    }
					}).
				    onError(STD_ERROR_HANDLER).start();
			    }
			};
		    AlertDialog.Builder builder = new AlertDialog.Builder(Droidippy.this).
			setNeutralButton(R.string.cancel, new OkClickable());
		    if (getAccountName(getApplicationContext()).equalsIgnoreCase(game.creator)) {
			builder.setTitle(R.string.destroy_game);
			builder.setMessage(R.string.do_you_want_to_destroy_the_game);
			builder.setPositiveButton(R.string.destroy, clickListener);
		    } else {
			builder.setTitle(R.string.leave_game);
			builder.setMessage(R.string.do_you_want_to_leave_this_game);
			builder.setPositiveButton(R.string.leave, clickListener);
		    }
		    builder.show();
		    return true;
		} else {
		    editedGame = game;
		    removeDialog(DIALOG_GAME_SETTINGS);
		    showDialog(DIALOG_GAME_SETTINGS);
		    return true;
		}
	    }
	}
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
	    Game game = getGame(parent, position);
	    if (game != null) {
		if (FORMING.equals(getType(parent, position))) {
		    // foo
		} else if (INVITING.equals(getType(parent, position))) {
		    invitingGame = game;
		    showProgress(R.string.loading_game_details);
		    invitingGame.loadInvitationalDetails(getApplicationContext(), new HandlerDoable<Object>() {
			    public void handle(Object o) {
				hideProgress();
				removeDialog(DIALOG_INVITING_GAME);
				showDialog(DIALOG_INVITING_GAME);
			    }
			}, STD_ERROR_HANDLER, null);
		} else if (STARTED.equals(getType(parent, position))) {
		    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("game:" + game.id), getApplicationContext(), ViewGame.class);
		    startActivity(intent);
		} else {
		    throw new RuntimeException("Unknown game type " + getType(parent, position));
		}
	    }
	}
    }

    private class GameListAdapter extends ArrayAdapter<String> {
	public List<Game> gameList = new ArrayList<Game>();
	public List<String> gameTypes = new ArrayList<String>();
	public GameListAdapter() {
	    super(Droidippy.this, R.layout.game_list_item, new ArrayList<String>());	    
	    addList(R.string.orders_needed, gamesNeedingOrders, STARTED);
	    addList(R.string.waiting_games, waitingGames, STARTED);
	    addList(R.string.forming_games, formingGames, FORMING);
	    addList(R.string.created_games, createdGames, INVITING);
	    addList(R.string.inviting_games, invitingGames, INVITING);
	    addList(R.string.finished_games, finishedGames, STARTED);
	}
	public boolean areAllItemsEnabled() {
	    return false;
	}
	public boolean isEnabled(int position) {
	    return gameList.get(position) != null;
	}
	private void addList(int description, List<Game> list, String gameType) {
	    if (list.size() > 0) {
		addString(getResources().getString(description));
		gameTypes.add(null);
		for (Game game : list) {
		    addGame(game);
		    gameTypes.add(gameType);
		}
	    }
	}
	private void addGame(Game game) {
	    gameList.add(game);
	    add(null);
	}
	private void addString(String s) {
	    gameList.add(null);
	    add(s);
	}
	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
	    Game game = gameList.get(position);
	    View returnValue = null;
	    if (game == null) {
		returnValue = ((LayoutInflater) Droidippy.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.game_list_title_item, null);
		((TextView) returnValue.findViewById(R.id.game_list_title_item_text)).setText(getItem(position));
	    } else {
		returnValue = ((LayoutInflater) Droidippy.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.game_list_item, null);
		((TextView) returnValue.findViewById(R.id.game_list_item_text)).setText(game.toString());
	    }
	    adjustTextViews(returnValue);
	    return returnValue;
	}
    }

    /**
     * Dialog ids
     */
    private static final int DIALOG_SELECT_ACCOUNT = 0;
    private static final int DIALOG_ORDER_POWERS = 1;
    private static final int DIALOG_INVITING_GAME = 2;
    private static final int DIALOG_INVITE_MEMBER = 3;
    private static final int DIALOG_JOIN_INVITATIONAL = 4;
    private static final int DIALOG_GAME_SETTINGS = 5;
    private static final int DIALOG_CUSTOM_GAME_INFORMATION = 6;
    
    private static final int ORDER_POWERS_JOIN = 0;
    private static final int ORDER_POWERS_CREATE = 1;

    private static final Map<String, String> chatSettingKeys = new HashMap<String, String>();
    static {
	chatSettingKeys.put("On", CHAT_ON);
	chatSettingKeys.put("Off", CHAT_OFF);
	chatSettingKeys.put("Anonymous", CHAT_ANON);
    }
    private static final Map<String, String> reverseChatSettingKeys = new HashMap<String, String>();
    static {
	for (Map.Entry<String, String> entry : chatSettingKeys.entrySet()) {
	    reverseChatSettingKeys.put(entry.getValue(), entry.getKey());
	}
    }

    private static List<String> endYears;
    static {
	endYears = new ArrayList<String>();
	for (int i = 1902; i < 1937; i++) {
	    endYears.add("" + i);
	}
    }

    private static final Map<String, Long> deadlines = new HashMap<String, Long>();
    static {
	deadlines.put("10m", 10 * 60 * 1000l);
	deadlines.put("30m", 30 * 60 * 1000l);
	deadlines.put("1h", 60 * 60 * 1000l);
	deadlines.put("2h", 2 * 60 * 60 * 1000l);
	deadlines.put("4h", 4 * 60 * 60 * 1000l);
	deadlines.put("8h", 8 * 60 * 60 * 1000l);
	deadlines.put("12h", 12 * 60 * 60 * 1000l);
	deadlines.put("24h", 24 * 60 * 60 * 1000l);
	deadlines.put("36h", 36 * 60 * 60 * 1000l);
	deadlines.put("48h", 48 * 60 * 60 * 1000l);
	deadlines.put("72h", 72 * 60 * 60 * 1000l);
	deadlines.put("4d", 4 * 24 * 60 * 60 * 1000l);
	deadlines.put("6d", 6 *24 * 60 * 60 * 1000l);
	deadlines.put("10d", 10 * 24 * 60 * 60 * 1000l);
	deadlines.put("14d", 14 * 24 * 60 * 60 * 1000l);
    }
    private static final Map<Long, String> reverseDeadlines = new HashMap<Long, String>();
    static {
	for (Map.Entry<String, Long> entry : deadlines.entrySet()) {
	    reverseDeadlines.put(entry.getValue(), entry.getKey());
	}
    }

    private static Pattern PATTERN_406 = Pattern.compile("406: (.*)");

    private ArrayList<Game> gamesNeedingOrders = new ArrayList<Game>();
    private ArrayList<Game> waitingGames = new ArrayList<Game>();
    private ArrayList<Game> formingGames = new ArrayList<Game>();
    private ArrayList<Game> invitingGames = new ArrayList<Game>();
    private ArrayList<Game> createdGames = new ArrayList<Game>();
    private ArrayList<Game> finishedGames = new ArrayList<Game>();
    
    private final Doable<List<String>> JOIN_RANDOM_DOABLE = new Doable<List<String>>() {
	public void doit(List<String> powerPreferences) {
	    joinRandomGame(powerPreferences);
	}
    };
    private final Doable<List<String>> CREATE_GAME_DOABLE = new Doable<List<String>>() {
	public void doit(List<String> powerPreferences) {
	    createGame(powerPreferences);
	}
    };
    private final Doable<List<String>> JOIN_INVITATIONAL_DOABLE = new Doable<List<String>>() {
	public void doit(List<String> powerPreferences) {
	    joinInvitationalGame(invitationCode, powerPreferences);
	}
    };

    protected static void prepareCustomGameInformationDialog(final Activity activity,
							       final Doable<List<String>> doable,
							       Dialog dialog, 
							       final Map<String, Object> joinInvitationalMeta) {
        if (dialog.findViewById(R.id.custom_game_information_dialog_private_chat) != null) {
            ((TextView) dialog.findViewById(R.id.custom_game_information_dialog_private_chat)).
                setText(reverseChatSettingKeys.get("" + joinInvitationalMeta.get(GAME_PRIVATE_CHAT_SETTING)));
            ((TextView) dialog.findViewById(R.id.custom_game_information_dialog_conference_chat)).
                setText(reverseChatSettingKeys.get("" + joinInvitationalMeta.get(GAME_CONFERENCE_CHAT_SETTING)));
            ((TextView) dialog.findViewById(R.id.custom_game_information_dialog_deadline)).
                setText(reverseDeadlines.get(new Long("" + joinInvitationalMeta.get(GAME_PHASE_LENGTH))));
            ((TextView) dialog.findViewById(R.id.custom_game_information_dialog_public_email)).
                setText("true".equals("" + joinInvitationalMeta.get(GAME_PUBLIC_EMAIL)) ? R.string.yes : R.string.no);
            ((TextView) dialog.findViewById(R.id.custom_game_information_dialog_dias)).
                setText("true".equals("" + joinInvitationalMeta.get(GAME_DIAS)) ? R.string.yes : R.string.no);
            ((TextView) dialog.findViewById(R.id.custom_game_information_dialog_positions)).
                setText(activity.getResources().
                        getString("true".equals("" + joinInvitationalMeta.get(GAME_PREFERENCE_LIST)) ? 
                                  R.string.preference_list :
                                  R.string.random));
            ((TextView) dialog.findViewById(R.id.custom_game_information_dialog_allow_invalid_orders)).
                setText("true".equals("" + joinInvitationalMeta.get(GAME_ALLOW_INVALID_ORDERS)) ? R.string.yes : R.string.no);
            ((TextView) dialog.findViewById(R.id.custom_game_information_dialog_end_year)).
                setText(joinInvitationalMeta.get(GAME_END_YEAR) == null ? DEFAULT_END_YEAR : "" + joinInvitationalMeta.get(GAME_END_YEAR));
            ((TextView) dialog.findViewById(R.id.custom_game_information_dialog_points_system)).
                setText(joinInvitationalMeta.get(GAME_POINTS_SYSTEM) == null ? "-" : "" + joinInvitationalMeta.get(GAME_POINTS_SYSTEM));
            ((TextView) dialog.findViewById(R.id.custom_game_information_dialog_minimum_reliability)).
                setText(joinInvitationalMeta.get(GAME_MINIMUM_RELIABILITY) == null ? "0" : "" + joinInvitationalMeta.get(GAME_MINIMUM_RELIABILITY));
            ((Button) dialog.findViewById(R.id.custom_game_information_dialog_ok)).setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        activity.removeDialog(DIALOG_CUSTOM_GAME_INFORMATION);
                        if (!"true".equals(joinInvitationalMeta.get(GAME_PREFERENCE_LIST))) {
                            doable.doit(Arrays.asList(Game.POWERS));
                        } else {
                            activity.removeDialog(DIALOG_ORDER_POWERS);
                            activity.showDialog(DIALOG_ORDER_POWERS);
                        }
                    }
                });
        }
    }

    protected static Dialog prepareOrderPowersDialog(Context context, 
						     View.OnClickListener whenDone, 
						     int titleResource, 
						     int headerResource, 
						     List<DragAndDropAdapter> adapterContainer) {
	Dialog dialog = new Dialog(context);
	dialog.setContentView(R.layout.order_powers_dialog);
	dialog.setTitle(titleResource);
	((TextView) dialog.findViewById(R.id.order_powers_dialog_header)).setText(context.getResources().getString(headerResource));
	DragAndDropListView powerList = (DragAndDropListView) dialog.findViewById(R.id.order_powers_dialog_power_list);
	DragAndDropAdapter powerAdapter = new DragAndDropAdapter(context, 
								 R.layout.small_list_item, 
								 R.id.small_list_item_text, 
								 getPowerPreferences(context));
	adapterContainer.add(powerAdapter);
	powerList.setAdapter(powerAdapter);
	powerList.setDropListener(powerAdapter);
	Button commitButton = (Button) dialog.findViewById(R.id.order_powers_dialog_submit);
	commitButton.setOnClickListener(whenDone);
	return dialog;
    }

    private Doable<List<String>> orderPowersDoable = null;
    private String invitationCode = null;
    private Game editedGame = null;
    private Game invitingGame = null;
    private Dialog invitingGameDialog = null;
    private Map<String, Object> joinInvitationalMeta = null;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GCMIntentService.registerGCM(this);
        setContentView(R.layout.main);	

	ListView gameList = (ListView) findViewById(R.id.main_games);
	gameList.setAdapter(new GameListAdapter());
	GameListClickListener clickListener = new GameListClickListener();
	gameList.setOnItemClickListener(clickListener);
	gameList.setOnItemLongClickListener(clickListener);

	if (!hasToken(this)) {
	    register();
	}
    }

    public boolean deliver(Map<String, String> data) {
        handler.post(new Runnable() {
                public void run() {
                    fetchGames();
                }
            });
	return true;
    }

    private void register() {
	showProgress(R.string.registering);
	Account[] googleAccounts = AccountManager.get(this).getAccountsByType(GOOGLE_ACCOUNT_TYPE);
	
	if (googleAccounts.length == 0) {
	    hideProgress();
	    new AlertDialog.Builder(Droidippy.this).
		setMessage(R.string.must_have_google_account).
		setTitle(R.string.problem).
		setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
			    dialog.dismiss();
			    finish();
			}
		    }).show();
	} else if (googleAccounts.length > 1) {
	    removeDialog(DIALOG_SELECT_ACCOUNT);
	    showDialog(DIALOG_SELECT_ACCOUNT);
	} else {
	    setAccount(googleAccounts[0]);
	}
    }

    private void fetchGames() {
	if (hasToken(this)) {
	    if (invitingGameDialog != null && invitingGame != null) {
		invitingGame.loadInvitationalDetails(getApplicationContext(), new HandlerDoable<Object>() {
			public void handle(Object o) {
			    fixInvitingGameDialog();
			}
		    }, STD_ERROR_HANDLER, new HandlerDoable<String>() {
			public void handle(String s) {
			    removeDialog(DIALOG_INVITING_GAME);
			}
		    });
	    }
	    new Getter<Map<String, Object>>(this, GET_GAMES_URL).
		onResult(new HandlerDoable<Map<String, Object>>() {
			public void handle(Map<String, Object> data) {
			    hideProgress();
			    parseGames(data);
			}
		    }).onError(STD_ERROR_HANDLER).start();
	}
    }

    @Override
    protected void verificationResult(String result, String productName) {
	super.verificationResult(result, productName);
	fetchGames();
    }

    private void parseGames(Map<String, Object> data) {
	DEBUG_MODE = "true".equals(data.get("debug_mode"));
	int recommendedVersion = Integer.parseInt("" + data.get(RECOMMENDED_VERSION));
	if (recommendedVersion > packageInfo(this).versionCode) {
	    SpannableString message = new SpannableString(getResources().getString(R.string.an_upgrade_is_recommended).replace(";", "\n"));
	    Linkify.addLinks(message, Linkify.ALL);
	    AlertDialog dialog = new AlertDialog.Builder(this).
		setMessage(message).
		setTitle(R.string.outdated_client).
		setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int which) {
			dialog.dismiss();
			Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse("market://details?id=cx.ath.troja.droidippy"));
			startActivity(intent);
		    }
		}).
		setNegativeButton(R.string.cancel, new OkClickable()).
		create();
	    dialog.show();
	    ((TextView) dialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
	}
	setTitle(getApplicationContext().getResources().getString(R.string.app_name) + " <" + ("" + data.get("email")).trim() + ">" + (DEBUG_MODE ? " [debug]" : ""));
	if (data.containsKey(RELIABILITY)) {
	    reliability = Float.parseFloat("" + data.get(RELIABILITY));
	}
	Map<String, List<Map<String, Object>>> games = (Map<String, List<Map<String, Object>>>) data.get("games");
	boolean hasGames = false;
	gamesNeedingOrders.clear();
	if (games.containsKey(UNCOMMITTED)) {
	    for (Map<String, Object> game : games.get(UNCOMMITTED)) {
		hasGames = true;
		gamesNeedingOrders.add(new Game(game));
	    }
	}
	waitingGames.clear();
	if (games.containsKey(COMMITTED)) {
	    for (Map<String, Object> game : games.get(COMMITTED)) {
		hasGames = true;
		waitingGames.add(new Game(game));
	    }
	}
	formingGames.clear();
	if (games.containsKey(NEW)) {
	    for (Map<String, Object> game : games.get(NEW)) {
		hasGames = true;
		formingGames.add(new Game(game));
	    }
	}
	invitingGames.clear();
	if (games.containsKey(INVITING) && getShowInvitationalGames(getApplicationContext())) {
	    for (Map<String, Object> game : games.get(INVITING)) {
		Game g = new Game(game);
		if (!FINISHED.equals(g.gameState) || getShowFinishedGames(getApplicationContext())) {
		    hasGames = true;
		    invitingGames.add(g);
		}
	    }
	}
	createdGames.clear();
	if (games.containsKey(CREATED) && getShowCreatedGames(getApplicationContext())) {
	    for (Map<String, Object> game : games.get(CREATED)) {
		Game g = new Game(game);
		if (!FINISHED.equals(g.gameState) || getShowFinishedGames(getApplicationContext())) {
		    hasGames = true;
		    createdGames.add(g);
		}
	    }
	}
	finishedGames.clear();
	if (getShowFinishedGames(getApplicationContext()) && games.containsKey(FINISHED)) {
	    for (Map<String, Object> game : games.get(FINISHED)) {
		hasGames = true;
		finishedGames.add(new Game(game));
	    }
	}
	((ListView) findViewById(R.id.main_games)).setAdapter(new GameListAdapter());
	if (hasGames) {
	    findViewById(R.id.main_help_text).setVisibility(View.GONE);
	} else {
	    findViewById(R.id.main_help_text).setVisibility(View.VISIBLE);
	}
    }

    @Override
    protected void onPause() {
	super.onPause();
	GCMIntentService.gameLists.remove(this);
    }

    @Override
    protected void onResume() {
	if (!paused) {
	    showProgress(R.string.loading_games);
	}
	super.onResume();
	GCMIntentService.gameLists.put(this, this);
	fetchGames();
    }

    private void setAccount(Account account) {
        String lowerName = "";
        if (account.name != null) {
            lowerName = account.name.toLowerCase();
        }
	setAccountName(this, lowerName);
	new Registrator(this, 
			new HandlerDoable<Object>() {
			    public void handle(Object o) {
				fetchGames();
			    }
			},
			new HandlerDoable<RuntimeException>() {
			    public void handle(RuntimeException e) {
				hideProgress();
				error(e, true);
			    }
	}).start();
    }
    
    private void showJoinPublicWarning() {
	SpannableString message = new SpannableString(getResources().getString(R.string.joining_a_public_email_game));
	Linkify.addLinks(message, Linkify.ALL);
	AlertDialog dialog = new AlertDialog.Builder(this).setTitle(R.string.joining_is_serious_business).setMessage(message).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int which) {
		    dialog.dismiss();
		    joinPublicEmailGame();
		}
	    }).setNegativeButton(R.string.cancel, new OkClickable()).create();
	dialog.show();
	((TextView) dialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void showJoinWarning1() {
	SpannableString message = new SpannableString(getResources().getString(R.string.joining_is_serious_business_read_the_rules));
	Linkify.addLinks(message, Linkify.ALL);
	AlertDialog dialog = new AlertDialog.Builder(this).setTitle(R.string.joining_is_serious_business).setMessage(message).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int which) {
		    dialog.dismiss();
		    showJoinWarning2();
		}
	    }).setNegativeButton(R.string.cancel, new OkClickable()).create();
	dialog.show();
	((TextView) dialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void showJoinWarning2() {
	new AlertDialog.Builder(this).setTitle(R.string.big_brother_is_watching).setMessage(R.string.your_statistics_will_be_collected).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int which) {
		    dialog.dismiss();
		    orderPowersDoable = JOIN_RANDOM_DOABLE;
		    removeDialog(DIALOG_ORDER_POWERS);
		    showDialog(DIALOG_ORDER_POWERS);
		}
	    }).setNegativeButton(R.string.cancel, new OkClickable()).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
	switch (item.getItemId()) {
	case R.id.main_menu_stats:
	    Intent statsIntent = new Intent(getApplicationContext(), Statistics.class);
	    startActivity(statsIntent);
	    return true;
        case R.id.main_menu_invitation_games:
	    Intent invitationGamesIntent = new Intent(getApplicationContext(), InvitationGames.class);
	    startActivity(invitationGamesIntent);
            return true;
        case R.id.main_menu_forum:
	    Intent forumIntent = new Intent(getApplicationContext(), Forum.class);
	    startActivity(forumIntent);
            return true;
	case R.id.main_menu_game_chat:
	    new AlertDialog.Builder(this).
		setTitle(R.string.are_you_sure).
		setMessage(R.string.you_will_display_part_of_your_account).
		setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
			    dialog.dismiss();
			    Intent gameChatIntent = new Intent(getApplicationContext(), IRCClient.class);
			    startActivity(gameChatIntent);
			}
		    }).
		setNeutralButton(R.string.cancel, new OkClickable()).
		show();
	    return true;
	case R.id.main_menu_play:
	    List<String> baseOptions = new ArrayList<String>(Arrays.asList(getResources().getStringArray(R.array.play_options)));
	    if (reliability < 50 || hasFormingPublicGame()) {
		baseOptions.remove(getResources().getString(R.string.join_public_email));
	    }
	    if (hasFormingRandomGame()) {
		baseOptions.remove(getResources().getString(R.string.join_random_game));
	    }
	    final String[] options = baseOptions.toArray(new String[0]);
	    new AlertDialog.Builder(this).
		setTitle(R.string.play).
		setSingleChoiceItems(options, -1, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
			    dialog.dismiss();
			    if (getResources().getString(R.string.join_random_game).equals(options[which])) {
				showJoinWarning1();
			    } else if (getResources().getString(R.string.create_game).equals(options[which])) {
				orderPowersDoable = CREATE_GAME_DOABLE;
				removeDialog(DIALOG_ORDER_POWERS);
				showDialog(DIALOG_ORDER_POWERS);
			    } else if (getResources().getString(R.string.join_invitational_game).equals(options[which])) {
				removeDialog(DIALOG_JOIN_INVITATIONAL);
				showDialog(DIALOG_JOIN_INVITATIONAL);
			    } else if (getResources().getString(R.string.join_public_email).equals(options[which])) {
				showJoinPublicWarning();
			    } else {
				throw new RuntimeException("Unknown play option " + options[which]);
			    }
			}
		    }).
		setNegativeButton(R.string.cancel, new OkClickable()).
		show();
	    return true;
	case R.id.main_menu_preferences:
	    Intent preferencesIntent = new Intent(this, DippyPrefs.class);
	    startActivity(preferencesIntent);
	    return true;
	default:
	    return super.onOptionsItemSelected(item);
	}
    }

    private void createGame(List<String> powerPreferences) {
	showProgress(R.string.creating_game);
	new Poster<Object>(this, CREATE_GAME_URL, powerPreferences).
	    onResult(new HandlerDoable<Object>() {
		    public void handle(Object s) {
			hideProgress();
			fetchGames();
			new AlertDialog.Builder(Droidippy.this).setMessage(R.string.created_a_game_invite_players).setTitle(R.string.created_a_game).setNeutralButton(R.string.ok, new OkClickable()).show();
		    }
	    }).
	    onError(STD_ERROR_HANDLER).start();
    }

    private void joinInvitationalGame(String invitationCode, List<String> powerPreferences) {
	showProgress(R.string.joining_game);
	new Poster<String>(this, MessageFormat.format(JOIN_GAME_FORMAT, invitationCode), powerPreferences).
	    onResult(new HandlerDoable<String>() {
		    public void handle(String s) {
			hideProgress();
			if (!INVITING.equals(s) && !STARTED.equals(s)) {
			    toast(R.string.game_has_already_started);
			} else {
			    new AlertDialog.Builder(Droidippy.this).setTitle(R.string.joined_a_game).setMessage(R.string.joined_invitational_game).setNeutralButton(R.string.ok, new OkClickable()).show();
			}
		    }
		}).
            onError(406, new HandlerDoable<String>() {
                    public void handle(String s) {
                        hideProgress();
                        Matcher m = PATTERN_406.matcher(s);
                        if (m.matches()) {
                            toast(m.group(1));
                        } else {
                            toast(s);
                        }
                    }
                }).
	    onError(404, new HandlerDoable<String>() {
		    public void handle(String s) {
			hideProgress();
			toast(R.string.no_such_game);
		    }
		}).
	    onError(STD_ERROR_HANDLER).start();
    }

    private void joinPublicEmailGame() {
	showProgress(R.string.joining_game);
	new Getter<Object>(this, JOIN_PUBLIC_EMAIL_GAME_URL).
	    onResult(new HandlerDoable<Object>() {
		    public void handle(Object s) {
			hideProgress();
			fetchGames();
			new AlertDialog.Builder(Droidippy.this).setMessage(R.string.joined_a_game_it_will_show_up).setTitle(R.string.joined_a_game).setNeutralButton(R.string.ok, new OkClickable()).show();
		    }
	    }).
	    onError(STD_ERROR_HANDLER).start();
    }

    private void joinRandomGame(List<String> powerPreferences) {
	showProgress(R.string.joining_game);
	new Poster<Object>(this, JOIN_RANDOM_GAME_URL, powerPreferences).
	    onResult(new HandlerDoable<Object>() {
		    public void handle(Object s) {
			hideProgress();
			fetchGames();
			new AlertDialog.Builder(Droidippy.this).setMessage(R.string.joined_a_game_it_will_show_up).setTitle(R.string.joined_a_game).setNeutralButton(R.string.ok, new OkClickable()).show();
		    }
	    }).
	    onError(STD_ERROR_HANDLER).start();
    }

    private boolean hasInvitingCreatedGame() {
	for (int i = 0; i < createdGames.size(); i++) {
	    if (createdGames.get(i).creator != null && NEW.equals(createdGames.get(i).gameState)) {
		return true;
	    }
	}
	return false;
    }

    private boolean hasFormingPublicGame() {
	for (int i = 0; i < formingGames.size(); i++) {
	    if (formingGames.get(i).creator == null && formingGames.get(i).publicEmail && NEW.equals(formingGames.get(i).gameState)) {
		return true;
	    }
	}
	return false;
    }

    private boolean hasFormingRandomGame() {
	for (int i = 0; i < formingGames.size(); i++) {
	    if (formingGames.get(i).creator == null && !formingGames.get(i).publicEmail && NEW.equals(formingGames.get(i).gameState)) {
		return true;
	    }
	}
	return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
	getMenuInflater().inflate(R.menu.main, menu);
	return true;
    }
    
    private void fixInvitingGameDialog() {
	final Game finalGame = invitingGame;
	if (invitingGameDialog != null && finalGame != null) {
	    invitingGameDialog.setTitle(finalGame.creator.equalsIgnoreCase(getAccountName(this)) ? R.string.created_game : R.string.invitational_game);

	    ArrayList<String> deadlineNames = new ArrayList<String>(deadlines.keySet());
	    Collections.sort(deadlineNames, new Comparator<String>() {
		    public int compare(String s1, String s2) {
			return deadlines.get(s1).compareTo(deadlines.get(s2));
		    }
		});
	    final ArrayAdapter<String> deadlineAdapter = new ArrayAdapter<String>(this, R.layout.small_spinner_item, deadlineNames);
	    Spinner deadline = (Spinner) invitingGameDialog.findViewById(R.id.inviting_game_dialog_deadline);
	    deadline.setAdapter(deadlineAdapter);
	    for (int i = 0; i < deadlineNames.size(); i++) {
		String deadlineName = deadlineNames.get(i);
		if (deadlines.get(deadlineName).longValue() == finalGame.phaseLength && !deadline.getSelectedItem().equals(deadlineName)) {
		    deadline.setSelection(i, true);
		}
	    }
	    
	    List<String> positionNames = Arrays.asList(new String[] {
		    getResources().getString(R.string.preference_list),
		    getResources().getString(R.string.random)
		});
	    final ArrayAdapter<String> positionsAdapter = new ArrayAdapter<String>(this, R.layout.small_spinner_item, positionNames);
	    Spinner positions = (Spinner) invitingGameDialog.findViewById(R.id.inviting_game_dialog_positions);
	    positions.setAdapter(positionsAdapter);
	    if (finalGame.preferenceList) {
		positions.setSelection(0, true);
	    } else {
		positions.setSelection(1, true);
	    }

	    final CheckBox allowInvalidOrders = (CheckBox) invitingGameDialog.findViewById(R.id.inviting_game_dialog_allow_invalid_orders);
	    allowInvalidOrders.setChecked(finalGame.allowInvalidOrders);

	    final CheckBox dias = (CheckBox) invitingGameDialog.findViewById(R.id.inviting_game_dialog_dias);
	    dias.setChecked(finalGame.dias);
            
            final EditText minimumReliability = (EditText) invitingGameDialog.findViewById(R.id.inviting_game_dialog_minimum_reliability);
            minimumReliability.setText(finalGame.reliabilityStep == null ? "0" : "" + finalGame.reliabilityStep);

	    final CheckBox publicEmail = (CheckBox) invitingGameDialog.findViewById(R.id.inviting_game_dialog_public_email);
	    publicEmail.setChecked(finalGame.publicEmail);

	    ArrayAdapter<String> endYearAdapter = new ArrayAdapter<String>(this, R.layout.small_spinner_item, endYears);
	    Spinner endYear = (Spinner) invitingGameDialog.findViewById(R.id.inviting_game_dialog_end_year);
	    endYear.setAdapter(endYearAdapter);
	    boolean setEndYear = false;
	    for (int i = 0; i < endYears.size(); i++) {
		if (new Long(endYears.get(i)).equals(finalGame.endYear)) {
		    endYear.setSelection(i, true);
		    setEndYear = true;
		}
	    }
	    if (!setEndYear) {
		endYear.setSelection(endYears.size() - 1, true);
	    }

	    List<String> pointsSystems = new ArrayList<String>(finalGame.pointsSystems);
	    pointsSystems.add(0, "-");
	    final ArrayAdapter<String> pointsSystemAdapter = new ArrayAdapter<String>(this, R.layout.small_spinner_item, pointsSystems);
	    Spinner pointsSystem = (Spinner) invitingGameDialog.findViewById(R.id.inviting_game_dialog_points_system);
	    pointsSystem.setAdapter(pointsSystemAdapter);
	    for (int i = 0; i < pointsSystems.size(); i++) {
		if (pointsSystems.get(i).equals(finalGame.pointsSystem)) {
		    pointsSystem.setSelection(i, true);
		}
	    }

	    final LinearLayout members = (LinearLayout) invitingGameDialog.findViewById(R.id.inviting_game_dialog_members);
	    members.removeAllViews();
	    for (String member : finalGame.members) {
		TextView childView = (TextView) ((LayoutInflater) Droidippy.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.small_list_item, null);
		if (finalGame.creator.equalsIgnoreCase(getAccountName(this)) &&
		    NEW.equals(finalGame.gameState)) {
		    
		    childView.setOnLongClickListener(new View.OnLongClickListener() {
			    public boolean onLongClick(View view) {
				final View finalView = view;
				final String finalEmail = ((TextView) view).getText().toString();
				if (!finalEmail.equalsIgnoreCase(getAccountName(getApplicationContext()))) {
				    new AlertDialog.Builder(Droidippy.this).
					setTitle(finalEmail).
					setMessage(MessageFormat.format(getResources().getString(R.string.kick_member), finalEmail)).
					setPositiveButton(R.string.kick, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
						    dialog.dismiss();
						    new Poster<Object>(getApplicationContext(), MessageFormat.format(KICK_MEMBER_FORMAT, "" + finalGame.id), finalEmail).
							onResult(new HandlerDoable<Object>() {
								public void handle(Object o) {
								    members.removeView(finalView);
								}
							    }).
							onError(STD_ERROR_HANDLER).start();
						}
					    }).
					setNeutralButton(R.string.cancel, new OkClickable()).
					show();
				    return true;
				} else {
				    return false;
				}
			    }
			});
		}
		childView.setText(member);
		members.addView(childView);
	    }

	    Button invite = (Button) invitingGameDialog.findViewById(R.id.inviting_game_dialog_invite);

	    List<String> chatSettings = Arrays.asList(new String[] { "On", "Off", "Anonymous" });

	    final ArrayAdapter<String> privateChatAdapter = new ArrayAdapter<String>(this, R.layout.small_spinner_item, chatSettings);
	    Spinner privateChat = (Spinner) invitingGameDialog.findViewById(R.id.inviting_game_dialog_private_chat);
	    privateChat.setAdapter(privateChatAdapter);
	    for (int i = 0; i < chatSettings.size(); i++) {
		String chatSettingName = chatSettings.get(i);
		if (chatSettingKeys.get(chatSettingName).equals(finalGame.privateChatSetting)) {
		    privateChat.setSelection(i, true);
		}
	    }
	    
	    final ArrayAdapter<String> conferenceChatAdapter = new ArrayAdapter<String>(this, R.layout.small_spinner_item, chatSettings);
	    Spinner conferenceChat = (Spinner) invitingGameDialog.findViewById(R.id.inviting_game_dialog_conference_chat);
	    conferenceChat.setAdapter(conferenceChatAdapter);
	    for (int i = 0; i < chatSettings.size(); i++) {
		String chatSettingName = chatSettings.get(i);
		if (chatSettingKeys.get(chatSettingName).equals(finalGame.conferenceChatSetting)) {
		    conferenceChat.setSelection(i, true);
		}
	    }
	    
	    if (finalGame.creator.equalsIgnoreCase(getAccountName(this))) {
		if (FINISHED.equals(finalGame.gameState)) {
		    deadline.setEnabled(false);
		    allowInvalidOrders.setEnabled(false);
		} else {
		    allowInvalidOrders.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			    public void onCheckedChanged(CompoundButton button, boolean isChecked) {
				new Getter<Boolean>(getApplicationContext(), MessageFormat.format(UPDATE_ALLOW_INVALID_FORMAT, "" + finalGame.id, isChecked ? "true" : "false")).
				    onResult(new HandlerDoable<Boolean>() {
					    public void handle(Boolean b) {
						finalGame.allowInvalidOrders = b;
					    }
					}).
				    onError(STD_ERROR_HANDLER).start();
			    }
			});
		    deadline.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			    public void onItemSelected(AdapterView parent, View view, int position, long id) {
				final Long newPhaseLength = deadlines.get(deadlineAdapter.getItem(position));
				if (newPhaseLength.longValue() != finalGame.phaseLength) {
				    new Poster<Object>(getApplicationContext(), MessageFormat.format(SET_PHASE_LENGTH_FORMAT, "" + finalGame.id), newPhaseLength).
					onResult(new HandlerDoable<Object>() {
						public void handle(Object o) {
						    finalGame.phaseLength = newPhaseLength;
						}
					    }).
					onError(STD_ERROR_HANDLER).start();
				}
			    }
			    public void onNothingSelected(AdapterView parent) {
			    }
			});
		}
		if (NEW.equals(finalGame.gameState)) {
                    minimumReliability.setKeyListener(new DigitsKeyListener() {
                            public boolean onKeyUp(View view, Editable content, int keyCode, KeyEvent event) {
                                super.onKeyUp(view, content, keyCode, event);
                                new Getter<Integer>(getApplicationContext(), MessageFormat.format(UPDATE_MINIMUM_RELIABILITY_FORMAT, "" + finalGame.id, minimumReliability.getText())).
                                    onResult(new HandlerDoable<Integer>() {
                                            public void handle(Integer i) {
                                                finalGame.reliabilityStep = i;
                                            }
                                        }).
                                    onError(STD_ERROR_HANDLER).start();
                                return false;
                            }
                        });
		    endYear.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			    public void onItemSelected(AdapterView parent, View view, int position, long id) {
				String newSetting = endYears.get(position);
				if ((DEFAULT_END_YEAR.equals(newSetting) && !new Long(DEFAULT_END_YEAR).equals(finalGame.endYear)) || (!DEFAULT_END_YEAR.equals(newSetting) && !new Long(newSetting).equals(finalGame.endYear))) {
				    new Getter<Long>(getApplicationContext(), MessageFormat.format(UPDATE_END_YEAR_FORMAT, "" + finalGame.id, newSetting)).
					onResult(new HandlerDoable<Long>() {
						public void handle(Long o) {
						    finalGame.endYear = o;
						}
					    }).
					onError(STD_ERROR_HANDLER).start();
				}
			    }
			    public void onNothingSelected(AdapterView parent) {
			    }
			});
		    pointsSystem.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			    public void onItemSelected(AdapterView parent, View view, int position, long id) {
				String newSetting = pointsSystemAdapter.getItem(position);
				if (("-".equals(newSetting) && finalGame.pointsSystem != null) || (!"-".equals(newSetting) && !newSetting.equals(finalGame.pointsSystem))) {
				    new Getter<String>(getApplicationContext(), MessageFormat.format(UPDATE_POINTS_SYSTEM_FORMAT, "" + finalGame.id, newSetting)).
					onResult(new HandlerDoable<String>() {
						public void handle(String o) {
						    finalGame.pointsSystem = o;
						}
					    }).
					onError(STD_ERROR_HANDLER).start();
				}
			    }
			    public void onNothingSelected(AdapterView parent) {
			    }
			});
		    privateChat.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			    public void onItemSelected(AdapterView parent, View view, int position, long id) {
				String newSetting = chatSettingKeys.get(privateChatAdapter.getItem(position));
				if (!newSetting.equals(finalGame.privateChatSetting)) {
				    new Getter<String>(getApplicationContext(), MessageFormat.format(UPDATE_PRIVATE_CHAT_FORMAT, "" + finalGame.id, newSetting)).
					onResult(new HandlerDoable<String>() {
						public void handle(String o) {
						    finalGame.privateChatSetting = o;
						}
					    }).
					onError(STD_ERROR_HANDLER).start();
				}
			    }
			    public void onNothingSelected(AdapterView parent) {
			    }
			});
		    conferenceChat.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			    public void onItemSelected(AdapterView parent, View view, int position, long id) {
				String newSetting = chatSettingKeys.get(conferenceChatAdapter.getItem(position));
				if (!newSetting.equals(finalGame.conferenceChatSetting)) {
				    new Getter<String>(getApplicationContext(), MessageFormat.format(UPDATE_CONFERENCE_CHAT_FORMAT, "" + finalGame.id, newSetting)).
					onResult(new HandlerDoable<String>() {
						public void handle(String o) {
						    finalGame.conferenceChatSetting = o;
						}
					    }).
					onError(STD_ERROR_HANDLER).start();
				}
			    }
			    public void onNothingSelected(AdapterView parent) {
			    }
			});

		    positions.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			    public void onItemSelected(AdapterView parent, View view, int position, long id) {
				boolean newSetting = position == 0;
				if (newSetting != finalGame.preferenceList) {
				    new Getter<String>(getApplicationContext(), MessageFormat.format(UPDATE_PREFERENCE_LIST_FORMAT, "" + finalGame.id, "" + newSetting)).
					onResult(new HandlerDoable<String>() {
						public void handle(String o) {
						    finalGame.preferenceList = "true".equals(o);
						}
					    }).
					onError(STD_ERROR_HANDLER).start();
				}
			    }
			    public void onNothingSelected(AdapterView parent) {
			    }
			});

		    TextView code = (TextView) invitingGameDialog.findViewById(R.id.inviting_game_dialog_code);
		    code.setText(MessageFormat.format(getResources().getString(R.string.invitation_code), finalGame.invitationCode));
		    code.setOnClickListener(new View.OnClickListener() {
			    public void onClick(View v) {
				((android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE)).setText(finalGame.invitationCode);
				toast(R.string.invitation_code_copied);
			    }
			});

		    invite.setOnClickListener(new View.OnClickListener() {
			    public void onClick(View v) {
				removeDialog(DIALOG_INVITE_MEMBER);
				showDialog(DIALOG_INVITE_MEMBER);
			    }
			});

		    dias.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			    public void onCheckedChanged(CompoundButton button, boolean isChecked) {
				new Getter<Boolean>(getApplicationContext(), MessageFormat.format(UPDATE_DIAS, "" + finalGame.id, isChecked ? "true" : "false")).
				    onResult(new HandlerDoable<Boolean>() {
					    public void handle(Boolean b) {
						finalGame.dias = b;
					    }
					}).
				    onError(STD_ERROR_HANDLER).start();
			    }
			});
		    publicEmail.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			    public void onCheckedChanged(CompoundButton button, boolean isChecked) {
				new Getter<Boolean>(getApplicationContext(), MessageFormat.format(UPDATE_PUBLIC_EMAIL, "" + finalGame.id, isChecked ? "true" : "false")).
				    onResult(new HandlerDoable<Boolean>() {
					    public void handle(Boolean b) {
						finalGame.publicEmail = b;
					    }
					}).
				    onError(STD_ERROR_HANDLER).start();
			    }
			});
		} else {
		    ((TextView) invitingGameDialog.findViewById(R.id.inviting_game_dialog_code)).setVisibility(View.GONE);
		    invite.setVisibility(View.GONE);
		    privateChat.setEnabled(false);
		    conferenceChat.setEnabled(false);
		    endYear.setEnabled(false);
		    positions.setEnabled(false);
		    pointsSystem.setEnabled(false);
		    dias.setEnabled(false);
		    publicEmail.setEnabled(false);
                    minimumReliability.setEnabled(false);
		}
	    } else {
		((TextView) invitingGameDialog.findViewById(R.id.inviting_game_dialog_code)).setVisibility(View.GONE);
                minimumReliability.setEnabled(false);
		deadline.setEnabled(false);
		positions.setEnabled(false);
		invite.setVisibility(View.GONE);
		privateChat.setEnabled(false);
		conferenceChat.setEnabled(false);
		endYear.setEnabled(false);
		allowInvalidOrders.setEnabled(false);
		pointsSystem.setEnabled(false);
		dias.setEnabled(false);
		publicEmail.setEnabled(false);
	    }
	    invitingGameDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
		    public void onDismiss(DialogInterface dialog) {
			invitingGame = null;
			invitingGameDialog = null;
		    }
		});
	}
    }

    @Override
    protected Dialog onCreateDialog(int id) {
	Dialog dialog;
	switch(id) {
	case DIALOG_GAME_SETTINGS:
	    dialog = new Dialog(this);
	    dialog.setTitle(R.string.game_settings);
	    dialog.setContentView(R.layout.game_settings_dialog);
	    if (editedGame != null) {
		final EditText alias = (EditText) dialog.findViewById(R.id.game_settings_dialog_alias);
		alias.setText(editedGame.gameAlias == null ? "" : editedGame.gameAlias);
		final CheckBox silenced = (CheckBox) dialog.findViewById(R.id.game_settings_dialog_silenced);
		if (editedGame.eliminated || editedGame.stranded || FINISHED.equals(editedGame.gameState)) {
		    silenced.setChecked(!editedGame.wantsNotifications);
		} else {
		    silenced.setVisibility(View.GONE);
		    dialog.findViewById(R.id.game_settings_dialog_silenced_text).setVisibility(View.GONE);
		}
		final long finalGameId = editedGame.id;
		TextView gameId = (TextView) dialog.findViewById(R.id.game_settings_dialog_gameid);
		String gameInfoText = "Game " + finalGameId;
		if (editedGame.reliabilityStep != null) {
		    gameInfoText = gameInfoText + ", reliability " + editedGame.reliabilityStep;
		}
		gameId.setText(gameInfoText);
		gameId.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
			    ((android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE)).setText("" + finalGameId);
			    toast(R.string.game_id_copied);
			}
		    });
		((Button) dialog.findViewById(R.id.game_settings_dialog_update)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
			    Map<String, Object> params = new HashMap<String, Object>();
			    params.put(MEMBER_WANTS_NOTIFICATIONS, new Boolean(!silenced.isChecked()));
			    if (alias.getText().toString().length() > 0) {
				params.put(MEMBER_GAME_ALIAS, alias.getText().toString());
			    }
			    removeDialog(DIALOG_GAME_SETTINGS);
			    showProgress(R.string.changing_settings);
			    new Poster<Map<String, Object>>(getApplicationContext(), MessageFormat.format(EDIT_GAME_SETTINGS_FORMAT, "" + finalGameId), params).
				onResult(new HandlerDoable<Map<String, Object>>() {
					public void handle(Map<String, Object> data) {
					    hideProgress();
					    parseGames(data);
					}
				    }).onError(STD_ERROR_HANDLER).start();
			}
		    });
	    }
	    break;
	case DIALOG_CUSTOM_GAME_INFORMATION:
	    dialog = new Dialog(this);
	    dialog.setTitle(R.string.join_invitational_game);
	    dialog.setContentView(R.layout.custom_game_information_dialog);
	    prepareCustomGameInformationDialog(this, orderPowersDoable, dialog, joinInvitationalMeta);
	    break;
	case DIALOG_JOIN_INVITATIONAL:
	    dialog = new Dialog(this);
	    dialog.setTitle(R.string.join_invitational_game);
	    dialog.setContentView(R.layout.join_invitational_dialog);
	    final EditText code = (EditText) dialog.findViewById(R.id.join_invitational_dialog_code);
	    ((Button) dialog.findViewById(R.id.join_invitational_dialog_join)).setOnClickListener(new View.OnClickListener() {
		    public void onClick(View v) {
			removeDialog(DIALOG_JOIN_INVITATIONAL);
			invitationCode = code.getText().toString();
			showProgress(R.string.loading_game_details);
                        new Getter<Map<String, Object>>(getApplicationContext(), MessageFormat.format(GET_INVITATIONAL_META_FORMAT, invitationCode)).
                            onResult(new HandlerDoable<Map<String, Object>>() {
                                    public void handle(Map<String, Object> data) {
                                        hideProgress();
                                        joinInvitationalMeta = data;
                                        orderPowersDoable = JOIN_INVITATIONAL_DOABLE;
                                        removeDialog(DIALOG_CUSTOM_GAME_INFORMATION);
                                        showDialog(DIALOG_CUSTOM_GAME_INFORMATION);
                                    }
                                }).
                            onError(404, new HandlerDoable<String>() {
                                    public void handle(String s) {
                                        Log.w("Droidippy", "blabla");
                                        hideProgress();
                                        toast(R.string.no_such_game);
                                    }
                                }).
                            onError(STD_ERROR_HANDLER).start();
		    }
		});
	    break;
	case DIALOG_INVITING_GAME:
	    dialog = new Dialog(this);
	    dialog.setContentView(R.layout.inviting_game_dialog);
	    invitingGameDialog = dialog;
	    fixInvitingGameDialog();
	    break;
	case DIALOG_INVITE_MEMBER:
	    dialog = new Dialog(this);
	    dialog.setTitle(R.string.invite);
	    dialog.setContentView(R.layout.invite_member_dialog);
	    final EditText email = (EditText) dialog.findViewById(R.id.invite_member_dialog_email);
	    Button button = (Button) dialog.findViewById(R.id.invite_member_dialog_invite);
	    final Game finalGame = invitingGame;
	    button.setOnClickListener(new View.OnClickListener() {
		    public void onClick(View v) {
			if (finalGame != null && email != null && email.getText() != null && !email.getText().toString().equals("")) {
			    showProgress(R.string.inviting);
			    new Poster<Object>(getApplicationContext(), MessageFormat.format(INVITE_MEMBER_FORMAT, "" + finalGame.id), email.getText().toString()).
				onResult(new HandlerDoable<Object>() {
					public void handle(Object o) {
					    hideProgress();
					    removeDialog(DIALOG_INVITE_MEMBER);
					    new AlertDialog.Builder(Droidippy.this).
						setMessage(MessageFormat.format(getResources().getString(R.string.has_been_invited), email.getText().toString())).
						setTitle(R.string.invited).
						setNeutralButton(R.string.ok, new OkClickable()).
						show();
					}
				    }).
				onError(409, new HandlerDoable<String>() {
					public void handle(String s) {
					    hideProgress();
					    removeDialog(DIALOG_INVITE_MEMBER);
					    new AlertDialog.Builder(Droidippy.this).
						setMessage(R.string.game_is_started).
						setTitle(R.string.started).
						setNeutralButton(R.string.ok, new OkClickable()).
						show();
					}
				    }).
				onError(404, new HandlerDoable<String>() {
					public void handle(String s) {
					    hideProgress();
					    removeDialog(DIALOG_INVITE_MEMBER);
					    new AlertDialog.Builder(Droidippy.this).
						setMessage(MessageFormat.format(getResources().getString(R.string.can_not_be_found), email.getText().toString())).
						setTitle(R.string.not_found).
						setNeutralButton(R.string.ok, new OkClickable()).
						show();
					}
				    }).
				onError(STD_ERROR_HANDLER).start();
			}
		    }
		});
	    break;
	case DIALOG_ORDER_POWERS:
	    final List<DragAndDropAdapter> adapterContainer = new ArrayList<DragAndDropAdapter>();
	    View.OnClickListener whenDone = new View.OnClickListener() {
		    public void onClick(View view) {
			removeDialog(DIALOG_ORDER_POWERS);
			ArrayList<String> powerPreferences = new ArrayList<String>();
			DragAndDropAdapter powerAdapter = adapterContainer.get(0);
			for (int i = 0; i < powerAdapter.getCount(); i++) {
			    powerPreferences.add((String) powerAdapter.getItem(i));
			}
			setPowerPreferences(getApplicationContext(), powerPreferences);
			orderPowersDoable.doit(powerPreferences);
		    }
		};
	    if (orderPowersDoable == CREATE_GAME_DOABLE) {
		dialog = prepareOrderPowersDialog(this, 
						  whenDone, 
						  R.string.create_game,
						  R.string.select_power_preferences_create, 
						  adapterContainer);
	    } else if (orderPowersDoable == JOIN_RANDOM_DOABLE) {
		dialog = prepareOrderPowersDialog(this, 
						  whenDone, 
						  R.string.join_game,
						  R.string.select_power_preferences_join, 
						  adapterContainer);
	    } else if (orderPowersDoable == JOIN_INVITATIONAL_DOABLE) {
		dialog = prepareOrderPowersDialog(this, 
						  whenDone, 
						  R.string.join_game,
						  R.string.select_power_preferences_join, 
						  adapterContainer);
	    } else {
		dialog = null;
	    }
	    break;
	case DIALOG_SELECT_ACCOUNT:
	    dialog = new Dialog(this, android.R.style.Theme);
	    dialog.setContentView(R.layout.select_account_dialog);
	    dialog.setTitle(R.string.select_account);
	    ListView accountsList = (ListView) dialog.findViewById(R.id.select_account_dialog_accounts);
	    ArrayList<String> accounts = new ArrayList<String>();
	    final Account[] googleAccounts = AccountManager.get(this).getAccountsByType(GOOGLE_ACCOUNT_TYPE);
	    for (Account account : googleAccounts) {
		accounts.add(account.name);
	    }
	    final ArrayAdapter<String> accountsAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, accounts);
	    accountsList.setAdapter(accountsAdapter);
	    accountsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
		    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			removeDialog(DIALOG_SELECT_ACCOUNT);
			String accountName = accountsAdapter.getItem(position);
			Account selectedAccount = null;
			for (Account account : googleAccounts) {
			    if (account.name.equals(accountName)) {
				selectedAccount = account;
			    }
			}
			if (selectedAccount != null) {
			    Droidippy.this.setAccount(selectedAccount);
			} else {
			    error(new RuntimeException("Couldn't find an account named " + accountName + " even when it was selected from a list of supposedly existing accounts?!"), true);
			}
		    }
		});
	    break;
	default:
	    dialog = null;
	}
	if (dialog != null) {
	    adjustTextViews(this, dialog);
	}
	return dialog;
    }
    
}
