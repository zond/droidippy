package cx.ath.troja.droidippy;

import android.graphics.*;
import android.graphics.drawable.*;
import android.content.*;

import java.util.*;
import java.util.regex.*;
import java.io.*;

import static cx.ath.troja.droidippy.Util.*;

public abstract class Order implements Serializable {

    public static final List<Class> SUBCLASSES = new ArrayList<Class>();
    static {
	SUBCLASSES.add(Build.class);
	SUBCLASSES.add(Remove.class);
	SUBCLASSES.add(Convoy.class);
	SUBCLASSES.add(Cancel.class);
	SUBCLASSES.add(Hold.class);
	SUBCLASSES.add(Move.class);
	SUBCLASSES.add(Support.class);
	SUBCLASSES.add(Disband.class);
	SUBCLASSES.add(Retreat.class);
    }

    public static class Build extends Order {
	public static final Pattern pattern = Pattern.compile("^([^\\W:]+): build (\\S+) (\\S+)$");
	private String fleetSource;
	public Build(Game game, Matcher matcher) {
	    super(game);
	    if (matcher.matches()) {
		supplyPower = matcher.group(1);
		parameters[0] = matcher.group(2);
		source = matcher.group(3);
	    } else {
		throw new OrderCreationException(matcher + " is not valid " + this.getClass());
	    }
	}
	@Override
	public String getHelpText() {
	    return "After every fall phase is completed the number of supply centers (provinces with grey blips) is compared to the number of units (armies and fleets) of each player. If the number of supply centers is greater than the number of units the player will be able to build new units in his or her unoccupied home provinces (the provinces the player began the game with). See http://en.wikibooks.org/wiki/Diplomacy/Rules#Game_Phases for more information.";
	}
	public Build(Game game, String armySource, String fleetSource) { 
	    super(game, armySource); 
	    this.fleetSource = fleetSource; 
	}
	protected Object[] getParameterTypes() { return new Object[] { UNIT_TYPE }; }
	protected int[] getParameterDescriptionResources() { return new int[0]; }
	public int getTypeResource() { return R.string.build; }
	public String toFullString() { 
	    if (fleetSource != null && FLEET.equals(parameters[0])) {
		return supplyPower + ": build " + parameters[0] + " " + fleetSource;
	    } else {
		return supplyPower + ": build " + parameters[0] + " " + source;
	    }
	}
	public String getPower() { return supplyPower; }
	protected void validate() { 
	    if (!game.phaseType.equals(ADJUSTMENT)) {
		throw new OrderCreationException("" + this.getClass() + " can only be created during " + ADJUSTMENT + " phases");
	    }
	    if (game.adjustmentBalance < 1) {
		throw new OrderCreationException("" + this.getClass() + " can only be created during " + ADJUSTMENT + " phases where you have something to build");
	    }
	    if (supplyPower == null) {
		throw new OrderCreationException("" + this.getClass() + " can only be created for an owned province (" + source + ")");
	    }
	    if (!Game.HOME_PROVINCES.get(supplyPower).contains(source)) {
		throw new OrderCreationException("" + this.getClass() + " can only be created in home provinces (" + supplyPower + ", " + source + ")");
	    }
	    if (!DEBUG_MODE && !game.power.equals(supplyPower)) {
		throw new OrderCreationException("" + this.getClass() + " can only be created for owner of province (" + source + ")");
	    }
	}
	public void setNext(float[] point) { throw new RuntimeException("should not be called"); }
	public void addToLayers(Context context, List<Drawable> layers, Poi mapDimensions) {
	    float[] coords = null;
	    if (fleetSource != null && FLEET.equals(parameters[0])) {
		coords = Game.PROVINCE_COORDINATES.get(fleetSource);
	    } else {
		coords = Game.PROVINCE_COORDINATES.get(source);
	    }
	    if (ARMY.equals(parameters[0])) {
		layers.add(Unit.createArmy(context, coords[0], coords[1], 0xcc << 24, false, mapDimensions.x, mapDimensions.y, Color.WHITE));
	    } else if (FLEET.equals(parameters[0])) {
		layers.add(Unit.createFleet(context, coords[0], coords[1], 0xcc << 24, false, mapDimensions.x, mapDimensions.y, Color.WHITE));
	    }
	}
    }
    public static class Remove extends Order {
	public static final Pattern pattern = Pattern.compile("^([^\\W:]+): remove (\\S+) (\\S+)$");
	public Remove(Game game, Matcher matcher) {
	    super(game);
	    if (matcher.matches()) {
		unitPower = matcher.group(1);
		unitType = matcher.group(2);
		source = matcher.group(3);
	    } else {
		throw new OrderCreationException(matcher + " is not valid " + this.getClass());
	    }
	}
	@Override
	public String getHelpText() {
	    return "After every fall phase is completed the number of supply centers (provinces with grey blips) is compared to the number of units (armies and fleets) of each player. If the number of supply centers is greater than the number of units the player will be able to build new units in his or her unoccupied home provinces (the provinces the player began the game with). See http://en.wikibooks.org/wiki/Diplomacy/Rules#Game_Phases for more information.";
	}
	public Remove(Game game, String source) { super(game, source); }
	protected Object[] getParameterTypes() { return new Object[0]; }
	protected int[] getParameterDescriptionResources() { return new int[0]; }
	public String getPower() { return unitPower; }
	public int getTypeResource() { return R.string.remove; }
	public String toFullString() { return unitPower + ": remove " + unitType + " " + source; }
	protected void validate() { 
	    if (!game.phaseType.equals(ADJUSTMENT)) {
		throw new OrderCreationException("" + this.getClass() + " can only be created during " + ADJUSTMENT + " phases");
	    }
	    if (game.adjustmentBalance > -1) {
		throw new OrderCreationException("" + this.getClass() + " can only be created during " + ADJUSTMENT + " phases where you must remove units");
	    }
	    if (unitType == null) {
		throw new OrderCreationException("" + this.getClass() + " can only be for province with unit (" + source + ")");
	    }
	    if (!DEBUG_MODE && !game.power.equals(unitPower)) {
		throw new OrderCreationException("" + this.getClass() + " can only be created for owner of unit (" + unitPower + ")");
	    }
	}
	public void setNext(float[] point) { throw new RuntimeException("should not be called"); }
	public void addToLayers(Context context, List<Drawable> layers, Poi mapDimensions) {
	    layers.add(new Cross(Game.PROVINCE_COORDINATES.get(source),
				 getColor(context, unitPower)));
	}
    }
    public static class Convoy extends Order {
	public static final Pattern pattern = Pattern.compile("^([^\\W:]+): (\\S+) (\\S+) convoy \\S+ (\\S+) move (\\S+)$");
	public Convoy(Game game, Matcher matcher) {
	    super(game);
	    if (matcher.matches()) {
		unitPower = matcher.group(1);
		unitType = matcher.group(2);
		source = matcher.group(3);
		parameters[0] = matcher.group(4);
		parameters[1] = matcher.group(5);
	    } else {
		throw new OrderCreationException(matcher + " is not valid " + this.getClass());
	    }
	}
	public Convoy(Game game, String source) { super(game, source); }
	@Override
	public String getHelpText() {
	    return "This move is used to transfer army units across sea spaces, or to move large distances in one move. Only armies may be convoyed, and only fleets may convoy. See http://en.wikibooks.org/wiki/Diplomacy/Rules#Convoy for more information.";
	}
	protected Object[] getParameterTypes() { return new Object[] { PROVINCE, PROVINCE }; }
	protected int[] getParameterDescriptionResources() { return new int[] { R.string.select_from_where_to_convoy, R.string.select_where_to_convoy }; }
	public String getPower() { return unitPower; }
	public int getTypeResource() { return R.string.convoy; }
	public String toFullString() { 
	    String[] powerAndTypeAtLoc = game.getUnitPowerAndType(parameters[0]);
	    return unitPower + ": " + unitType + " " + source + " convoy " + powerAndTypeAtLoc[1] + " " + parameters[0] + " move " + parameters[1]; 
	}
	public void setNext(float[] point) { 
	    setNext(game.getProvinceAsDestination(point[0], point[1], ARMY));
	}
	protected void validate() { 
	    if (!game.phaseType.equals(MOVEMENT)) {
		throw new OrderCreationException("" + this.getClass() + " can only be created during " + MOVEMENT + " phases");
	    }
	    if (!DEBUG_MODE && !game.power.equals(unitPower)) {
		throw new OrderCreationException("" + this.getClass() + " can only be created for owner of unit (" + unitPower + ")");
	    }
	    if (unitType == null) {
		throw new OrderCreationException("" + this.getClass() + " can only be for province with unit (" + source + ")");
	    }
	    if (!FLEET.equals(unitType)) {
		throw new OrderCreationException("" + this.getClass() + " can only be created for fleet units (" + unitType + ")");
	    }
	    if (!Game.SEAS.contains(source)) {
		throw new OrderCreationException("" + this.getClass() + " can only be issued for units at sea (" + source + ")");
	    }
	    if (parameters[0] != null) {
		String[] powerAndTypeAtLoc = game.getUnitPowerAndType(parameters[0]);
		if (powerAndTypeAtLoc == null || !powerAndTypeAtLoc[1].equals(ARMY)) {
		    throw new OrderParamException("must select convoy source with army", R.string.must_select_convoy_source_with_army);
		}
	    }
	}
	public void addToLayers(Context context, List<Drawable> layers, Poi mapDimensions) {
	    if (parameters[1] != null) {
		layers.add(new Box(Game.PROVINCE_COORDINATES.get(source), 5, getColor(context, unitPower)));
		layers.add(new Arrow(new Poi(Game.PROVINCE_COORDINATES.get(parameters[0])), 
				     new Poi(Game.PROVINCE_COORDINATES.get(source)),
				     new Poi(Game.PROVINCE_COORDINATES.get(parameters[1])),
				     getColor(context, unitPower)).failed(failed));
	    } else if (parameters[0] != null) {
		layers.add(new Box(Game.PROVINCE_COORDINATES.get(source), 5, getColor(context, unitPower)));
		layers.add(new Arrow(new Poi(Game.PROVINCE_COORDINATES.get(parameters[0])), 
				     new Poi(Game.PROVINCE_COORDINATES.get(source)),
				     getColor(context, unitPower)).failed(failed));
	    }
	}
    }
    public static class Cancel extends Order {
	public Cancel(Game game, String source) { super(game, source); }
	protected Object[] getParameterTypes() { return new Object[0]; }
	protected int[] getParameterDescriptionResources() { return new int[0]; }
	public String getPower() { 
	    if (game.orders.containsKey(source)) {
		return game.orders.get(source).getPower();
	    } else {
		return null;
	    }
	}
	public int getTypeResource() { return R.string.cancel; }
	public String toFullString() { return null; }
	protected void validate() { 
	    if (!game.orders.containsKey(source)) {
		throw new OrderCreationException("" + this.getClass() + " can only be created for " + source + " when there is an order there");
	    }
	    if (!DEBUG_MODE && !game.power.equals(game.orders.get(source).getPower())) {
		throw new OrderCreationException("" + this.getClass() + " can only be created when " + game.power + " is the owner of the order at " + source);
	    }
	}
	public void setNext(float[] point) { throw new RuntimeException("should not be called"); }
	public void addToLayers(Context context, List<Drawable> layers, Poi mapDimensions) { }
    }
    public static class Hold extends Order {
	public static final Pattern pattern = Pattern.compile("^([^\\W:]+): (\\S+) (\\S+) hold$");
	public Hold(Game game, Matcher matcher) {
	    super(game);
	    if (matcher.matches()) {
		unitPower = matcher.group(1);
		unitType = matcher.group(2);
		source = matcher.group(3);
	    } else {
		throw new OrderCreationException(matcher + " is not valid " + this.getClass());
	    }
	}
	public Hold(Game game, String source) { super(game, source); }
	protected Object[] getParameterTypes() { return new Object[0]; }
	@Override
	public String getHelpText() {
	    return "This is the default for all units (what they will do if not given any other orders). The unit will stay in its position, and will not move, support, convoy, or do anything. Holding units can be supported by units in neighboring provinces or be attacked by foreign units. If the attacking unit has more units supporting it than the holding unit, the holding unit is ousted from that province and must either retreat or disband. See http://en.wikibooks.org/wiki/Diplomacy/Rules#Hold for more information.";
	}
	protected int[] getParameterDescriptionResources() { return new int[0]; }
	public String getPower() { return unitPower; }
	public int getTypeResource() { return R.string.hold; }
	public String toFullString() { return unitPower + ": " + unitType + " " + source + " hold"; }
	protected void validate() { 
	    if (!game.phaseType.equals(MOVEMENT)) {
		throw new OrderCreationException("" + this.getClass() + " can only be created during " + MOVEMENT + " phases");
	    }
	    if (unitType == null) {
		throw new OrderCreationException("" + this.getClass() + " can only be for province with unit (" + source + ")");
	    }
	    if (!DEBUG_MODE && !game.power.equals(unitPower)) {
		throw new OrderCreationException("" + this.getClass() + " can only be created for owner of unit (" + unitPower + ")");
	    }
	}
	public void setNext(float[] point) { throw new RuntimeException("should not be called"); }
	public void addToLayers(Context context, List<Drawable> layers, Poi mapDimensions) {
	    layers.add(new Box(Game.PROVINCE_COORDINATES.get(source), 4, getColor(context, unitPower)));
	}
    }
    public static class MoveViaConvoy extends Move {
	public static final Pattern pattern = Pattern.compile("^([^\\W:]+): (\\S+) (\\S+) move (\\S+) via convoy$");
	public MoveViaConvoy(Game game, Matcher matcher) {
	    super(game, matcher);
	}
	public MoveViaConvoy(Game game, String source) { super(game, source); }
	public String getHelpText() {
	    return "This order moves the unit in one province to an adjacent province via a convoy.\n" + 
		"This is allows you to switch two adjacent armies if one of them is convoyed with a fleet.\n" +
		"A unit may not move into a province held by another unit unless it has support. As units may be supported either in attacking a province or in holding a province, the attacking unit must have more support than the defending unit if the attack is to be successful. If the attack is not successful, the attacking unit does not move anywhere. See http://en.wikibooks.org/wiki/Diplomacy/Rules#Attack.2FMove or more information.";
	}
	protected void validate() {
	    super.validate();
	    if (!Game.COAST_SET.contains(source) || !unitType.equals(ARMY)) {
		throw new OrderCreationException("" + this.getClass() + " can only be created for armies on coasts");
	    }
	}
	public String toFullString() { return unitPower + ": " + unitType + " " + source + " move " + parameters[0] + " via convoy"; }
	public int getTypeResource() { return R.string.move_via_convoy; }
	public void addToLayers(Context context, List<Drawable> layers, Poi mapDimensions) {
	    if (parameters[0] != null) {
		layers.add(new Box(Game.PROVINCE_COORDINATES.get(source), 5, getColor(context, unitPower)));
		layers.add(new Arrow(new Poi(Game.PROVINCE_COORDINATES.get(source)), 
				     new Poi(Game.PROVINCE_COORDINATES.get(parameters[0])),
				     getColor(context, getPower())).failed(failed));
	    }
	}
    }
    public static class Move extends Order {
	public static final Pattern pattern = Pattern.compile("^([^\\W:]+): (\\S+) (\\S+) move (\\S+)$");
	public Move(Game game, Matcher matcher) {
	    super(game);
	    if (matcher.matches()) {
		unitPower = matcher.group(1);
		unitType = matcher.group(2);
		source = matcher.group(3);
		parameters[0] = matcher.group(4);
	    } else {
		throw new OrderCreationException(matcher + " is not valid " + this.getClass());
	    }
	}
	public Move(Game game, String source) { super(game, source); }
	protected Object[] getParameterTypes() { return new Object[] { PROVINCE }; }
	@Override
	public String getHelpText() {
	    return "This order moves the unit in one province to an adjacent province. Of course, armies cannot move into sea provinces, and fleets cannot move into landlocked provinces.\n" + 
		"A unit may not move into a province held by another unit unless it has support. As units may be supported either in attacking a province or in holding a province, the attacking unit must have more support than the defending unit if the attack is to be successful. If the attack is not successful, the attacking unit does not move anywhere. See http://en.wikibooks.org/wiki/Diplomacy/Rules#Attack.2FMove or more information.";
	}
	public String getPower() { return unitPower; }
	protected int[] getParameterDescriptionResources() { return new int[] { R.string.select_where_to_move }; }
	public int getTypeResource() { return R.string.move; }
	public String toFullString() { return unitPower + ": " + unitType + " " + source + " move " + parameters[0]; }
	protected void validate() { 
	    if (!game.phaseType.equals(MOVEMENT)) {
		throw new OrderCreationException("" + this.getClass() + " can only be created during " + MOVEMENT + " phases");
	    }
	    if (unitType == null) {
		throw new OrderCreationException("" + this.getClass() + " can only be for province with unit (" + source + ")");
	    }
	    if (!DEBUG_MODE && !game.power.equals(unitPower)) {
		throw new OrderCreationException("" + this.getClass() + " can only be created for owner of unit (" + unitPower + ")");
	    }
	}
	public void setNext(float[] point) { 
	    setNext(game.getProvinceAsDestination(point[0], point[1], unitType));
	}
	public void addToLayers(Context context, List<Drawable> layers, Poi mapDimensions) {
	    if (parameters[0] != null) {
		layers.add(new Arrow(new Poi(Game.PROVINCE_COORDINATES.get(source)), 
				     new Poi(Game.PROVINCE_COORDINATES.get(parameters[0])),
				     getColor(context, getPower())).failed(failed));
	    }
	}
    }
    public static class Support extends Order {
	private String supportedType;
	public static final Pattern pattern = Pattern.compile("^([^\\W:]+): (\\S+) (\\S+) support (\\S+) (\\S+)( move (\\S+))?$");
	public Support(Game game, Matcher matcher) {
	    super(game);
	    if (matcher.matches()) {
		unitPower = matcher.group(1);
		unitType = matcher.group(2);
		source = matcher.group(3);
		supportedType = matcher.group(4);
		parameters[0] = matcher.group(5);
		if (matcher.group(6) != null) {
		    parameters[1] = matcher.group(7);
		} else {
		    parameters[1] = parameters[0];
		}
	    } else {
		throw new OrderCreationException(matcher + " is not valid " + this.getClass());
	    }
	}
	public Support(Game game, String source) { super(game, source); }
	protected Object[] getParameterTypes() { return new Object[] { PROVINCE, PROVINCE }; }
	public String getPower() { return unitPower; }
	protected int[] getParameterDescriptionResources() { return new int[] { R.string.select_what_unit_to_support, R.string.select_where_to_support_it }; }
	@Override
	public String getHelpText() {
	    return "Support is the trickiest aspect of the rules, and the most important of the game. Support may involve cooperation between two (or more) powers, and is the only way to make forward progress through enemy territory (unless you can convince the enemy to let you in). Simply put, more support defeats less support.\n" + 
		"The support order is given in reference to another unit's move. That other unit's move must be to a province into which the supporting unit could otherwise move. Support may also be given to a unit holding its position. In addition, units giving support can themselves be supported in their holding position.\n" + 
		"Support is a unit's sole action for a given move, and supporting units remain where they are (unless they are attacked by greater support and have to retreat or disband during the retreat phase). See http://en.wikibooks.org/wiki/Diplomacy/Rules#Support for more information";
	}
	public int getTypeResource() { return R.string.support; }
	public String toFullString() { 
	    if (parameters[0].equals(parameters[1])) {
		return unitPower + ": " + unitType + " " + source + " support " + supportedType + " " + parameters[0];
	    } else {
		return unitPower + ": " + unitType + " " + source + " support " + supportedType + " " + parameters[0] + " move " + parameters[1];
	    }
	}
	protected void validate() { 
	    if (!game.phaseType.equals(MOVEMENT)) {
		throw new OrderCreationException("" + this.getClass() + " can only be created during " + MOVEMENT + " phases");
	    }
	    if (unitType == null) {
		throw new OrderCreationException("" + this.getClass() + " can only be for province with unit (" + source + ")");
	    }
	    if (!DEBUG_MODE && !game.power.equals(unitPower)) {
		throw new OrderCreationException("" + this.getClass() + " can only be created for owner of unit (" + unitPower + ")");
	    }
	    if (parameters[0] != null) {
		String[] powerAndTypeAtLoc = game.getUnitPowerAndType(parameters[0]);
		if (powerAndTypeAtLoc == null) {
		    throw new OrderParamException("must select support source with unit", R.string.must_select_support_source_with_unit);
		}
	    }
	}
	public void setNext(float[] point) { 
	    if (nextParam == 0) {
		String unitProvince = game.getProvinceWithUnit(point[0], point[1]);
		if (unitProvince == null) {
		    throw new OrderParamException("must select support source with unit", R.string.must_select_support_source_with_unit);
		}
		String[] powerAndTypeAtLoc = game.getUnitPowerAndType(unitProvince);
		supportedType = powerAndTypeAtLoc[1];
		setNext(unitProvince);
	    } else {
		String unitProvince = game.getExactProvinceWithUnit(point[0], point[1]);
		if (unitProvince != null && unitProvince.equals(parameters[0])) {
		    setNext(unitProvince);
		} else {
		    String province = game.getProvinceAsDestination(point[0], point[1], ARMY);
		    if (province == null) {
			throw new OrderParamException("must select destination province", R.string.expected_a_province);
		    }
		    setNext(province);
		}
	    }
	}
	public void addToLayers(Context context, List<Drawable> layers, Poi mapDimensions) {
	    if (parameters[1] != null && !parameters[1].equals(parameters[0])) {
		layers.add(new Box(Game.PROVINCE_COORDINATES.get(source), 3, getColor(context, unitPower)));
		layers.add(new Arrow(new Poi(Game.PROVINCE_COORDINATES.get(source)), 
				     new Poi(Game.PROVINCE_COORDINATES.get(parameters[0])),
				     new Poi(Game.PROVINCE_COORDINATES.get(parameters[1])),
				     getColor(context, unitPower)).failed(failed));
	    } else if (parameters[0] != null) {
		layers.add(new Box(Game.PROVINCE_COORDINATES.get(source), 3, getColor(context, unitPower)));
		layers.add(new Arrow(new Poi(Game.PROVINCE_COORDINATES.get(source)), 
				     new Poi(Game.PROVINCE_COORDINATES.get(parameters[0])),
				     getColor(context, unitPower)).failed(failed));
	    }
	}
    }
    public static class Disband extends Order {
	public static final Pattern pattern = Pattern.compile("^([^\\W:]+): (\\S+) (\\S+) disband$");
	public Disband(Game game, Matcher matcher) {
	    super(game);
	    if (matcher.matches()) {
		dislodgedPower = matcher.group(1);
		dislodgedType = matcher.group(2);
		source = matcher.group(3);
	    } else {
		throw new OrderCreationException(matcher + " is not valid " + this.getClass());
	    }
	}
	public Disband(Game game, String source) { super(game, source); }
	public String getPower() { return dislodgedPower; }
	protected Object[] getParameterTypes() { return new Object[0]; }
	@Override
	public String getHelpText() {
	    return "When a unit is dislodged from a province, it will be required to either retreat or disband. If it can not retreat it will automatically disband. See http://en.wikibooks.org/wiki/Diplomacy/Rules for more information.";
	}
	protected int[] getParameterDescriptionResources() { return new int[0]; }
	public int getTypeResource() { return R.string.disband; }
	public String toFullString() { return dislodgedPower + ": " + dislodgedType + " " + source + " disband"; }
	protected void validate() { 
	    if (!game.phaseType.equals(RETREAT)) {
		throw new OrderCreationException("" + this.getClass() + " can only be created during " + RETREAT + " phases");
	    }
	    if (dislodgedType == null) {
		throw new OrderCreationException("" + this.getClass() + " can only be for province with dislodged unit (" + source + ")");
	    }
	    if (!DEBUG_MODE && !game.power.equals(dislodgedPower)) {
		throw new OrderCreationException("" + this.getClass() + " can only be created for owner of dislodged unit (" + dislodgedPower + ")");
	    }
	}
	public void setNext(float[] point) { throw new RuntimeException("should not be called"); }
	public void addToLayers(Context context, List<Drawable> layers, Poi mapDimensions) {
	    layers.add(new Cross(Game.PROVINCE_COORDINATES.get(source),
				 getColor(context, dislodgedPower)));
	}
    }
    public static class Retreat extends Order {
	public static final Pattern pattern = Pattern.compile("^([^\\W:]+): (\\S+) (\\S+) move (\\S+)$");
	public Retreat(Game game, Matcher matcher) { 
	    super(game);
	    if (matcher.matches()) {
		dislodgedPower = matcher.group(1);
		dislodgedType = matcher.group(2);
		source = matcher.group(3);
		parameters[0] = matcher.group(4);
	    } else {
		throw new OrderCreationException(matcher + " is not valid " + this.getClass());
	    }
	}
	public Retreat(Game game, String source) { super(game, source); }
	protected Object[] getParameterTypes() { return new Object[] { PROVINCE }; }
	public String getPower() { return dislodgedPower; }
	protected int[] getParameterDescriptionResources() { return new int[] { R.string.select_where_to_retreat }; }
	@Override
	public String getHelpText() {
	    return "When a unit is dislodged from a province, it will be required to either retreat or disband. If it can not retreat it will automatically disband. See http://en.wikibooks.org/wiki/Diplomacy/Rules for more information.";
	}
	public int getTypeResource() { return R.string.retreat; }
	public String toFullString() { return dislodgedPower + ": " + dislodgedType + " " + source + " move " + parameters[0]; }
	protected void validate() { 
	    if (!game.phaseType.equals(RETREAT)) {
		throw new OrderCreationException("" + this.getClass() + " can only be created during " + RETREAT + " phases");
	    }
	    if (dislodgedType == null) {
		throw new OrderCreationException("" + this.getClass() + " can only be for province with dislodged unit (" + source + ")");
	    }
	    if (!DEBUG_MODE && !game.power.equals(dislodgedPower)) {
		throw new OrderCreationException("" + this.getClass() + " can only be created for owner of dislodged unit (" + dislodgedPower + ")");
	    }
	}
	public void setNext(float[] point) { 
	    setNext(game.getProvinceAsDestination(point[0], point[1], dislodgedType));
	}
	public void addToLayers(Context context, List<Drawable> layers, Poi mapDimensions) {
	    if (parameters[0] != null) {
		layers.add(new Arrow(new Poi(Game.PROVINCE_COORDINATES.get(source)), 
				     new Poi(Game.PROVINCE_COORDINATES.get(parameters[0])),
				     getColor(context, getPower())).failed(failed));
	    }
	}
    }

