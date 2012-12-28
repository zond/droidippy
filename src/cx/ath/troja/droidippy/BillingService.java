
package cx.ath.troja.droidippy;

import com.android.vending.billing.IMarketBillingService;

import android.app.*;
import android.content.*;
import android.os.*;
import android.util.*;
import android.widget.*;

import java.util.*;
import java.security.*;
import java.util.concurrent.*;

import static cx.ath.troja.droidippy.Util.*;

import cx.ath.troja.droidippy.BillingConsts.ResponseCode;

public class BillingService extends Service implements ServiceConnection {

    private static IMarketBillingService service;

    private volatile int mostRecentStartId = -1;
    private final ConcurrentMap<Integer, Object> startIds = new ConcurrentHashMap<Integer, Object>();
    
    private static Queue<BillingRequest> pendingRequests = new ConcurrentLinkedQueue<BillingRequest>();
    private static ConcurrentMap<Long, BillingRequest> sentRequests = new ConcurrentHashMap<Long, BillingRequest>();

    public static final String MONTH = "cx.ath.troja.droidippy.month";
    public static final String YEAR = "cx.ath.troja.droidippy.year";

    public static final Map<String, String> PRODUCT_NAMES = new HashMap<String, String>();
    static {
	PRODUCT_NAMES.put(MONTH, "One month Premium Service");
	PRODUCT_NAMES.put(YEAR, "One year Premium Service");
	PRODUCT_NAMES.put("android.test.purchased", "Successful purchase");
	PRODUCT_NAMES.put("android.test.canceled", "Failed purchase");
    }

    public class BillingBinder extends Binder {
	public void checkBillingSupported(Doable<Boolean> onResult) {
	    BillingService.this.checkBillingSupported(onResult);
	}
	public void requestPurchase(Activity activity, String productId) {
	    BillingService.this.requestPurchase(activity, productId);
	}
    }

    abstract class BillingRequest {
        private final int startId;
        protected long requestId;

        public BillingRequest(int startId) {
            this.startId = startId;
        }

        public int getStartId() {
            return startId;
        }

        public void runRequest() {
	    pendingRequests.add(this);
	    if (service == null) {
		bindToMarketBillingService();
	    } else {
		runPendingRequests();
	    }
        }

        public boolean runIfConnected() {
	    if (service != null) {
		try {
                    requestId = run();
                    if (requestId >= 0) {
                        sentRequests.put(requestId, this);
                    }
                    return true;
		} catch (RemoteException e) {
		    Log.w(getPackageName(), "remote billing service crashed");
		    service = null;
		    return false;
		}
	    } else {
		return false;
	    }
        }

        abstract protected long run() throws RemoteException;

        protected void responseCodeReceived(ResponseCode responseCode) {
        }

        protected Bundle makeRequestBundle(String method) {
            Bundle request = new Bundle();
            request.putString(BillingConsts.BILLING_REQUEST_METHOD, method);
            request.putInt(BillingConsts.BILLING_REQUEST_API_VERSION, 1);
            request.putString(BillingConsts.BILLING_REQUEST_PACKAGE_NAME, getPackageName());
            return request;
        }

    }

    class CheckBillingSupported extends BillingRequest {
	private Doable<Boolean> onResult;
        public CheckBillingSupported(Doable<Boolean> onResult) {
            super(-1);
	    this.onResult = onResult;
        }

        @Override
        protected long run() throws RemoteException {
            Bundle request = makeRequestBundle("CHECK_BILLING_SUPPORTED");
            Bundle response = service.sendBillingRequest(request);
	    if (response != null) {
		int responseCode = response.getInt(BillingConsts.BILLING_RESPONSE_RESPONSE_CODE);
		onResult.doit(responseCode == ResponseCode.RESULT_OK.ordinal());
	    } else {
		onResult.doit(false);
	    }
	    return BillingConsts.BILLING_RESPONSE_INVALID_REQUEST_ID;
        }
    }

    class RequestPurchase extends BillingRequest {
        public final String productId;
	private Activity activity;

        public RequestPurchase(Activity activity, String itemId) {
            super(-1);
	    this.activity = activity;
            productId = itemId;
        }

