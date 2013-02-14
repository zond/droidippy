package cx.ath.troja.droidippy;

import java.math.*;
import java.util.*;
import java.util.concurrent.*;
import java.io.*;
import java.text.*;

import android.content.*;
import android.graphics.drawable.*;
import android.graphics.*;
import android.util.*;

import static cx.ath.troja.droidippy.Util.*;

public class Game implements Serializable {

  public static final int CLICK_SENSITIVITY = 20;

  public static final long DEFAULT_DEADLINE = 24 * 60 * 60 * 1000;

  public static final String[] COASTS = new String[] {
    "alb",
      "ank",
      "apu",
      "arm",
      "bel",
      "ber",
      "bre",
      "bul",
      "cly",
      "con",
      "den",
      "edi",
      "fin",
      "gas",
      "gre",
      "hol",
      "kie",
      "lon",
      "lvn",
      "lvp",
      "mar",
      "naf",
      "nap",
      "nwy",
      "pic",
      "pie",
      "por",
      "pru",
      "rom",
      "rum",
      "sev",
      "smy",
      "spa",
      "stp",
      "swe",
      "syr",
      "tri",
      "tun",
      "tus",
      "ven",
      "wal",
      "yor"    
  };
  public static final Set<String> COAST_SET = new HashSet<String>();
  static {
    for (int i = 0; i < COASTS.length; i++) {
      COAST_SET.add(COASTS[i]);
    }
  }

  public static final String[] PROVINCE_NAMES_ARY = new String[] {
    "Adriatic Sea",
      "Aegean Sea",
      "Shqipëri",
      "Ankara",
      "Puglia",
      "Hayastan",
      "Baltic Sea",
      "Barents Sea",
      "Belgium",
      "Berlin",
      "Black Sea",
      "Böhmen",
      "Brest",
      "Budapest",
      "Bulgaria",
      "Bulgaria East Coast",
      "Bulgaria South Coast",
      "Bourgogne",
      "Clyde",
      "Istanbul",
      "Danmark",
      "East Mediterranean",
      "Edinburgh",
      "English Channel",
      "Suomi",
      "Galizien",
      "Gascogne",
      "Elláda",
      "Gulf of Bothnia",
      "Gulf of Lyon",
      "Helgoland Bight",
      "Holland",
      "Ionian Sea",
      "Irish Sea",
      "Kiel",
      "London",
      "Liivimaa",
      "Liverpool",
      "Marseille",
      "Mid Atlantic",
      "Moskva",
      "München",
      "North Africa",
      "Napoli",
      "North Atlantic",
      "North Sea",
      "Norge",
      "Norwegian Sea",
      "Paris",
      "Picardie",
      "Piemonte",
      "Portugal",
      "Preußen",
      "Roma",
      "Ruhr",
      "România",
      "Srbija",
      "Sevastopol",
      "Schlesien",
      "Skagerrak",
      "Izmir",
      "España",
      "España North Coast",
      "España South Coast",
      "Sankt-Petersburg",
      "Sankt-Petersburg North Coast",
      "Sankt-Petersburg South Coast",
      "Sverige",
      "Syriac",
      "Triest",
      "Tunes",
      "Toscana",
      "Tirol",
      "Tyrrhenian Sea",
      "Ukrayina",
      "Venezia",
      "Wien",
      "Wales",
      "Warzawa",
      "West Mediterranean",
      "York"
  };

  public static final String[] ARMY_COAST_PROVINCES = new String[] {
    "bul",
      "spa",
      "stp"
  };
  public static final Set<String> ARMY_COAST_PROVINCE_SET = new HashSet<String>();
  static {
    for (int i = 0; i < ARMY_COAST_PROVINCES.length; i++) {
      ARMY_COAST_PROVINCE_SET.add(ARMY_COAST_PROVINCES[i]);
    }
  }

  public static final String[] FLEET_COAST_PROVINCES = new String[] {
    "bul/ec",
      "bul/sc",
      "spa/nc",
      "spa/sc",
      "stp/nc",
      "stp/sc"
  };
  public static final Set<String> FLEET_COAST_PROVINCE_SET = new HashSet<String>();
  static {
    for (int i = 0; i < FLEET_COAST_PROVINCES.length; i++) {
      FLEET_COAST_PROVINCE_SET.add(FLEET_COAST_PROVINCES[i]);
    }
  }

