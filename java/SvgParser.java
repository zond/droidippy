
import java.io.*;
import java.util.regex.*;
import java.util.*;
import java.math.*;

import javax.xml.parsers.*;
import javax.vecmath.*;

import org.xml.sax.*;
import org.xml.sax.helpers.*;

import cx.ath.troja.nja.*;

public class SvgParser extends DefaultHandler {

    public static final String SVG_NS = "http://www.w3.org/2000/svg";
    public static final Pattern COLOR_PATTERN = Pattern.compile("^#([0-9a-fA-F]{6,6})$");
    public static final Pattern PATH_PATTERN1_0 = Pattern.compile("^[zZ][\\s,]*(.*)$");
    public static final Pattern PATH_PATTERN1_2 = Pattern.compile("^([mMlL])[\\s,]*([eE0-9.-]+)[\\s,]+([eE0-9.-]+)[\\s,]*(.*)$");
    public static final Pattern PATH_PATTERN1_6 = Pattern.compile("^([cC])[\\s,]*([eE0-9.-]+)[\\s,]+([eE0-9.-]+)[\\s,]+([eE0-9.-]+)[\\s,]+([eE0-9.-]+)[\\s,]+([eE0-9.-]+)[\\s,]+([eE0-9.-]+)[\\s,]*(.*)$");
    public static final Pattern PATH_PATTERN0_2 = Pattern.compile("^([eE0-9.-]+)[\\s,]+([eE0-9.-]+)[\\s,]*(.*)$");
    public static final Pattern PATH_PATTERN0_6 = Pattern.compile("^([eE0-9.-]+)[\\s,]+([eE0-9.-]+)[\\s,]+([eE0-9.-]+)[\\s,]+([eE0-9.-]+)[\\s,]+([eE0-9.-]+)[\\s,]+([eE0-9.-]+)[\\s,]*(.*)$");
    public static final Pattern MATRIX_PATTERN = Pattern.compile("^matrix\\(\\s*([-0-9.]+)\\s*,\\s*([-0-9.]+)\\s*,\\s*([-0-9.]+)\\s*,\\s*([-0-9.]+)\\s*,\\s*([-0-9.]+)\\s*,\\s*([-0-9.]+)\\s*\\)$");
    public static final Pattern TRANSLATE_PATTERN = Pattern.compile("^translate\\(\\s*([-0-9.]+)\\s*,\\s*([-0-9.]+)\\s*\\)$");
    public static final Pattern STYLE_PATTERN = Pattern.compile("^([^:]+):([^;]+);?(.*)$");
    public static final Pattern XLINK_PATTERN = Pattern.compile("^#(.*)$");
    public static final Pattern PIXEL_SIZE_PATTERN = Pattern.compile("^([.0-9]+)(px)?$");

    private class Attribute {
	public int index;
	public String localName;
	public String qName;
	public String type;
	public String value;
	public Attribute(Attributes attributes, int index) {
	    this.index = index;
	    this.localName = attributes.getLocalName(index);
	    this.qName = attributes.getQName(index);
	    this.type = attributes.getType(index);
	    this.value = attributes.getValue(index);
	}
	public String toString() {
	    return qName + "=" + value;
	}
    }

    private class Element {
	public String uri;
	public String localName;
	public String qName;
	public Map<String, Attribute> attributes;
	public Matrix3f transform = null;
	public Element(String uri, String localName, String qName, Attributes attributes) {
	    this.uri = uri;
	    this.localName = localName;
	    this.qName = qName;
	    this.attributes = new HashMap<String, Attribute>();
	    if (attributes != null) {
		for (int i = 0; i < attributes.getLength(); i++) {
		    Attribute attribute = new Attribute(attributes, i);
		    this.attributes.put(attribute.qName, attribute);
		    if (attribute.qName.equals("transform")) {
			Matcher matcher = null;
			if ((matcher = MATRIX_PATTERN.matcher(attribute.value)).matches()) {
			    transform = new Matrix3f(Float.parseFloat(matcher.group(1)), Float.parseFloat(matcher.group(3)), Float.parseFloat(matcher.group(5)),
						     Float.parseFloat(matcher.group(2)), Float.parseFloat(matcher.group(4)), Float.parseFloat(matcher.group(6)),
						     0, 0, 1);
			} else if ((matcher = TRANSLATE_PATTERN.matcher(attribute.value)).matches()) {
			    transform = new Matrix3f(1, 0, Float.parseFloat(matcher.group(1)),
						     0, 1, Float.parseFloat(matcher.group(2)),
						     0, 0, 1);
			} else {
			    throw new RuntimeException("unsupported transform type " + attribute);
			}
		    }
		}
	    }
	}
	public String toString() {
	    return "<" + qName + " " + attributes.values() + ">";
	}
	public String g(String name) {
	    return g(name, null);
	}
	public String g(String name, String def) {
	    Attribute attribute = attributes.get(name);
	    if (attribute == null) {
		return def;
	    } else {
		return attribute.value;
	    }
	}
    }

