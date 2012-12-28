
package cx.ath.troja.droidippy;

import org.apache.http.*;
import org.apache.http.protocol.*;
import org.apache.http.client.protocol.*;
import org.apache.http.impl.client.*;
import org.apache.http.client.methods.*;
import org.apache.http.client.*;
import org.apache.http.conn.*;
import org.apache.http.params.*;
import org.apache.http.conn.ssl.*;
import org.apache.http.conn.scheme.*;
import org.apache.http.impl.conn.tsccm.*;

import javax.net.ssl.*;
import java.security.*;
import java.net.*;
import java.io.*;
import java.security.cert.*;

import android.content.*;
import android.content.pm.*;

public class HttpClient extends DefaultHttpClient {
 
    final Context context;
 
    public HttpClient(Context context) {
        this.context = context;
    }
 
    @Override
    protected ClientConnectionManager createClientConnectionManager() {
        SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        registry.register(new Scheme("https", newSslSocketFactory(), 443));
        return new ThreadSafeClientConnManager(getParams(), registry);
    }
 
    private org.apache.http.conn.ssl.SSLSocketFactory newSslSocketFactory() {
        try {
            KeyStore trusted = KeyStore.getInstance("BKS");
            InputStream in;
            if ((context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                in = context.getResources().openRawResource(R.raw.debug);
            } else {
                in = context.getResources().openRawResource(R.raw.server);
            }
            try {
                trusted.load(in, "123456".toCharArray());
            } finally {
                in.close();
            }
            org.apache.http.conn.ssl.SSLSocketFactory sf = new org.apache.http.conn.ssl.SSLSocketFactory(trusted);
            sf.setHostnameVerifier(org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            return sf;
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
