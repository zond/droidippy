package cx.ath.troja.droidippy;

import android.content.*;

import org.apache.http.*;
import org.apache.http.protocol.*;
import org.apache.http.client.protocol.*;
import org.apache.http.impl.client.*;
import org.apache.http.client.methods.*;
import org.apache.http.client.*;
import org.apache.http.impl.cookie.*;
import 	org.apache.http.entity.*;

import java.io.*;

import static cx.ath.troja.droidippy.Util.*;

/**
 * Shall POST url with body and call doer with result body if it gets a 200.
 *
 * Will retry after running a Registrator if it gets a 401.
 */
public class Poster<T> extends Getter<T> {
    private Object input;
    public Poster(Context context, String url, Object input) {
	super(context, url);
	this.input = input;
    }
    @Override
    protected HttpUriRequest getRequest() {
	try {
	    HttpPost returnValue = new HttpPost(url);
	    returnValue.setEntity(new ByteArrayEntity(Cerealizer.pack(input)));
	    return returnValue;
	} catch (Exception e) {
	    error(new RuntimeException(e));
	    return null;
	}
    }
}
    