    public static void main(String[] s) throws Exception {
	SAXParserFactory factory = SAXParserFactory.newInstance();
	factory.setNamespaceAware(false);
	XMLReader xmlReader = factory.newSAXParser().getXMLReader();
	RuntimeArguments arguments = new RuntimeArguments(s);
	xmlReader.setContentHandler(new SvgParser(arguments));
	xmlReader.parse(new InputSource(new FileInputStream(arguments.mustGet("input", "You must provide an input!"))));
    }

    private float width = 0;
    private float height = 0;
    private RuntimeArguments arguments;
    private List<String> layerMethods = new ArrayList<String>();
    private int idSequence = 0;
    private PrintStream out;
    private Deque<Element> elementStack = new LinkedList<Element>();
    private StringBuffer lastText = new StringBuffer();
    private List<String> startingCoordinates = new ArrayList<String>();
    private Map<String, String> pathMethods = new HashMap<String, String>();

    public SvgParser(RuntimeArguments arguments) {
	try {
	    this.arguments = arguments;
	    if (arguments.has("output")) {
		this.out = new PrintStream(new FileOutputStream(arguments.get("output")));
	    } else {
		this.out = System.out;
	    }
	} catch (Exception e) {
	    throw new RuntimeException(e);
	}
    }

    private float[] transform(float x, float y) {
	Matrix3f ctm = new Matrix3f();
	ctm.setIdentity();
	for (Element element : elementStack) {
	    if (element.transform != null) {
		ctm.mul(element.transform);
	    }
	}
	return new float[] { ctm.m00 * x + ctm.m01 * y + ctm.m02, ctm.m10 * x + ctm.m11 * y + ctm.m12 };
    }

    public void warning(SAXParseException e) {
	System.err.println("" + e);
    }

    public void fatalError(SAXParseException e) {
	System.err.println("" + e);
    }

    public void startDocument() {
	if (arguments.has("package")) {
	    out.println("package " + arguments.get("package") + ";");
	}
	out.println("import java.util.*;");
	out.println("import java.math.*;");
	out.println("import android.content.*;");
	out.println("import android.util.*;");
	out.println("import android.graphics.*;");
	out.println("import android.graphics.drawable.*;");
	out.println("import android.graphics.drawable.shapes.*;");
	out.println("public class " + arguments.mustGet("class", "You must provide a class name for the output class!") + " {");
	out.println("  public static Map<String, float[]> startingCoordinates = new HashMap<String, float[]>();");
	out.println("  public static class TextDrawable extends Drawable {");
	out.println("    private float x;");
	out.println("    private float y;");
	out.println("    private int alpha = 255;");
	out.println("    private int textSize;");
	out.println("    private int intrinsicWidth = 0;");
	out.println("    private int intrinsicHeight = 0;");
	out.println("    private String text;");
	out.println("    private Path path;");
	out.println("    private Typeface font;");
	out.println("    public TextDrawable(float x, float y, String text, int textSize, Typeface font, Path path, int intrinsicWidth, int intrinsicHeight) {");
	out.println("      this.x = x;");
	out.println("      this.y = y;");
	out.println("      this.textSize = textSize;");
	out.println("      this.path = path;");
	out.println("      this.font = font;");
	out.println("      this.text = text;");
	out.println("      this.intrinsicWidth = intrinsicWidth;");
	out.println("      this.intrinsicHeight = intrinsicHeight;");
        out.println("    }");
	out.println("    public void draw(Canvas canvas) {");
	out.println("      Paint paint = new Paint();");
	out.println("      paint.setAntiAlias(true);");
	out.println("      paint.setColor(alpha << 24);");
	out.println("      float scaleX = ((float) getBounds().right - getBounds().left) / intrinsicWidth;");
	out.println("      float scaleY = ((float) getBounds().bottom - getBounds().top) / intrinsicHeight;");
	out.println("      paint.setTextSize((int) (textSize * scaleY));");
	out.println("      paint.setTextScaleX(scaleX);");
	out.println("      if (font != null) {");
	out.println("        paint.setTypeface(font);");
	out.println("      }");
	out.println("      if (path == null) {");
	out.println("        canvas.drawText(text, (x * scaleX) + getBounds().left, (y * scaleY) + getBounds().top, paint);");
	out.println("      } else {");
	out.println("        canvas.drawTextOnPath(text, path, 0, 0, paint);");
	out.println("      }");
        out.println("    }");
	out.println("    public int getOpacity() {");
	out.println("      return PixelFormat.OPAQUE;");
	out.println("    }");
	out.println("    public void setAlpha(int alpha) {");
	out.println("      this.alpha = alpha;");
	out.println("    }");
	out.println("    public void setColorFilter(ColorFilter cf) {");
	out.println("    }");
	out.println("  }");
    }
    
