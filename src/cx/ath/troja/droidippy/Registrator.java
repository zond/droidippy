package cx.ath.troja.droidippy;

import android.app.*;
import android.os.*;
import android.content.*;
import android.accounts.*;
import android.util.*;

import org.apache.http.*;
import org.apache.http.protocol.*;
import org.apache.http.client.protocol.*;
import org.apache.http.impl.client.*;
import org.apache.http.client.methods.*;
import org.apache.http.client.*;
import org.apache.http.impl.cookie.*;

import java.security.*;
import java.util.*;
import java.text.*;
import java.math.*;

import static cx.ath.troja.droidippy.Util.*;

/**
 * Will register us with a new dippy token at the dippy server.
 *
 * Will remember the new token permanently, register on GCM and run the whendone Runnable if it gets a 204.
 *
 * Will retry after invalidating the current auth token and getting a new one if it gets a 401.
 */
public class Registrator extends Thread implements BaseActivity.Cancellable {
    private Context context;
    private Account account;
    private String tmpDippyToken;
    private int tries;
    private DefaultHttpClient client;
    private Doable<Object> whenDone;
    private Doable<RuntimeException> errorHandler;
    private boolean cancelled = false;

    public Registrator(Context context, Doable<Object> whenDone, Doable<RuntimeException> errorHandler) {
	this.context = context;
	this.account = getAccount(context);
	this.whenDone = whenDone;
	this.errorHandler = errorHandler;
	this.tries = 0;
	this.tmpDippyToken = generateDippyToken();
	this.client = new HttpClient(context);
    }
    @Override
    public void cancel() {
	cancelled = true;
    }
    private String generateDippyToken() {
	Random random = new SecureRandom();
	byte[] randomBytes = new byte[128];
	random.nextBytes(randomBytes);
	byte[] tokenBytes = new byte[account.name.getBytes().length + randomBytes.length + 1];
	System.arraycopy(account.name.getBytes(), 0, tokenBytes, 0, account.name.getBytes().length);
	System.arraycopy(randomBytes, 0, tokenBytes, account.name.getBytes().length + 1, randomBytes.length);
	return new BigInteger(tokenBytes).toString(36);
    }
    private void error(RuntimeException e) {
	if (!cancelled) {
	    errorHandler.doit(e);
	}
    }
    private void registerWithToken(String authToken) {
	try {
	    HttpGet get = new HttpGet(MessageFormat.format(REGISTER_URL_FORMAT, 
							   tmpDippyToken, 
							   authToken));
	    HttpResponse response = client.execute(get);
	    if (response.getStatusLine().getStatusCode() == 204) {
		setDippyToken(context, tmpDippyToken);
		GCMIntentService.registerGCM(context);
		if (!cancelled) {
		    whenDone.doit(null);
		}
	    } else if (response.getStatusLine().getStatusCode() == 401) {
		if (tries < MAX_RETRIES) {
		    AccountManager.get(context).invalidateAuthToken(GOOGLE_ACCOUNT_TYPE, authToken);
		    run();
		} else {
		    error(new RuntimeException("Unable to login after " + tries + " tries: " + response.getStatusLine()));
		}
	    } else {
		error(new RuntimeException("Logging in gave unexpected status: " + response.getStatusLine()));
	    }
	} catch (Exception e) {
	    error(new RuntimeException(e));
	}
    }
    public void run() {	
	if (context instanceof BaseActivity) {
	    ((BaseActivity) context).register(this);
	}
	try {
	    tries++;
	    Bundle authData = null;
	    if (context instanceof Activity) {
		authData = AccountManager.get(context).getAuthToken(account, 
								    AUTH_TOKEN_TYPE, 
								    null,
								    (Activity) context,
								    null,
								    null).getResult();
	    } else {
		authData = AccountManager.get(context).getAuthToken(account, 
								    AUTH_TOKEN_TYPE, 
								    false,
								    null,
								    null).getResult();
	    }		    
	    if (authData.containsKey(AccountManager.KEY_AUTHTOKEN)) {
		registerWithToken(authData.getString(AccountManager.KEY_AUTHTOKEN));
	    } else {
		error(new RuntimeException("Failed to get auth token!"));
	    }
	} catch (Exception e) {
	    error(new RuntimeException(e));
	} finally {
	    if (context instanceof BaseActivity) {
		((BaseActivity) context).unregister(this);
	    }
	}
    }
}

