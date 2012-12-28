
package cx.ath.troja.droidippy;

import android.preference.*;
import android.webkit.*;
import android.os.*;
import android.util.*;
import android.content.*;
import android.text.*;

import static cx.ath.troja.droidippy.Util.*;

public class Help extends BaseActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
	WebView webView = new WebView(this);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
	webView.setWebViewClient(new WebViewClient() {
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
		    view.loadUrl(url);
		    return true;
		}
	    });
	setContentView(webView);
	webView.loadUrl(HELP_URI);
    }

}