    private static String safe(String s) {
	return s.replaceAll("\\W", "_");
    }

    public void printCircle(String id, float x, float y, float radius, String color, String style) {
	out.println("  public static Drawable build" + safe(id) + "Circle(float[] translation, int width, int height) {");
	out.println("    Path path = new Path();");
	out.println("    path.addCircle(translation[0] + " + x + "f, translation[1] + " + y + "f, " + radius + "f, Path.Direction.CW);");
	out.println("    ShapeDrawable drawable = new ShapeDrawable(new PathShape(path, (float) width, (float) height));");
	out.println("    Paint paint = drawable.getPaint();");
	out.println("    paint.setAntiAlias(true);");
	out.println("    paint.setStyle(" + style + ");");
	if (color.equals("black")) {
	    out.println("    paint.setColor(" + new BigInteger("ff000000", 16).intValue() + ");");
	} else {
	    throw new RuntimeException("color " + color + " is not supported!");
	}
	out.println("    drawable.setIntrinsicWidth(width);");
	out.println("    drawable.setIntrinsicHeight(height);");
	out.println("    return drawable;");
	out.println("  }");
	layerMethods.add("    layers.add(build" + safe(id) + "Circle(translation, width, height));");
    }

    public void printPath(List<String> operations, String id) {
	out.println("  public static void buildPath" + safe(id) + "(Path path) {");
	int lines = 0;
	int builder = 0;
	for (String operation : operations) {
	    out.println("    path" + operation);
	    lines++;
	    if (lines > 200) {
		lines = 0;
		builder++;
		out.println("    buildPath" + safe(id) + "_" + builder + "(path);");
		out.println("  }");
		out.println("  public static void buildPath" + safe(id) + "_" + builder + "(Path path) {");

	    }
	}
	out.println("  }");
	pathMethods.put(id, "    buildPath" + safe(id) + "(path);");
    }

    public void printPathShape(List<String> operations, String id, String color, String style, String fillRule, String strokeWidth, int alpha) {
	Matcher matcher = null;
	printPath(operations, id);
	out.println("  public static Drawable buildPathShape" + safe(id) + "(float[] translation, Object configuration, int width, int height) {");
	out.println("    Path path = new Path();");
	if (fillRule != null) {
	    out.println("    path.setFillType(" + fillRule + ");");
	}
	out.println("    buildPath" + safe(id) + "(path);");
	out.println("    path.offset(translation[0], translation[1]);");
	out.println("    ShapeDrawable drawable = new ShapeDrawable(new PathShape(path, (float) width, (float) height));");
	out.println("    Paint paint = drawable.getPaint();");
	out.println("    paint.setStrokeWidth(" + strokeWidth + "f);");
	out.println("    paint.setStyle(" + style + ");");
	if ((matcher = COLOR_PATTERN.matcher(color)).matches()) {
	    out.println("    if (configuration != null) {");
	    out.println("      paint.setColor((Integer) configuration);");
	    out.println("    } else {");
	    out.println("      paint.setColor(" + new BigInteger("ff" + matcher.group(1), 16).intValue() + ");");
	    out.println("      paint.setAlpha(" + alpha + ");");
	    out.println("    }");
	} else {
	    throw new RuntimeException("color " + color + " is not supported!");
	}
	out.println("    drawable.setIntrinsicWidth(width);");
	out.println("    drawable.setIntrinsicHeight(height);");
	out.println("    return drawable;");
	out.println("  }");
	layerMethods.add("    if (!\"hide\".equals(configuration.get(\"" + id + "\"))) {");
	layerMethods.add("      layers.add(buildPathShape" + safe(id) + "(translation, configuration.get(\"" + id + "\"), width, height));");
	layerMethods.add("    }");
    }