  public static final String[] PROVINCES = new String[] {
    "adr",
      "aeg",
      "alb",
      "ank",
      "apu",
      "arm",
      "bal",
      "bar",
      "bel",
      "ber",
      "bla",
      "boh",
      "bre",
      "bud",
      "bul",
      "bul/ec",
      "bul/sc",
      "bur",
      "cly",
      "con",
      "den",
      "eas",
      "edi",
      "eng",
      "fin",
      "gal",
      "gas",
      "gre",
      "bot",
      "lyo",
      "hel",
      "hol",
      "ion",
      "iri",
      "kie",
      "lon",
      "lvn",
      "lvp",
      "mar",
      "mao",
      "mos",
      "mun",
      "naf",
      "nap",
      "nao",
      "nth",
      "nwy",
      "nwg",
      "par",
      "pic",
      "pie",
      "por",
      "pru",
      "rom",
      "ruh",
      "rum",
      "ser",
      "sev",
      "sil",
      "ska",
      "smy",
      "spa",
      "spa/nc",
      "spa/sc",
      "stp",
      "stp/nc",
      "stp/sc",
      "swe",
      "syr",
      "tri",
      "tun",
      "tus",
      "tyr",
      "tys",
      "ukr",
      "ven",
      "vie",
      "wal",
      "war",
      "wes",
      "yor"    
  };

  public static final Set<String> LANDLOCKED_HOME_PROVINCES = new HashSet<String>();
  static {
    LANDLOCKED_HOME_PROVINCES.add("par");
    LANDLOCKED_HOME_PROVINCES.add("mun");
    LANDLOCKED_HOME_PROVINCES.add("mos");
    LANDLOCKED_HOME_PROVINCES.add("war");
    LANDLOCKED_HOME_PROVINCES.add("vie");
    LANDLOCKED_HOME_PROVINCES.add("bud");
  }

  public static final Map<String, Set<String>> HOME_PROVINCES = new HashMap<String, Set<String>>();
  static {
    HOME_PROVINCES.put("Austria", new HashSet<String>());
    HOME_PROVINCES.get("Austria").add("vie");
    HOME_PROVINCES.get("Austria").add("bud");
    HOME_PROVINCES.get("Austria").add("tri");
    HOME_PROVINCES.put("England", new HashSet<String>());
    HOME_PROVINCES.get("England").add("edi");
    HOME_PROVINCES.get("England").add("lon");
    HOME_PROVINCES.get("England").add("lvp");
    HOME_PROVINCES.put("France", new HashSet<String>());
    HOME_PROVINCES.get("France").add("bre");
    HOME_PROVINCES.get("France").add("par");
    HOME_PROVINCES.get("France").add("mar");
    HOME_PROVINCES.put("Germany", new HashSet<String>());
    HOME_PROVINCES.get("Germany").add("kie");
    HOME_PROVINCES.get("Germany").add("ber");
    HOME_PROVINCES.get("Germany").add("mun");
    HOME_PROVINCES.put("Italy", new HashSet<String>());
    HOME_PROVINCES.get("Italy").add("ven");
    HOME_PROVINCES.get("Italy").add("rom");
    HOME_PROVINCES.get("Italy").add("nap");
    HOME_PROVINCES.put("Russia", new HashSet<String>());
    HOME_PROVINCES.get("Russia").add("stp");
    HOME_PROVINCES.get("Russia").add("mos");
    HOME_PROVINCES.get("Russia").add("war");
    HOME_PROVINCES.get("Russia").add("sev");
    HOME_PROVINCES.put("Turkey", new HashSet<String>());
    HOME_PROVINCES.get("Turkey").add("con");
    HOME_PROVINCES.get("Turkey").add("smy");
    HOME_PROVINCES.get("Turkey").add("ank");
  }

  public static final Set<String> SEAS = new HashSet<String>();
  static {
    SEAS.add("bar");
    SEAS.add("nwg");
    SEAS.add("nao");
    SEAS.add("bot");
    SEAS.add("bal");
    SEAS.add("ska");
    SEAS.add("hel");
    SEAS.add("nth");
    SEAS.add("eng");
    SEAS.add("iri");
    SEAS.add("mao");
    SEAS.add("wes");
    SEAS.add("lyo");
    SEAS.add("tys");
    SEAS.add("ion");
    SEAS.add("adr");
    SEAS.add("aeg");
    SEAS.add("eas");
    SEAS.add("bla");
  }

