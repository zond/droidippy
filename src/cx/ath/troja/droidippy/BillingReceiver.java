
package cx.ath.troja.droidippy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import cx.ath.troja.droidippy.BillingConsts.ResponseCode;

import static cx.ath.troja.droidippy.Util.*;

public class BillingReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (BillingConsts.ACTION_PURCHASE_STATE_CHANGED.equals(action)) {
            String signedData = intent.getStringExtra(BillingConsts.INAPP_SIGNED_DATA);
            String signature = intent.getStringExtra(BillingConsts.INAPP_SIGNATURE);
            purchaseStateChanged(context, signedData, signature);
        } else if (BillingConsts.ACTION_NOTIFY.equals(action)) {
            String notifyId = intent.getStringExtra(BillingConsts.NOTIFICATION_ID);
            if (BillingConsts.DEBUG) {
                Log.i(PACKAGE_NAME, "notifyId: " + notifyId);
            }
            notify(context, notifyId);
        } else if (BillingConsts.ACTION_RESPONSE_CODE.equals(action)) {
            long requestId = intent.getLongExtra(BillingConsts.INAPP_REQUEST_ID, -1);
            int responseCodeIndex = intent.getIntExtra(BillingConsts.INAPP_RESPONSE_CODE,
                    ResponseCode.RESULT_ERROR.ordinal());
            checkResponseCode(context, requestId, responseCodeIndex);
        } else {
            Log.w(PACKAGE_NAME, "unexpected action: " + action);
        }
    }

    private void purchaseStateChanged(Context context, String signedData, String signature) {
        Intent intent = new Intent(BillingConsts.ACTION_PURCHASE_STATE_CHANGED);
        intent.setClass(context, BillingService.class);
        intent.putExtra(BillingConsts.INAPP_SIGNED_DATA, signedData);
        intent.putExtra(BillingConsts.INAPP_SIGNATURE, signature);
        context.startService(intent);
    }

    private void notify(Context context, String notifyId) {
        Intent intent = new Intent(BillingConsts.ACTION_GET_PURCHASE_INFORMATION);
        intent.setClass(context, BillingService.class);
        intent.putExtra(BillingConsts.NOTIFICATION_ID, notifyId);
        context.startService(intent);
    }

    private void checkResponseCode(Context context, long requestId, int responseCodeIndex) {
        Intent intent = new Intent(BillingConsts.ACTION_RESPONSE_CODE);
        intent.setClass(context, BillingService.class);
        intent.putExtra(BillingConsts.INAPP_REQUEST_ID, requestId);
        intent.putExtra(BillingConsts.INAPP_RESPONSE_CODE, responseCodeIndex);
        context.startService(intent);
    }
}