    public void characters(char[] chars, int start, int length) {
	lastText.append(new String(chars, start, length));
    }

    private List<String> allAttributes(String name) {
	List<String> returnValue = new ArrayList<String>();
	for (Element element : elementStack) {
	    if (element.g(name) != null) {
		returnValue.add(element.g(name));
	    }
	}
	return returnValue;
    }

    private String findClosestAttribute(String name) {
	ArrayList<Element> elements = new ArrayList<Element>(elementStack);
	for (int i = elements.size() - 1; i > -1; i--) {
	    Element element = elements.get(i);
	    if (element.g(name) != null) {
		return element.g(name);
	    }
	}
	return null;
    }

    private boolean isWithin(String name) {
	ArrayList<Element> elements = new ArrayList<Element>(elementStack);
	for (int i = elements.size() - 1; i > -1; i--) {
	    if (elements.get(i).qName.equals(name)) {
		return true;
	    }
	}
	return false;
    }

    public void endElement(String uri, String localName, String qName) {
	Element element = elementStack.getLast();
	if (!element.qName.equals(qName)) {
	    throw new RuntimeException("the element stack is corrupt! i expected to end " + element + " but instead ended " + new Element(uri, localName, qName, null));
	}
	if (qName.equals("tspan") || qName.equals("textPath")) {
	    String[] xy = new String[] { findClosestAttribute("x"), findClosestAttribute("y") };
	    String id = element.g("id", "unnamed" + (idSequence++));
	    String textSize = "5";
	    String typeface = "null";
	    Matcher matcher = null;
	    for (String style : allAttributes("style")) {
		while ((matcher = STYLE_PATTERN.matcher(style)).matches()) {
		    String name = matcher.group(1).trim();
		    String value = matcher.group(2).trim();
		    style = matcher.group(3).trim();;
		    if (name.equals("font-size")) {
			matcher = PIXEL_SIZE_PATTERN.matcher(value);
			if (matcher.matches()) {
			    textSize = matcher.group(1);
			} else {
			    throw new RuntimeException("font-size " + value + " is not supported");
			}
		    }
		}
		if (style.length() != 0) {
		    throw new RuntimeException("unrecognized style part of path: " + style);
		}
	    }
	    if (xy[0] != null && xy[1] != null) {
		xy = transform(xy);
	    }
	    if (isWithin("textPath")) {
		String xlink = findClosestAttribute("xlink:href");
		if (xlink == null) {
		    throw new RuntimeException("textPaths without xlink:href not supported");
		}
		if (!(matcher = XLINK_PATTERN.matcher(xlink)).matches()) {
		    throw new RuntimeException("xlink:href looking like " + element.g("xlink:href") + " is not supported");
		}
		layerMethods.add("    Path textPath" + safe(id) + " = new Path();");
		layerMethods.add("    buildPath" + matcher.group(1) + "(textPath" + safe(id) + ");");
                layerMethods.add("    layers.add(new TextDrawable(translation[0] + 0f, translation[1] + 0f, \"" + lastText.toString().trim() + "\", " + new Float(textSize).intValue() + ", serif, textPath" + safe(id) + ", width, height));");
	    } else {
		if (xy[0] == null || xy[1] == null) {
		    throw new RuntimeException("plain text without x and y is not supported");
		}
		layerMethods.add("    layers.add(new TextDrawable(translation[0] + " + xy[0] + "f, translation[1] + " + xy[1] + "f, \"" + lastText.toString().trim() + "\", " + new Float(textSize).intValue() + ", serif, null, width, height));");
	    }
	}
	elementStack.removeLast();
	lastText = new StringBuffer();
    }

    private String[] transform(String[] coords) {
	for (int j = 0; j < coords.length; j += 2) {
	    float[] t = transform(Float.parseFloat(coords[j]), Float.parseFloat(coords[j + 1]));
	    coords[j] = "" + t[0];
	    coords[j + 1] = "" + t[1];
	}
	return coords;
    }