  public static final Map<String, String> PROVINCE_NAMES = new HashMap<String, String>();
  public static final Map<String, Region> PROVINCE_REGIONS = new HashMap<String, Region>();
  public static final Map<String, float[]> PROVINCE_COORDINATES = new HashMap<String, float[]>();
  static {
    Collection<String> missingStarting = new ArrayList<String>();
    Collection<String> missingPath = new ArrayList<String>();
    for (int i = 0; i < PROVINCES.length; i++) {
      float[] startingCoordinates = StandardMap.startingCoordinates.get(PROVINCES[i] + "Center");
      if (startingCoordinates == null) {
	missingStarting.add(PROVINCES[i] + "Center");
      } else {
	startingCoordinates[1] -= 2.7;
	startingCoordinates[0] -= 0.7;
	PROVINCE_COORDINATES.put(PROVINCES[i], startingCoordinates);
      }
      Path path = StandardMap.buildPath(PROVINCES[i] + "Fill");
      if (path == null) {
	missingPath.add(PROVINCES[i] + "Fill");
      } else {
	Region region = new Region();
	RectF bound = new RectF();
	path.computeBounds(bound, true);
	region.setPath(path, new Region((int) bound.left,
	      (int) bound.top,
	      (int) bound.right,
	      (int) bound.bottom));
	PROVINCE_REGIONS.put(PROVINCES[i], region);
      }
      PROVINCE_NAMES.put(PROVINCES[i], PROVINCE_NAMES_ARY[i]);
    }
    if (missingStarting.size() > 0 || missingPath.size() > 0) {
      throw new RuntimeException("" + StandardMap.class + " lacks startingCoordinates for " + missingStarting + " and paths for " + missingPath);
    }
  }

  public static final int[] POWER_FLAGS = new int[] {
    R.drawable.austria,
      R.drawable.england,
      R.drawable.france,
      R.drawable.germany,
      R.drawable.italy,
      R.drawable.russia,
      R.drawable.turkey
  };

  public static final String[] POWERS = new String[] {
    "Austria",
      "England",
      "France",
      "Germany",
      "Italy",
      "Russia",
      "Turkey"
  };

  public static final Map<String, Integer> POWER_FLAGS_MAP = new HashMap<String, Integer>();
  static {
    for (int i = 0; i < POWERS.length; i++) {
      POWER_FLAGS_MAP.put(POWERS[i], POWER_FLAGS[i]);
    }
  }

  public static final String[] UNIT_TYPES = new String[] {
    ARMY,
      FLEET
  };

  public static String[] unitTypes(String province) {
    if (LANDLOCKED_HOME_PROVINCES.contains(province)) {
      return new String[] { ARMY };
    } else {
      return UNIT_TYPES;
    }
  }

  public static final Map<String, Integer> DEFAULT_POWER_COLORS = new HashMap<String, Integer>();
  static {
    DEFAULT_POWER_COLORS.put(POWERS[0], new BigInteger("ccafe773",16).intValue()); // ljusgrön
    DEFAULT_POWER_COLORS.put(POWERS[1], new BigInteger("cc483c6c",16).intValue()); // mörklila
    DEFAULT_POWER_COLORS.put(POWERS[2], new BigInteger("cc5693aa",16).intValue()); // ljusblå
    DEFAULT_POWER_COLORS.put(POWERS[3], new BigInteger("ccff8b66",16).intValue()); // orange
    DEFAULT_POWER_COLORS.put(POWERS[4], new BigInteger("cc1b6c61",16).intValue()); // mörkgrön
    DEFAULT_POWER_COLORS.put(POWERS[5], new BigInteger("cc8d5e68",16).intValue()); // rosa
    DEFAULT_POWER_COLORS.put(POWERS[6], new BigInteger("ccffdb66",16).intValue()); // gul
  }

  public long id;
  public String lastPhaseName;
  public String phaseName;
  public String phaseType;    
  public long phaseOrdinal;
  public String power;
  public Map<String, Map<String, String>> positions;
  public Map<String, Order> orders;
  public List<String> results;
  public String memberState;
  public String gameState;
  public boolean detailed;
  public boolean resolved;
  public long gameUpdatedAt;
  public long gameCreatedAt;
  public long phaseCount;
  public long deadline;
  public long allUnreadMessages;
  public String intent;
  public String intentExtras;
  public String creator;
  public Long phaseLength;
  public Map<String, Long> eachUnreadMessages;
  public Map<String, Long> eachMessages;
  public List<String> members;
  public long memberCount;
  public String invitationCode;
  public boolean eliminated;
  public boolean wantsNotifications;
  public String phaseMessage;
  public boolean needsOrders;
  public int adjustmentBalance;
  public Long uncommittedPlayers;
  public List<Map<String, Object>> phaseList;
  public String privateChatSetting;
  public String conferenceChatSetting;
  public boolean stranded;
  public String gameAlias;
  public boolean allowInvalidOrders;
  public Set<String> failedOrders;
  public Long endYear;
  public String pointsSystem;
  public List<String> pointsSystems;
  public boolean dias;
  public boolean publicEmail;
  public Map<String, String> emailByPower;
  public boolean preferenceList;
  public Integer reliabilityStep;

  private Semaphore orderSemaphore = new Semaphore(1, true);

  public Game(long id) {
    this.id = id;
    this.detailed = false;
  }