        @Override
        protected long run() throws RemoteException {
	    try {
		Bundle request = makeRequestBundle("REQUEST_PURCHASE");
		request.putString(BillingConsts.BILLING_REQUEST_ITEM_ID, productId);
		
		Bundle response = service.sendBillingRequest(request);
		PendingIntent pendingIntent = response.getParcelable(BillingConsts.BILLING_RESPONSE_PURCHASE_INTENT);
		if (pendingIntent == null) {
		    Log.e(getPackageName(), "Error with requestPurchase");
		    return BillingConsts.BILLING_RESPONSE_INVALID_REQUEST_ID;
		}
		
		activity.startIntentSender(pendingIntent.getIntentSender(), new Intent(), 0, 0, 0);
		return response.getLong(BillingConsts.BILLING_RESPONSE_REQUEST_ID,
					BillingConsts.BILLING_RESPONSE_INVALID_REQUEST_ID);
	    } catch (RemoteException e) {
		throw e;
	    } catch (Exception e) {
		throw new RuntimeException(e);
	    }
	}
	
	@Override
        protected void responseCodeReceived(ResponseCode responseCode) {
	    if (responseCode == ResponseCode.RESULT_USER_CANCELED) {
		BaseActivity.deliverVerificationResult(CANCEL_OK, PRODUCT_NAMES.get(productId));
	    }
        }

    }

    class ConfirmNotifications extends BillingRequest {
        final String[] notifyIds;

        public ConfirmNotifications(int startId, String[] notifyIds) {
            super(startId);
            this.notifyIds = notifyIds;
        }

        @Override
        protected long run() throws RemoteException {
            Bundle request = makeRequestBundle("CONFIRM_NOTIFICATIONS");
            request.putStringArray(BillingConsts.BILLING_REQUEST_NOTIFY_IDS, notifyIds);
            Bundle response = service.sendBillingRequest(request);
	    return response.getLong(BillingConsts.BILLING_RESPONSE_REQUEST_ID,
				    BillingConsts.BILLING_RESPONSE_INVALID_REQUEST_ID);
        }
    }

    class GetPurchaseInformation extends BillingRequest {
        final String[] notifyIds;

        public GetPurchaseInformation(int startId, String[] notifyIds) {
            super(startId);
            this.notifyIds = notifyIds;
        }

        @Override
        protected long run() throws RemoteException {
	    final BlockingQueue<Object> responseContainer = new LinkedBlockingQueue<Object>();
	    try {
		new Getter<Long>(BillingService.this, GET_NONCE_URL).
		    onResult(new Doable<Long>() {
			    public void doit(Long nonce) {
				try {
				    Bundle request = makeRequestBundle("GET_PURCHASE_INFORMATION");
				    request.putLong(BillingConsts.BILLING_REQUEST_NONCE, nonce);
				    request.putStringArray(BillingConsts.BILLING_REQUEST_NOTIFY_IDS, notifyIds);
				    Bundle response = service.sendBillingRequest(request);
				    responseContainer.add(response.getLong(BillingConsts.BILLING_RESPONSE_REQUEST_ID,
									   BillingConsts.BILLING_RESPONSE_INVALID_REQUEST_ID));
				} catch (Exception e) {
				    responseContainer.add(e);
				}
			    }
			}).	    
		    onError(new Doable<RuntimeException>() {
			    public void doit(RuntimeException e) {
				BaseActivity.deliverVerificationResult(ERROR, "something");
				responseContainer.add(BillingConsts.BILLING_RESPONSE_INVALID_REQUEST_ID);
			    }
			}).
		    start();
		Object returnValue = responseContainer.take();
		if (returnValue instanceof RemoteException) {
		    throw (RemoteException) returnValue;
		} else {
		    return (Long) returnValue;
		}
	    } catch (RemoteException e) {
		throw e;
	    } catch (Exception e) {
		throw new RuntimeException(e);
	    }
        }

    }

    public BillingService() {
        super();
    }

    public void setContext(Context context) {
        attachBaseContext(context);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
	mostRecentStartId = startId;
	startIds.put(startId, new Object());
	handleCommand(intent, startId);
	return START_NOT_STICKY;
    }