    public static final Object UNIT_TYPE = new Object();
    public static final Object PROVINCE = new Object();

    public static Order parse(Game game, String s) {
	Matcher matcher = null;
	if ((matcher = Build.pattern.matcher(s)).matches()) return new Build(game, matcher);
	if ((matcher = Remove.pattern.matcher(s)).matches()) return new Remove(game, matcher);
	if ((matcher = Convoy.pattern.matcher(s)).matches()) return new Convoy(game, matcher);
	if ((matcher = Hold.pattern.matcher(s)).matches()) return new Hold(game, matcher);
	if (game.phaseType.equals(MOVEMENT) && (matcher = MoveViaConvoy.pattern.matcher(s)).matches()) return new MoveViaConvoy(game, matcher);
	if (game.phaseType.equals(MOVEMENT) && (matcher = Move.pattern.matcher(s)).matches()) return new Move(game, matcher);
	if ((matcher = Support.pattern.matcher(s)).matches()) return new Support(game, matcher);
	if ((matcher = Disband.pattern.matcher(s)).matches()) return new Disband(game, matcher);
	if (game.phaseType.equals(RETREAT) && (matcher = Retreat.pattern.matcher(s)).matches()) return new Retreat(game, matcher);
	return null;
    }

    protected String[] parameters;
    protected String source;
    protected String unitPower;
    protected String dislodgedPower;
    protected String unitType;
    protected String dislodgedType;
    protected String supplyPower;
    protected int nextParam = 0;
    protected Game game;
    protected boolean failed = false;