  @SuppressWarnings("unchecked")
    public Game(Map<String, Object> map) {
      this(Long.parseLong((String) map.get(GAME_ID)));
      parseSmallMap(map);
    }

  @Override
    public boolean equals(Object o) {
      if (o instanceof Game) {
	return id == ((Game) o).id;
      } else {
	return false;
      }
    }

  public boolean lastPhase() {
    return phaseOrdinal >= phaseCount - 1;
  }

  public void parseSmallMap(Map<String, Object> map) {
    this.lastPhaseName = "" + map.get(LAST_PHASE_NAME);
    this.gameAlias = (String) map.get(MEMBER_GAME_ALIAS);
    this.gameUpdatedAt = Long.parseLong("" + map.get(GAME_UPDATED_AT));
    this.gameCreatedAt = Long.parseLong("" + map.get(GAME_CREATED_AT));
    this.deadline = Long.parseLong("" + map.get(PHASE_DEADLINE));
    this.allUnreadMessages = Long.parseLong("" + map.get(MEMBER_ALL_UNREAD));
    this.memberState = "" + map.get(MEMBER_STATE);
    this.gameState = "" + map.get(GAME_STATE);
    this.power = "" + map.get(MEMBER_POWER);
    this.memberCount = Long.parseLong("" + map.get(GAME_MEMBER_COUNT));
    this.creator = (String) map.get(GAME_CREATOR);
    this.eliminated = "true".equals(map.get(MEMBER_ELIMINATED));
    this.wantsNotifications = "true".equals(map.get(MEMBER_WANTS_NOTIFICATIONS));
    this.uncommittedPlayers = (Long) map.get(UNCOMMITTED_PLAYERS);
    this.phaseLength = (Long) map.get(GAME_PHASE_LENGTH);
    this.privateChatSetting = (String) map.get(GAME_PRIVATE_CHAT_SETTING);
    this.conferenceChatSetting = (String) map.get(GAME_CONFERENCE_CHAT_SETTING);
    this.stranded = "true".equals(map.get(MEMBER_STRANDED));
    this.publicEmail = "true".equals(map.get(GAME_PUBLIC_EMAIL));
    this.dias = "true".equals(map.get(GAME_DIAS));
    this.reliabilityStep = (Integer) map.get(GAME_RELIABILITY_STEP);
  }

  public void parseInvitationalData(Map<String, Object> map) {
    this.members = (List<String>) map.get(GAME_MEMBERS);
    this.invitationCode = "" + map.get(GAME_INVITATION_CODE);
    this.allowInvalidOrders = "true".equals(map.get(GAME_ALLOW_INVALID_ORDERS));
    this.pointsSystems = (List<String>) map.get(POINTS_SYSTEMS);
    this.endYear = (Long) map.get(GAME_END_YEAR);
    this.pointsSystem = (String) map.get(GAME_POINTS_SYSTEM);
    this.preferenceList = "true".equals(map.get(GAME_PREFERENCE_LIST));
  }

  public void loadInvitationalDetails(Context context, final Doable<Object> whenDone, Doable<RuntimeException> onError, Doable<String> on404) {
    Getter getter = new Getter<Map<String, Object>>(context, MessageFormat.format(GET_INVITATIONAL_DETAILS_FORMAT, "" + id)).
      onResult(new Doable<Map<String, Object>>() {
	public void doit(Map<String, Object> map) {
	  parseInvitationalData(map);
	  whenDone.doit(map);
	}
      }).onError(onError);
    if (on404 != null) {
      getter.onError(404, on404);
    }
    getter.start();
  }    

  private void loadDetails(Map<String, Object> map) {
    parseSmallMap(map);
    this.emailByPower = (Map<String, String>) map.get(GAME_EMAIL_BY_POWER);
    this.eachUnreadMessages = (Map<String, Long>) map.get(MEMBER_EACH_UNREAD);
    this.eachMessages = (Map<String, Long>) map.get(MEMBER_EACH_MESSAGES);
    this.intent = "" + map.get(MEMBER_INTENT);
    this.intentExtras = (String) map.get(MEMBER_INTENT_EXTRAS);
    this.phaseName = "" + map.get(PHASE_NAME);
    this.phaseOrdinal = Long.parseLong("" + map.get(PHASE_ORDINAL));
    this.needsOrders = "true".equals("" + map.get(MEMBER_NEEDS_ORDERS));
    this.phaseType = "" + map.get(PHASE_TYPE);
    this.phaseCount = Long.parseLong("" + map.get(GAME_PHASE_COUNT));
    this.resolved = Boolean.TRUE.toString().equals(map.get(PHASE_RESOLVED));
    this.results = (List<String>) map.get(PHASE_RESULT);
    this.positions = (Map<String, Map<String, String>>) map.get(PHASE_POSITION);
    this.failedOrders = (Set<String>) map.get(PHASE_FAILED_ORDERS);
    this.orders = parseOrders((List<String>) map.get(PHASE_ORDER));
    this.detailed = true;
    this.phaseMessage = (String) map.get(PHASE_MESSAGE);
    this.phaseList = (List<Map<String, Object>>) map.get(GAME_PHASE_LIST);
    if (ADJUSTMENT.equals(this.phaseType)) {
      this.adjustmentBalance = Integer.parseInt("" + map.get(ADJUSTMENT_BALANCE));	    
    }
  }