    public void handleCommand(Intent intent, int startId) {
        String action = intent.getAction();
        if (BillingConsts.ACTION_CONFIRM_NOTIFICATION.equals(action)) {
            String[] notifyIds = intent.getStringArrayExtra(BillingConsts.NOTIFICATION_ID);
            confirmNotifications(startId, notifyIds);
        } else if (BillingConsts.ACTION_GET_PURCHASE_INFORMATION.equals(action)) {
            String notifyId = intent.getStringExtra(BillingConsts.NOTIFICATION_ID);
            getPurchaseInformation(startId, new String[] { notifyId });
        } else if (BillingConsts.ACTION_PURCHASE_STATE_CHANGED.equals(action)) {
            String signedData = intent.getStringExtra(BillingConsts.INAPP_SIGNED_DATA);
            String signature = intent.getStringExtra(BillingConsts.INAPP_SIGNATURE);
            purchaseStateChanged(startId, signedData, signature);
        } else if (BillingConsts.ACTION_RESPONSE_CODE.equals(action)) {
            long requestId = intent.getLongExtra(BillingConsts.INAPP_REQUEST_ID, -1);
            int responseCodeIndex = intent.getIntExtra(BillingConsts.INAPP_RESPONSE_CODE,
						       ResponseCode.RESULT_ERROR.ordinal());
            ResponseCode responseCode = ResponseCode.valueOf(responseCodeIndex);
            checkResponseCode(requestId, responseCode);
        }
    }

    private boolean bindToMarketBillingService() {
        try {
            boolean bindResult = bindService(new Intent(BillingConsts.MARKET_BILLING_SERVICE_ACTION),
					     this,
					     Context.BIND_AUTO_CREATE);

	    if (bindResult) {
                return true;
            } else {
                Log.e(getPackageName(), "Could not bind to service.");
		return false;
            }
        } catch (SecurityException e) {
            Log.e(getPackageName(), "Security exception: " + e);
	    return false;
        }
    }

    public void checkBillingSupported(Doable<Boolean> onResult) {
	new CheckBillingSupported(onResult).runRequest();
    }

    public void requestPurchase(Activity activity, String productId) {
        new RequestPurchase(activity, productId).runRequest();
    }

    private void confirmNotifications(int startId, String[] notifyIds) {
        new ConfirmNotifications(startId, notifyIds).runRequest();
    }

    private void getPurchaseInformation(int startId, String[] notifyIds) {
        new GetPurchaseInformation(startId, notifyIds).runRequest();
    }

    private void purchaseStateChanged(final int startId, String signedData, String signature) {
	new Poster<List<Map<String, String>>>(this, VERIFY_MARKET_PURCHASE_URL, new String[] { signedData, signature }).
	    onResult(new Doable<List<Map<String, String>>>() {
		    public void doit(List<Map<String, String>> result) {
			List<String> notifyIds = new ArrayList<String>();
			for (Map<String, String> map : result) {
			    String notificationId = map.get(NOTIFICATION_ID);
			    if (notificationId != null) {
				notifyIds.add(notificationId);
			    }
			    BaseActivity.deliverVerificationResult((String) map.get(RESULT),
								   (String) map.get(PRODUCT_NAME));
			}
			if (notifyIds.size() > 0) {
			    confirmNotifications(startId, notifyIds.toArray(new String[0]));
			}
		    }
		}).
	    onError(new Doable<RuntimeException>() {
		    public void doit(RuntimeException e) {
			BaseActivity.deliverVerificationResult(ERROR, "something");
		    }
		}).
	    start();
    }
    
    private void checkResponseCode(long requestId, ResponseCode responseCode) {
        BillingRequest request = sentRequests.get(requestId);
        if (request != null) {
            request.responseCodeReceived(responseCode);
        }
        sentRequests.remove(requestId);
    }

    private void runPendingRequests() {
	Iterator<BillingRequest> iterator = pendingRequests.iterator();
	while (iterator.hasNext()) {
	    BillingRequest request = iterator.next();
	    if (request.runIfConnected()) {
		iterator.remove();
		startIds.remove(request.getStartId());
	    } else {
		bindToMarketBillingService();
	    }
	}
        if (startIds.isEmpty()) {
            stopSelf(mostRecentStartId);
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        this.service = IMarketBillingService.Stub.asInterface(service);
	runPendingRequests();
    }

    @Override
    public IBinder onBind(Intent intent) {
	return new BillingBinder();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.w(getPackageName(), "Billing service disconnected");
        service = null;
    }

    @Override
    public void onDestroy() {
	super.onDestroy();
        try {
            unbindService(this);
        } catch (IllegalArgumentException e) {
            // This might happen if the service was disconnected
        }
    }
}