    private Order(Game game) {
	this.game = game;
	this.parameters = new String[getParameterTypes().length];
    }

    private Order(Game game, String source) {
	this.game = game;
	this.source = source;
	String[] powerAndType = game.getUnitPowerAndType(source);
	if (powerAndType != null) {
	    this.unitPower = powerAndType[0];
	    this.unitType = powerAndType[1];
	}
	powerAndType = game.getDislodgedPowerAndType(source);
	if (powerAndType != null) {
	    this.dislodgedPower = powerAndType[0];
	    this.dislodgedType = powerAndType[1];
	}
	this.supplyPower = game.getSupplyPower(source);
	this.parameters = new String[getParameterTypes().length];
    }

    public String getSourceProvince() {
	return source;
    }

    public abstract void setNext(float[] point);

    public abstract String getPower();

    public abstract void addToLayers(Context context, List<Drawable> layers, Poi mapDimensions);

    protected abstract Object[] getParameterTypes();

    protected abstract int[] getParameterDescriptionResources();

    public abstract int getTypeResource();

    public abstract String toFullString();

    protected abstract void validate();

    public String getSource() {
	return source;
    }
    public String toString() {
	return toFullString();
    }
    public Object nextType() {
	return getParameterTypes()[nextParam];
    }
    public int nextDescription() {
	return getParameterDescriptionResources()[nextParam];
    }
    public String getHelpText() {
	return null;
    }
    public boolean done() {
	return nextParam >= parameters.length;
    }
    public void setNext(String param) {
	parameters[nextParam] = param;
	try {
	    validate();
	} catch (OrderCreationException e) {
	    throw new OrderParamException(e.getMessage(), R.string.illegal_order);
	}
	if (param == null) {
	    throw new OrderParamException("null param?", R.string.expected_a_province);
	}
	nextParam++;
    }
}