  private void loadDetails(String url, Context context, final Doable<Object> whenDone, Doable<RuntimeException> onError) {
    new Getter<Map<String, Object>>(context, url).
      onResult(new Doable<Map<String, Object>>() {
	public void doit(Map<String, Object> map) {
	  loadDetails(map);
	  whenDone.doit(map);
	}
      }).onError(onError).start();
  }

  public void loadPreviousPhase(Context context, Doable<Object> whenDone, Doable<RuntimeException> onError) {
    if (phaseOrdinal > 0) {
      phaseOrdinal--;
      loadDetails(MessageFormat.format(LOAD_PHASE_PATTERN, "" + id, "" + phaseOrdinal),
	  context, whenDone, onError);
    } else {
      whenDone.doit(null);
    }
  }

  public void loadNextPhase(Context context, Doable<Object> whenDone, Doable<RuntimeException> onError) {
    phaseOrdinal++;
    loadDetails(MessageFormat.format(LOAD_PHASE_PATTERN, "" + id, "" + phaseOrdinal),
	context, whenDone, onError);
  }

  public void loadDetails(Context context, Doable<Object> whenDone, Doable<RuntimeException> onError) {
    loadDetails(MessageFormat.format(LOAD_DETAILS_PATTERN, "" + id),
	context, whenDone, onError);
  }

  public void reloadPhase(Context context, Doable<Object> whenDone, Doable<RuntimeException> onError) {
    loadDetails(MessageFormat.format(LOAD_PHASE_PATTERN, "" + id, "" + phaseOrdinal),
	context, whenDone, onError);
  }

  public void setIntent(Context context, String intent, String extras, final Doable<Object> whenDone, final Doable<RuntimeException> onError) {
    this.intent = intent;
    this.intentExtras = extras;
    new Poster<Object>(context, MessageFormat.format(SET_INTENT_URL_FORMAT, "" + id), Arrays.asList(new String[] { intent, extras })).
      onResult(new Doable<Object>() {
	public void doit(Object o) {
	  whenDone.doit(o);
	}
      }).onError(onError).start();
  }

  public void commit(Context context, final Doable<String> whenDone, Doable<RuntimeException> onError) {
    new Getter<String>(context, MessageFormat.format(COMMIT_ORDERS_FORMAT, "" + id)).onResult(new Doable<String>() {
      public void doit(String o) {
	memberState = COMMITTED;
	whenDone.doit(o);
      }
    }).onError(onError).setSemaphore(orderSemaphore).start();
  }

  public void uncommit(Context context, final Doable<String> whenDone, Doable<RuntimeException> onError) {
    new Getter<String>(context, MessageFormat.format(UNCOMMIT_ORDERS_FORMAT, "" + id)).onResult(new Doable<String>() {
      public void doit(String o) {
	memberState = UNCOMMITTED;
	whenDone.doit(o);
      }
    }).onError(onError).setSemaphore(orderSemaphore).start();
  }

  public String classify(String order) {
    String[] replacements = new String[] {
      "Army", "A",
	"Fleet", "F",
	"France", "F",
	"England", "E",
	"Germany", "G",
	"Italy", "I",
	"Austria", "A",
	"Turkey", "T",
	"Russia", "R",
	"remove", "R",
	"move", "M",
	"support", "S",
	"convoy", "C",
	"build", "B",
	"disband", "D",
	"hold", "H",
	"Remove", "R",
	"Move", "M",
	"Support", "S",
	"Convoy", "C",
	"Build", "B",
	"Disband", "D",
	"Hold", "H"
    };
    for (int i = 0; i < replacements.length; i += 2) {
      order = ra(order, replacements[i], replacements[i + 1]);
    }
    order = order.
      replaceAll("([^a-zA-Z]+[a-zA-Z]{3,3}(/.c)?) M ([a-zA-Z]{3,3}(/.c)?[^a-zA-Z]+)", "$1-$3").
      replaceAll("^([a-zA-Z]{3,3}(/.c)?) M ([a-zA-Z]{3,3}(/.c)?[^a-zA-Z]+)", "$1-$3").
      replaceAll("([^a-zA-Z]+[a-zA-Z]{3,3}(/.c)?) M ([a-zA-Z]{3,3}(/.c)?)$", "$1-$3");
    return order;
  }

