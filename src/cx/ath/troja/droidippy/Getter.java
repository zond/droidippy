package cx.ath.troja.droidippy;

import android.content.*;
import android.util.*;

import org.apache.http.*;
import org.apache.http.protocol.*;
import org.apache.http.client.protocol.*;
import org.apache.http.impl.client.*;
import org.apache.http.client.methods.*;
import org.apache.http.client.*;
import org.apache.http.impl.cookie.*;

import java.util.*;
import java.util.concurrent.*;
import java.io.*;

import static cx.ath.troja.droidippy.Util.*;

/**
 * Shall GET url and call doer with result body if it gets a 200.
 *
 * Will retry after running a Registrator if it gets a 401.
 */
public class Getter<T> extends Thread implements BaseActivity.Cancellable {

    public static class HttpException extends RuntimeException {
	public int code;
	public String body;
	public HttpException(int code, String s, String body) {
	    super(s + ": " + body);
	    this.body = body;
	    this.code = code;
	}
    }

    protected String url = null;
    private Doable<T> resultHandler = null;
    private Doable<RuntimeException> errorHandler = null;
    private Map<Integer, Doable<String>> errorHandlerMap = new HashMap<Integer, Doable<String>>();
    private int tries = 0;
    private Semaphore semaphore = null;
    private Context context = null;
    private boolean cancelled = false;
    public Getter(Context context, String url) {	
	this.context = context;
	this.url = url;
	this.tries = 0;
    }
    public Getter setSemaphore(Semaphore semaphore) {
	this.semaphore = semaphore;
	return this;
    }
    public Getter onError(int code, Doable<String> doable) {
	errorHandlerMap.put(code, doable);
	return this;
    }
    public Getter onResult(Doable<T> doable) {
	this.resultHandler = doable;
	return this;
    }
    public Getter onError(Doable<RuntimeException> doable) {
	this.errorHandler = doable;
	return this;
    }
    @Override
    public void cancel() {
	cancelled = true;
    }
    protected void error(RuntimeException e) {
	if (!cancelled) {
	    if (errorHandler != null) {
		release();
		errorHandler.doit(e);
	    } else {
		throw e;
	    }
	}
    }
    protected HttpUriRequest getRequest() {
	return new HttpGet(url);
    }
    private String getBodyString(HttpResponse response) throws IOException {
	BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
	StringBuffer returnValue = new StringBuffer();
	String read = null;
	while ((read = reader.readLine()) != null) {
	    returnValue.append(read).append("\n");
	}
	return returnValue.toString().trim();
    }
    private Object getBodyObject(HttpResponse response) throws IOException {
	try {
	    return new ObjectInputStream(response.getEntity().getContent()).readObject();
	} catch (IOException e) {
	    throw e;
	} catch (Exception e) {
	    throw new RuntimeException(e);
	}
    }
    private void release() {
	if (semaphore != null) {
	    semaphore.release();
	}
    }
    private void acquire() {
	if (semaphore != null) {
	    try {
		semaphore.acquire();
	    } catch (InterruptedException e) {
		throw new RuntimeException(e);
	    }
	}
    }
    private void registerAndRetry() {
	new Registrator(context, 
			new Doable<Object>() {
			    public void doit(Object o) {
				Getter.this.run();
			    }
	},
			new Doable<RuntimeException>() {
			    public void doit(RuntimeException e) {
				Getter.this.error(e);
			    }
			}).start();
    }
    public void run() {
	if (context instanceof BaseActivity) {
	    ((BaseActivity) context).register(this);
	}
	acquire();
	try {
	    HttpResponse response = null;
	    while (response == null) {
		try {
		    tries++;
		    response = getAuthorizedClient(context).execute(getRequest());
		} catch (IOException e) {
		    if (tries < MAX_RETRIES) {
			Thread.sleep(500);
		    } else {
			throw e;
		    }
		}
	    }
	    if (response.getStatusLine().getStatusCode() == 200) {
		if (!cancelled && resultHandler != null) {
		    release();
		    resultHandler.doit((T) getBodyObject(response));
		}
	    } else if (response.getStatusLine().getStatusCode() == 204) {
		if (!cancelled && resultHandler != null) {
		    release();
		    resultHandler.doit(null);
		}
	    } else if (response.getStatusLine().getStatusCode() == 401) {
		if (tries < MAX_RETRIES) {
		    registerAndRetry();
		} else {
		    error(new RuntimeException("Unable to fetch " + url + " after " + tries + " tries: " + response.getStatusLine()));
		}
	    } else if (errorHandlerMap.containsKey(response.getStatusLine().getStatusCode())) {
		if (!cancelled) {
		    release();
		    errorHandlerMap.get(response.getStatusLine().getStatusCode()).doit(getBodyString(response));
		}
	    } else {
		error(new HttpException(response.getStatusLine().getStatusCode(), 
					"Fetching " + url + " gave unexpected status: " + response.getStatusLine(),
					getBodyString(response)));
	    }
	} catch (MissingTokenException e) {
	    if (tries < MAX_RETRIES) {
		registerAndRetry();
	    } else {
		error(e);
	    }
	} catch (Exception e) {
	    error(new RuntimeException("While trying to fetch " + url, e));
	} finally {
	    if (context instanceof BaseActivity) {
		((BaseActivity) context).unregister(this);
	    }
	}
    }
}
    