    public void startElement(String uri, String localName, String qName, Attributes attributes) {
	Element element = new Element(uri, localName, qName, attributes);
	if (element.qName.equals("text") || element.qName.equals("tspan") || element.qName.equals("textPath")) {
	    if (element.g("transform") != null) {
		throw new RuntimeException("transforms on text, tspan or textPath are not supported (" + element.g("id") + ")");
	    }
	}
	elementStack.addLast(element);
	Matcher matcher = null;
	if (qName.equals("svg")) {
	    if (element.g("viewBox") != null) {
		String[] params = element.g("viewBox").split("\\s+");
		width = Float.parseFloat(params[2]);
		height = Float.parseFloat(params[3]);
	    }
	    if (element.g("width") != null) {
		width = Float.parseFloat(element.g("width"));
	    }
	    if (element.g("height") != null) {
		height = Float.parseFloat(element.g("height"));
	    }
	} else if (qName.equals("circle")) {
	    float x = 0;
	    float y = 0;
	    float radius = 0;
	    if (element.g("cx") != null) {
		x = Float.parseFloat(element.g("cx"));
	    }
	    if (element.g("cy") != null) {
		y = Float.parseFloat(element.g("cy"));
	    }
	    if (element.g("radius") != null) {
		radius = Float.parseFloat(element.g("radius"));
	    }
	    String fill = element.g("fill", "none");
	    String stroke = element.g("stroke", "none");
	    float[] t = transform(x, y);
	    x = t[0];
	    y = t[1];
	    String id = element.g("id", "unnamed" + (idSequence++));
	    if (!stroke.equals("none")) {
		printCircle(id, x, y, radius, stroke, "Paint.Style.STROKE");
	    }
	    if (!fill.equals("none")) {
		printCircle(id, x, y, radius, fill, "Paint.Style.FILL");
	    }
	} else if (qName.equals("path")) {
	    String stroke = element.g("stroke", "none");
	    String fill = element.g("fill", "none");
	    String id = element.g("id", "unnamed" + (idSequence++));
	    String fillRule = null;
	    String strokeWidth = "0";
	    int fillAlpha = 0xff;
	    int strokeAlpha = 0xff;
	    if (element.g("style") != null) {
		String styleString = element.g("style");
		while (styleString.length() > 0 && (matcher = STYLE_PATTERN.matcher(styleString)).matches()) {
		    String name = matcher.group(1).trim();
		    String value = matcher.group(2).trim();
		    styleString = matcher.group(3).trim();;
		    if (name.equals("fill")) {
			fill = value;
		    } else if (name.equals("stroke")) {
			stroke = value;
		    } else if (name.equals("stroke-opacity")) {
			strokeAlpha = (int) (0xff * Float.parseFloat(value));
		    } else if (name.equals("fill-opacity")) {
			fillAlpha = (int) (0xff * Float.parseFloat(value));
		    } else if (name.equals("stroke-width")) {
			if ((matcher = PIXEL_SIZE_PATTERN.matcher(value)).matches()) {
			    strokeWidth = matcher.group(1);
			} else {
			    throw new RuntimeException("stroke width " + value + " not supported");
			}
		    } else if (name.equals("fill-rule")) {
			if (value.equals("evenodd")) {
			    fillRule = "Path.FillType.EVEN_ODD";
			} else {
			    throw new RuntimeException("fill-rule " + value + " is not supported");
			}
		    }
		}
		if (styleString.length() != 0) {
		    throw new RuntimeException("unrecognized style part of path: " + styleString);
		}		
	    }	    
	    String value = element.g("d");
	    List<String> operations = new ArrayList<String>();
	    boolean printedStartCoords = false;
	    while (value.length() > 0) {
		if ((matcher = PATH_PATTERN1_0.matcher(value)).matches()) {
		    operations.add(".close();");
		    value = matcher.group(1);
		} else if ((matcher = PATH_PATTERN1_2.matcher(value)).matches()) {
		    String def = "";
		    String[] coords = transform(new String[] { matcher.group(2), matcher.group(3) });
		    if (matcher.group(1).equals("M")) {
			def = "L";
			if (!printedStartCoords) {
			    printedStartCoords = true;
			    startingCoordinates.add("    startingCoordinates.put(\"" + id + "\", new float[] { " + coords[0] + "f, " + coords[1] + "f });");
			}
			operations.add(".moveTo(" + coords[0] + "f, " + coords[1] + "f);");
			for (int j = 2; j < coords.length; j += 2) {
			    operations.add(".lineTo(" + coords[j] + "f, " + coords[j + 1] + "f);");
			}
		    } else if (matcher.group(1).equals("m")) {
			def = "l";
			operations.add(".rMoveTo(" + coords[0] + "f, " + coords[1] + "f);");
			for (int j = 2; j < coords.length; j += 2) {
			    operations.add(".rLineTo(" + coords[j] + "f, " + coords[j + 1] + "f);");
			}
		    } else if (matcher.group(1).equals("l")) {
			def = "l";
			operations.add(".rLineTo(" + coords[0] + "f, " + coords[1] + "f);");
			for (int j = 2; j < coords.length; j += 2) {
			    operations.add(".rLineTo(" + coords[j] + "f, " + coords[j + 1] + "f);");
			}
		    } else if (matcher.group(1).equals("L")) {
			def = "L";
			operations.add(".lineTo(" + coords[0] + "f, " + coords[1] + "f);");
			for (int j = 2; j < coords.length; j += 2) {
			    operations.add(".lineTo(" + coords[j] + "f, " + coords[j + 1] + "f);");
			}
		    }
		    value = matcher.group(4);
		    if ((matcher = PATH_PATTERN0_2.matcher(value)).matches()) {
			value = def + value;
		    }
		} else if ((matcher = PATH_PATTERN1_6.matcher(value)).matches()) {
		    String def = "";
		    String[] coords = transform(new String[] { matcher.group(2), matcher.group(3), matcher.group(4), matcher.group(5), matcher.group(6), matcher.group(7) });
		    if (matcher.group(1).equals("c")) {
			def = "c";
			operations.add(".rCubicTo(" + coords[0] + "f, " + coords[1] + "f, " + coords[2] + "f, " + coords[3] + "f, " + coords[4] + "f, " + coords[5] + "f);");
			for (int j = 6; j < coords.length; j += 6) {
			    operations.add(".rCubicTo(" + coords[j] + "f, " + coords[j + 1] + "f, " + coords[j + 2] + "f, " + coords[j + 3] + "f, " + coords[j + 4] + "f, " + coords[j + 5] + "f);");
			}
		    } else if (matcher.group(1).equals("C")) {
			def = "C";
			operations.add(".cubicTo(" + coords[0] + "f, " + coords[1] + "f, " + coords[2] + "f, " + coords[3] + "f, " + coords[4] + "f, " + coords[5] + "f);");
			for (int j = 6; j < coords.length; j += 6) {
			    operations.add(".cubicTo(" + coords[j] + "f, " + coords[j + 1] + "f, " + coords[j + 2] + "f, " + coords[j + 3] + "f, " + coords[j + 4] + "f, " + coords[j + 5] + "f);");
			}
		    }		    
		    value = matcher.group(8);
		    if ((matcher = PATH_PATTERN0_6.matcher(value)).matches()) {
			value = def + value;
		    }
		} else {
		    throw new RuntimeException("Unknown operation for " + value);
		}
	    }
	    if (fill.equals("none") && stroke.equals("none")) {
		printPath(operations, id);
	    } else {
		if (!fill.equals("none")) {
		    printPathShape(operations, id + "Fill", fill, "Paint.Style.FILL", fillRule, strokeWidth, fillAlpha);
		}
		if (!stroke.equals("none")) {
		    printPathShape(operations, id + "Stroke", stroke, "Paint.Style.STROKE", fillRule, strokeWidth, strokeAlpha);
		}
	    }
	}
    }