  private String ra(String source, String what, String rep) {
    return source.
      replaceAll("(\\W+)" + what + "(\\W+)", "$1" + rep + "$2").
      replaceAll("^" + what + "(\\W+)", rep + "$1").
      replaceAll("(\\W+)" + what + "$", "$1" + rep);
  }

  public String provincify(String result) {
    for (Map.Entry<String, String> entry : PROVINCE_NAMES.entrySet()) {
      result = ra(result, entry.getKey(), entry.getValue());
    }
    return result;
  }

  public String renderResults(Context context) {
    StringBuffer returnValue = new StringBuffer();
    List<String> orderedResults = new ArrayList<String>(results);
    Collections.sort(orderedResults);
    for (String result : orderedResults) {
      if (getClassicalOrders(context)) {
	returnValue.append(classify(result.trim())).append("\n");
      } else {
	returnValue.append(provincify(result.trim())).append("\n");
      }
    }
    return returnValue.toString();
  }

  public String renderOrders(Context context) {
    StringBuffer returnValue = new StringBuffer();
    List<Order> sortedOrders = new ArrayList<Order>(orders.values());
    Collections.sort(sortedOrders, new Comparator<Order>() {
      public int compare(Order o1, Order o2) {
	return o1.toFullString().compareTo(o2.toFullString());
      }
    });
    for (Order order : sortedOrders) {
      if (getClassicalOrders(context)) {
	returnValue.append(classify(order.toFullString())).append("\n");
      } else {
	returnValue.append(provincify(order.toFullString())).append("\n");
      }
    }
    return returnValue.toString();
  }

  private Map<String, Order> parseOrders(List<String> orderTexts) {
    Map<String, Order> returnValue = new HashMap<String, Order>();
    for (String text : orderTexts) {
      try {
	Order order = Order.parse(this, text);
	if (order != null) {
	  if (failedOrders != null && failedOrders.contains(order.getSourceProvince())) {
	    order.failed = true;
	  } 
	  returnValue.put(order.getSourceProvince(), order);
	}
      } catch (Exception e) {
	Log.w(PACKAGE_NAME, "Unable to parse '" + text + "'", e);
      }
    }
    return returnValue;
  }

  @SuppressWarnings("unchecked")
    public void execute(Context context, Order order, final Doable<Object> whenDone, Doable<RuntimeException> onError, final Doable<String> onMessage) {
      Map<String, String> data = new HashMap<String, String>();
      data.put(GAME_ID, "" + id);
      data.put(ORDER_SOURCE_PROVINCE, order.getSourceProvince());
      data.put(ORDER_POWER, order.getPower());
      data.put(ORDER_TEXT, order.toFullString());

      new Poster<Object>(context, SEND_ORDER_URL, data).
	onResult(new Doable<Object>() {
	  public void doit(Object result) {
	    if (result instanceof List) {
	      orders = parseOrders((List<String>) result);
	      whenDone.doit(result);
	    } else if (result instanceof Map) {
	      Map<String, Object> map = (Map<String, Object>) result;
	      orders = parseOrders((List<String>) map.get("orders"));
	      onMessage.doit((String) map.get("message"));
	    } else {
	      throw new RuntimeException("Unknown result type for " + SEND_ORDER_URL + ": " + result);
	    }
	  }
	}).
      onError(onError).
	setSemaphore(orderSemaphore).
	onError(400, onMessage).start();
    }

  public Drawable getMap(Context context) {
    Class x = StandardMap.class;
    Log.d(PACKAGE_NAME, "StandardMap.class is " + x);
    return StandardMap.create(context, new float[] { 0, 0 }, getMapConfig(context), null, null);
  }

  private String province(float x, float y, boolean needsUnit, Boolean army, boolean guesstimate) {
    String bestProvince = null;
    float bestDistance = 0f;
    for (Map.Entry<String, Region> entry : PROVINCE_REGIONS.entrySet()) {
      boolean provinceOk = ((Boolean.TRUE.equals(army) && !FLEET_COAST_PROVINCE_SET.contains(entry.getKey())) ||
	  (Boolean.FALSE.equals(army) && !ARMY_COAST_PROVINCE_SET.contains(entry.getKey())) ||
	  (army == null && getUnitPowerAndType(entry.getKey()) != null));
      if (provinceOk) {
	if (entry.getValue().contains((int) x,
	      (int) y)) {
	  return entry.getKey();
	} else if (guesstimate) {
	  float distance = new Poi(PROVINCE_COORDINATES.get(entry.getKey())).sub(new Poi(x, y)).len();
	  if (bestProvince == null || distance < bestDistance) {
	    bestProvince = entry.getKey();
	    bestDistance = distance;
	  }
	}
      }
    }
    return bestProvince;
  }

