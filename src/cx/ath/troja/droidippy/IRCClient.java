package cx.ath.troja.droidippy;

import android.widget.*;
import android.os.*;
import android.util.*;
import android.view.*;
import android.app.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.math.*;
import java.util.regex.*;

import static cx.ath.troja.droidippy.Util.*;

public class IRCClient extends BaseActivity {
    
    private static final Pattern IRC_PATTERN = Pattern.compile("^(:([^ !@]+)!?([^ !@]*)@?([^ !@]*) +)?([^ ]+) +(.*)$");
    private static final Pattern MESSAGE_PATTERN = Pattern.compile("^(\\S+)\\s+:(.*)$");
    private static final Pattern NAMES_PATTERN = Pattern.compile("^(\\S+)\\s*=\\s*(\\S+)\\s*:(.*)$");
    private static final Pattern ACCOUNT_PATTERN = Pattern.compile("^([^@]+)@.*$");
    private static final Pattern ACTION_PATTERN = Pattern.compile("\u0001ACTION\\s+(.*)\u0001$");

    private static final int MAX_MESSAGES = 100;

    private static final String CHANNEL = "#droidippy";

    private Thread listener = null;
    private Socket socket = null;
    private BufferedWriter writer = null;
    private BufferedReader reader = null;
    private boolean loggedIn = false;
    private boolean joined = false;    
    private ArrayAdapter<String> messages = null;
    private Button send = null;
    private String nick = null;
    private int randomNicks = 0;
    private Set<String> members = new HashSet<String>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.irc);	
	nick = getAccountNick();
	messages = new ArrayAdapter<String>(this, R.layout.game_chat_item);
	((TextView) findViewById(R.id.chat_title)).setText(R.string.irc_chat);
	((ListView) findViewById(R.id.chat_messages)).setAdapter(messages);
	final EditText input = (EditText) findViewById(R.id.chat_edit);
	send = (Button) findViewById(R.id.chat_send);
	send.setEnabled(false);
	send.setOnClickListener(new View.OnClickListener() {
		public void onClick(View v) {
		    if (socket != null && socket.isConnected() && loggedIn && joined) {
			try {
			    writer.write("PRIVMSG " + CHANNEL + " :" + input.getText() + "\r\n");
			    writer.flush();
			    messages.add(nick + ": " + input.getText());
			    input.setText("");
			} catch (Exception e) {
			    handleError(e);
			}
		    }
		}
	    });
	handler = new Handler();
	connect();
    }

    private String getAccountNick() {
	String accountName = getAccountName(this);
	Matcher m = ACCOUNT_PATTERN.matcher(accountName);
	if (m.matches()) {
	    return m.group(1).replaceAll("[^a-zA-Z0-9_-]", "_");
	} else {
	    return null;
	}
    }

    private void showMembers() {
	StringBuffer string = new StringBuffer();
	List<String> list = new ArrayList<String>(members);
	Collections.sort(list);
	Iterator<String> iterator = list.iterator();
	while (iterator.hasNext()) {
	    string.append(iterator.next());
	    if (iterator.hasNext()) {
		    string.append("\n");
	    }
	}
	new AlertDialog.Builder(this).setMessage(string.toString()).setTitle(R.string.members).setNeutralButton(R.string.ok, new OkClickable()).show();
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
	switch (item.getItemId()) {
	case R.id.game_chat_menu_members:
	    showMembers();
	    return true;
	default:
	    return super.onOptionsItemSelected(item);
	}
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
	getMenuInflater().inflate(R.menu.game_chat_menu, menu);
	return true;
    }

    @Override
    public void onDestroy() {
	disconnect();
	super.onDestroy();
    }

    private void handleError(final Exception e) {
	if (socket != null) {
	    handler.post(new Runnable() {
		    public void run() {
			if (socket != null) {
			    error(new RuntimeException(e), false);
			}
		    }
		});
	}
	disconnect();
    }

    private void disconnect() {
	try {
	    if (socket != null && socket.isConnected()) {
		try {
		    writer.write("QUIT\r\n");
		    writer.flush();
		} catch (SocketException e) {
		    if (!e.getMessage().matches(",*Socket closed.*")) {
			throw e;
		    }
		}
	    }
	    loggedIn = false;
	    joined = false;
	} catch (Exception e) {
	    Log.w(getPackageName(), "While disconnecting", e);
	} finally {
	    try {
		if (socket != null) {
		    socket.close();
		}
	    } catch (Exception e) {
	    }
	    socket = null;
	}
    }

    private String getRandomNick() {
	randomNicks++;
	if (randomNicks < 7) {
	    String accountNick = getAccountNick();
	    if (accountNick == null) {
		return "u" + new BigInteger("" + Math.abs(new Random().nextLong())).toString(36);
	    } else {
		return accountNick + new BigInteger("" + Math.abs(new Random().nextLong())).toString(36).substring(0,2);
	    }
	} else {
	    return "u" + new BigInteger("" + Math.abs(new Random().nextLong())).toString(36);
	}
    }

    private void login() throws IOException {	
	writer.write("NICK " + nick + "\r\n");
	writer.write("USER " + getRandomNick() + " localhost * : Droidippy IRCClient\r\n");
	writer.flush();
    }

    private void onLogin() throws IOException {
	writer.write("JOIN " + CHANNEL + "\r\n");
	writer.flush();
    }

    private void onJoin() {
	handler.post(new Runnable() {
		public void run() {
		    send.setEnabled(true);
		    hideProgress();
		    showMembers();
		}
	    });
    }

    private void onAction(String sender, String message) {
	addMessage(sender + " " + message);
    }

    private void onMessage(String sender, String message) {
	addMessage(sender + ": " + message);
    }

    private void onNotice(String sender, String message) {
	addMessage(sender + " whispers: " + message);
    }

    private void onJoin(final String who) {
	addMessage(who + " joined");
	members.add(who);
    }

    private void onLeave(final String who) {
	addMessage(who + " left");
	members.remove(who);
    }

    private void addMessage(final String m) {
	handler.post(new Runnable() {
		public void run() {
		    messages.add(m);
		    limitMessages();
		}
	    });
    }

    private void limitMessages() {
	while (messages.getCount() > MAX_MESSAGES) {
	    messages.remove(messages.getItem(0));
	}
    }

    private void onPing(String m) throws IOException {
	writer.write("PONG " + m + "\r\n");
	writer.flush();
    }

    private void onNames(String... names) {
	members.addAll(Arrays.asList(names));
    }

    private void connect() {
	showProgress(R.string.connecting);
	disconnect();
	listener = new Thread() {
		public void run() {
		    try {
			socket = new Socket("irc.freenode.net", 6667);
			writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			login();
			String line = null;
			Matcher matcher = null;
			while (socket.isConnected() && socket != null && (line = reader.readLine()) != null) {
			    if ((matcher = IRC_PATTERN.matcher(line)).matches()) {
				String command = matcher.group(5);
				if (command != null) {
				    if (command.equals("004")) {
					loggedIn = true;
					onLogin();
				    } else if (command.equals("433")) {
					nick = getRandomNick();
					login();
				    } else if (command.equals("432")) {
					nick = getRandomNick();
					login();
				    } else if (command.equals("366")) {
					joined = true;
					onJoin();
				    } else if (matcher.group(1) != null && matcher.group(1).matches(".*NickServ!NickServ@services.*") && command.equals("NOTICE") && line.matches(".*This nickname is registered.*")) {
					nick = getRandomNick();
					login();
				    } else if (command.equals("353")) {
					Matcher matcher2 = NAMES_PATTERN.matcher(matcher.group(6));
					if (matcher2.matches() && matcher2.group(2).equals(CHANNEL)) {
					    onNames(matcher2.group(3).split("\\s+"));
					}
				    } else if (command.equals("PART") || command.equals("QUIT")) {
					onLeave(matcher.group(2));
				    } else if (command.equals("366")) {
				    } else if (command.equals("JOIN")) {
					onJoin(matcher.group(2));
				    } else if (command.equals("PING")) {
					onPing(matcher.group(6));
				    } else if (command.equals("NOTICE") || command.equals("PRIVMSG")) {
					Matcher matcher2 = MESSAGE_PATTERN.matcher(matcher.group(6));
					if (matcher2.matches()) {
					    Matcher matcher3 = ACTION_PATTERN.matcher(matcher2.group(2));
					    if (matcher3.matches()) {
						onAction(matcher.group(2), matcher3.group(1));
					    } else if (matcher2.group(1).equals(nick)) {
						onNotice(matcher.group(2), matcher2.group(2));
					    } else {
						onMessage(matcher.group(2), matcher2.group(2));
					    }
					}
				    }
				}
			    }
			}
		    } catch (Exception e) {
			handleError(e);
		    }
		}
	    };
	listener.start();
    }

}