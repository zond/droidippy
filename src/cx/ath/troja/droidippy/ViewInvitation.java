
package cx.ath.troja.droidippy;

import android.app.*;
import android.os.*;
import android.widget.*;
import android.content.*;
import android.view.*;
import android.util.*;
import android.net.*;

import java.util.*;
import java.text.*;

import static cx.ath.troja.droidippy.Util.*;

public class ViewInvitation extends BaseActivity {

    private static final int DIALOG_ORDER_POWERS = 1;
    private static final int DIALOG_CUSTOM_GAME_INFORMATION = 2;

    private String invitationCode = null;
    private Map<String, Object> joinInvitationalMeta = null;

    private void initializeFrom(Intent intent) {
	setContentView(R.layout.view_invitation);	
	invitationCode = intent.getData().getSchemeSpecificPart();
    }

    @Override
    protected void onNewIntent(Intent intent) {
	if (!intent.getData().equals(getIntent().getData())) {
	    initializeFrom(intent);
	}
	super.onNewIntent(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	initializeFrom(getIntent());
    }
    
    @Override
    protected void onResume() {
	super.onResume();
    	((TextView) findViewById(R.id.view_invitation_explanation)).setText(MessageFormat.format(getResources().getString(R.string.you_have_been_invited_to_a_game), getIntent().getExtras().getString(INVITER)));
	((Button) findViewById(R.id.view_invitation_decline)).setOnClickListener(new View.OnClickListener() {
		public void onClick(View v) {
		    finish();
		}
	    });
	((Button) findViewById(R.id.view_invitation_accept)).setOnClickListener(new View.OnClickListener() {
		public void onClick(View v) {
		    showProgress(R.string.loading_game_details);
		    new Getter<Map<String, Object>>(getApplicationContext(), MessageFormat.format(GET_INVITATIONAL_META_FORMAT, invitationCode)).
			onResult(new HandlerDoable<Map<String, Object>>() {
				public void handle(Map<String, Object> data) {
				    hideProgress();
				    joinInvitationalMeta = data;
				    removeDialog(DIALOG_CUSTOM_GAME_INFORMATION);
				    showDialog(DIALOG_CUSTOM_GAME_INFORMATION);
				}
			    }).
			onError(404, new HandlerDoable<String>() {
				public void handle(String s) {
				    toast(R.string.game_has_already_started);
				}
			    }).
			onError(STD_ERROR_HANDLER).start();
		}
	    });
    }

    private void joinGame(List<String> powerPreferences) {
	((Button) ViewInvitation.this.findViewById(R.id.view_invitation_accept)).setEnabled(false);
	((Button) ViewInvitation.this.findViewById(R.id.view_invitation_decline)).setEnabled(false);
	showProgress(R.string.joining_game);
	new Poster<String>(getApplicationContext(), 
			   MessageFormat.format(JOIN_GAME_FORMAT, 
						invitationCode), 
			   powerPreferences).
	    onResult(new HandlerDoable<String>() {
		    public void handle(String s) {
			hideProgress();
			if (INVITING.equals(s) || STARTED.equals(s)) {
			    Intent gamesIntent = new Intent(getApplicationContext(), Droidippy.class);
			    startActivity(gamesIntent);
			} else {
			    toast(R.string.game_has_already_started);
			}
			finish();
		    }
		}).
	    onError(STD_ERROR_HANDLER).start();
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {
	Dialog dialog;
	switch(id) {
	case DIALOG_CUSTOM_GAME_INFORMATION:
	    dialog = new Dialog(this);
	    dialog.setTitle(R.string.join_invitational_game);
	    dialog.setContentView(R.layout.custom_game_information_dialog);
	    Droidippy.prepareCustomGameInformationDialog(this, new Doable<List<String>>() {
		    public void doit(List<String> powerPreferences) {
			removeDialog(DIALOG_CUSTOM_GAME_INFORMATION);
			joinGame(powerPreferences);
		    }
		}, dialog, joinInvitationalMeta);
	    break;
	case DIALOG_ORDER_POWERS:
	    final List<DragAndDropAdapter> adapterContainer = new ArrayList<DragAndDropAdapter>();
	    dialog = Droidippy.prepareOrderPowersDialog(this,
							new View.OnClickListener() {
							    public void onClick(View view) {
								removeDialog(DIALOG_ORDER_POWERS);
								ArrayList<String> powerPreferences = new ArrayList<String>();
								DragAndDropAdapter powerAdapter = adapterContainer.get(0);
								for (int i = 0; i < powerAdapter.getCount(); i++) {
								    powerPreferences.add((String) powerAdapter.getItem(i));
								}
								setPowerPreferences(getApplicationContext(), powerPreferences);
								joinGame(powerPreferences);
							    }
							},
							R.string.join_game,
							R.string.select_power_preferences_join,
							adapterContainer);
	    break;
	default:
	    dialog = null;
	}
	return dialog;
    }

}
