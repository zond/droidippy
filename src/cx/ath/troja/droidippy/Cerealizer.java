package cx.ath.troja.droidippy;

import java.io.*;
import java.math.*;
import java.security.*;

/**
 * A class that wraps the chunky serializing in java.
 */
public class Cerealizer {

    public static class ClassNotFoundException extends RuntimeException {
	private String className;
	public ClassNotFoundException(Throwable t, String c) {
	    super(c, t);
	    className = c;
	}
	public ClassNotFoundException(String s) {
	    super(s);
	}
	public String getClassName() {
	    return className;
	}
    }

    /**
     * Serialize something.
     *
     * @param s somethign to serialize
     * @return the bytes it became
     */
    public static byte[] pack(Object o) {
	try {
	    ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
	    ObjectOutputStream objectOut = new ObjectOutputStream(byteOut);
	    objectOut.writeObject(o);
	    objectOut.close();
	    byteOut.close();
	    return byteOut.toByteArray();
	} catch (Exception e) {
	    throw new RuntimeException(e);
	}
    }

    /**
     * Unserialize some bytes.
     *
     * @param c what to cast the result to
     * @param bytes what to unserialize
     * @return whatever the bytes was unserialized to
     */
    @SuppressWarnings("unchecked")
    public static <T> T unpack(Class<T> c, byte[] bytes) {
	return (T) unpack(bytes);
    }

    /**
     * Unserialize some bytes.
     *
     * @param bytes what to unserialize
     * @return whatever the bytes was unserialized to
     */
    public static Object unpack(byte[] bytes) {
	try {
	    ByteArrayInputStream byteIn = new ByteArrayInputStream(bytes);
	    ObjectInputStream objectIn = new ObjectInputStream(byteIn);
	    return objectIn.readObject();
	} catch (java.lang.ClassNotFoundException e) {
	    throw new Cerealizer.ClassNotFoundException(e, e.getMessage());
	} catch (Exception e) {
	    throw new RuntimeException(e);
	}
    }

}