    public void endDocument() {
	out.println("  static {");
	for (String startingCoordinate : startingCoordinates) {
	    out.println(startingCoordinate);
	}
	out.println("  }");
	out.println("  public static Path buildPath(String id) {");
	out.println("    Path path = new Path();");
	for (Map.Entry<String, String> entry : pathMethods.entrySet()) {
	    out.println("    if (id.equals(\"" + entry.getKey() + "\")) {");
	    out.println("      " + entry.getValue());
	    out.println("    }");
	}
	out.println("    return path;");
	out.println("  }");
	out.println("  public static Drawable create(Context context, float[] translation, Map<String, Object> configuration, Integer intrinsicWidth, Integer intrinsicHeight) {");
	out.println("    int width = 0;");
	out.println("    if (intrinsicWidth == null) {");
	out.println("      width = " + ((int) width) + ";");
	out.println("    } else {");
	out.println("      width = intrinsicWidth;");
	out.println("    }");
	out.println("    int height = 0;");
	out.println("    if (intrinsicHeight == null) {");
	out.println("      height = " + ((int) height) + ";");
	out.println("    } else {");
	out.println("      height = intrinsicHeight;");
	out.println("    }");
	out.println("    Typeface serif = Typeface.SERIF;");
	out.println("    ArrayList<Drawable> layers = new ArrayList<Drawable>();");
	for (String layerMethod : layerMethods) {
	    out.println(layerMethod);
	}
	out.println("    return new LayerDrawable(layers.toArray(new Drawable[0]));");
	out.println("  }");
	out.println("}");
	out.close();
    }

}