  public String getProvinceAsDestination(float x, float y, String unitType) {
    return province(x, y, false, ARMY.equals(unitType), true);
  }

  public String getProvinceWithUnit(float x, float y) {
    return province(x, y, true, null, true);
  }

  public String getExactProvinceWithUnit(float x, float y) {
    return province(x, y, true, null, false);
  }

  public String[] getUnitPowerAndType(String province) {
    if (positions != null) {
      Map<String, String> position = positions.get(province);
      if (position != null) {
	String armyPower = position.get(TYPE_ARMY);
	String fleetPower = position.get(TYPE_FLEET);
	if (armyPower != null && fleetPower != null) {
	  throw new RuntimeException("a province can only have one unit!");
	} else if (armyPower != null) {
	  return new String[] { armyPower, ARMY };
	} else if (fleetPower != null) {
	  return new String[] { fleetPower, FLEET };
	} else {
	  return null;
	}
      } else {
	return null;
      }
    } else {
      return null;
    }
  }

  public String[] getDislodgedPowerAndType(String province) {
    if (positions != null) {
      Map<String, String> position = positions.get(province);
      if (position != null) {
	String armyPower = position.get(TYPE_ARMY_DIS);
	String fleetPower = position.get(TYPE_FLEET_DIS);
	if (armyPower != null && fleetPower != null) {
	  throw new RuntimeException("a province can only have one dislodged unit!");
	} else if (armyPower != null) {
	  return new String[] { armyPower, ARMY };
	} else if (fleetPower != null) {
	  return new String[] { fleetPower, FLEET };
	} else {
	  return null;
	}
      } else {
	return null;
      }
    } else {
      return null;
    }
  }

  public String getSupplyPower(String province) {
    if (positions != null) {
      Map<String, String> position = positions.get(province);
      if (position != null) {
	return position.get(TYPE_SUPPLY);
      } else {
	return null;
      }
    } else {
      return null;
    }
  }

  private void addEachOrder(String province, List<Order> orders) {
    orders.add(new Order.Remove(this, province));
    orders.add(new Order.Convoy(this, province));
    orders.add(new Order.Hold(this, province));
    orders.add(new Order.Move(this, province));
    orders.add(new Order.MoveViaConvoy(this, province));
    orders.add(new Order.Support(this, province));
    orders.add(new Order.Disband(this, province));
    orders.add(new Order.Retreat(this, province));
    orders.add(new Order.Cancel(this, province));
  }

  public List<Order> getOrders(float x, float y) {
    List<Order> orders = new ArrayList<Order>();
    if ((DEBUG_MODE || needsOrders) && !resolved && STARTED.equals(gameState)) {
      String fleetProvince = getProvinceAsDestination(x, y, FLEET);
      String armyProvince = getProvinceAsDestination(x, y, ARMY);
      if (armyProvince != null) {
	orders.add(new Order.Build(this, armyProvince, fleetProvince));
	addEachOrder(armyProvince, orders);
      }
      if (fleetProvince != null) {
	addEachOrder(fleetProvince, orders);
      }
      Iterator<Order> iterator = orders.iterator();
      while (iterator.hasNext()) {
	Order o = null;
	try {
	  iterator.next().validate();
	} catch (OrderCreationException e) {
	  iterator.remove();
	}
      }
      Set<Class> uniqueSet = new HashSet<Class>();
      iterator = orders.iterator();
      while (iterator.hasNext()) {
	Order order = iterator.next();
	if (uniqueSet.contains(order.getClass())) {
	  iterator.remove();
	} else {
	  uniqueSet.add(order.getClass());
	}
      }
    }
    return orders;
  }

  public List<Drawable> getUnits(Context context, float width, float height) {
    List<Drawable> returnValue = new ArrayList<Drawable>();
    for (int i = 0; i < PROVINCES.length; i++) {
      String[] powerAndType = getUnitPowerAndType(PROVINCES[i]);
      if (powerAndType != null) {
	float[] position = PROVINCE_COORDINATES.get(PROVINCES[i]);
	if (ARMY.equals(powerAndType[1])) {
	  returnValue.add(Unit.createArmy(context, position[0], position[1], getColor(context, powerAndType[0]), false, width, height, Color.BLACK));
	} else if (FLEET.equals(powerAndType[1])) {
	  returnValue.add(Unit.createFleet(context, position[0], position[1], getColor(context, powerAndType[0]), false, width, height, Color.BLACK));
	} else {
	  throw new RuntimeException("unknown unit type " + powerAndType[1]);
	}
      }
      powerAndType = getDislodgedPowerAndType(PROVINCES[i]);
      if (powerAndType != null) {
	float[] position = PROVINCE_COORDINATES.get(PROVINCES[i]);
	if (ARMY.equals(powerAndType[1])) {
	  returnValue.add(Unit.createArmy(context, position[0], position[1], getColor(context, powerAndType[0]), true, width, height, Color.BLACK));
	} else if (FLEET.equals(powerAndType[1])) {
	  returnValue.add(Unit.createFleet(context, position[0], position[1], getColor(context, powerAndType[0]), true, width, height, Color.BLACK));
	} else {
	  throw new RuntimeException("unknown unit type " + powerAndType[1]);
	}
      }
    }
    return returnValue;
  }

  private Map<String, Object> getMapConfig(Context context) {
    Map<String, Object> returnValue = new HashMap<String, Object>();
    if (positions != null) {
      for (int i = 0; i < PROVINCES.length; i++) {
	Map<String, String> position = positions.get(PROVINCES[i]);
	String pathCode = PROVINCES[i] + "Fill";
	if (position == null) {
	  returnValue.put(pathCode, HIDE);
	} else {
	  String holder = position.get(TYPE_SUPPLY);
	  if (holder == null) {
	    returnValue.put(pathCode, HIDE);
	  } else {
	    returnValue.put(pathCode, getColor(context, holder));
	  }
	}
      }
    }
    return returnValue;
  }

  public String timeLeft() {
    StringBuffer returnValue = new StringBuffer();
    if (PROBATION.equals(memberState)) {
      returnValue.append("probation");
    } else {
      if (BaseActivity.absoluteTimes) {
	returnValue.append(toAbsoluteTimeString(deadline));
      } else {
	returnValue.append(toRelativeTimeString(deadline));
	if (BaseActivity.longGameData) {
	  returnValue.append(" left");
	}
      }
    }
    return returnValue.toString();
  }

  public String uncommitted() {
    StringBuffer returnValue = new StringBuffer("" + uncommittedPlayers);
    if (BaseActivity.longGameData) {
      returnValue.append(" uncom");
    }
    return returnValue.toString();
  }

  public String unread() {
    StringBuffer returnValue = new StringBuffer("" + allUnreadMessages);
    if (BaseActivity.longGameData) {
      returnValue.append(" unread");
    }
    return returnValue.toString();
  }

  public String getVariant() {
    if (CHAT_OFF.equals(privateChatSetting)) {
      if (CHAT_OFF.equals(conferenceChatSetting)) {
	return "gunboat";
      } else {
	return "custom";
      }
    } else if (CHAT_ANON.equals(privateChatSetting)) {
      if (CHAT_ANON.equals(conferenceChatSetting)) {
	return "anon";
      } else {
	return "custom";
      }
    } else if (CHAT_ON.equals(privateChatSetting)) {
      if (CHAT_ON.equals(conferenceChatSetting)) {
	return null;
      } else {
	return "custom";
      }
    } else {
      return null;
    }
  }

  public String toShortString() {
    StringBuffer returnValue = new StringBuffer();
    if (gameAlias != null) {
      returnValue.append(gameAlias).append("\n");
    }
    if (NEW.equals(gameState)) {
      returnValue.append(MessageFormat.format(MEMBERS_OF_MAX_FORMAT, "" + memberCount, "" + MAX_PLAYERS));
    } else {
      if (phaseName != null) {
	returnValue.append(power).append(", ").append(phaseName);
      } else {
	returnValue.append(power).append(", ").append(lastPhaseName);
      }
    }
    String variant = getVariant();
    if (variant != null) {
      returnValue.append(" (").append(variant).append(")");
    }
    return returnValue.toString();
  }

  public String toString() {
    StringBuffer returnValue = new StringBuffer(toShortString()).append("\n<");
    if (NEW.equals(gameState)) {
      if (BaseActivity.longGameData) {
	returnValue.append("created ");
      }
      returnValue.append(DateFormat.getDateInstance().format(new Date(gameCreatedAt)));
    } else {
      if (FINISHED.equals(gameState)) {
	if (BaseActivity.longGameData) {
	  returnValue.append("ended ");
	}
	returnValue.append(DateFormat.getDateInstance().format(new Date(gameUpdatedAt)));
      } else {
	returnValue.append(timeLeft());
      }
      returnValue.append(", ").append(uncommitted());
      returnValue.append(", ").append(unread());
      if ((eliminated || stranded || FINISHED.equals(gameState)) && wantsNotifications) {
	returnValue.append(", unsilenced");
      } else if (!wantsNotifications) {
	returnValue.append(", silenced");
      }
    }
    if (phaseLength.longValue() != DEFAULT_DEADLINE) {
      returnValue.append(", ").append(toRelativeTimeString(phaseLength));
    }
    returnValue.append(">");
    return returnValue.toString();
  }

}
