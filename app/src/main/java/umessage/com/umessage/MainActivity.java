package umessage.com.umessage;

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import java.net.URISyntaxException;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {
    private final String TAG = "MainActivity";

    private Socket mSocket;

    private final String SERVER_URI = "https://simple-umessage-server.herokuapp.com";
    private EditText sendMessageView;
    private TextView currentReceivedMessageView;
    private ListView messageListView;
    private Button sendButton;

    private ArrayList<String> messages;
    private ArrayAdapter<String> messageListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        messages = new ArrayList<>();

        setContentView(R.layout.activity_main);

        sendMessageView = (EditText) findViewById(R.id.message_input);
        messageListView = (ListView) findViewById(R.id.messages_view);

        messageListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, messages);
        messageListView.setAdapter(messageListAdapter);

        sendButton = (Button) findViewById(R.id.send_button);
    }

    @Override
    public void onStart() {
        super.onStart();
        connectSocket();
        initializeSendButtonListener();
    }

    private Emitter.Listener onNewMessage = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            Log.d(TAG, "WOW I GOT SOMETHING");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String message = (String) args[0];
                    messages.add(message);
                    messageListAdapter.notifyDataSetChanged();
                }
            });
        }
    };

    private Emitter.Listener onConnectionFailure = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.d(TAG, "Failed to connect to " + SERVER_URI);
            mSocket.disconnect();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    displaySocketFailureDialog();
                }
            });
        }
    };

    private void connectSocket() {
        try {
            mSocket = IO.socket(SERVER_URI);

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        mSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectionFailure);
        mSocket.on("chat message", onNewMessage);

        mSocket.connect();
    }


    private void initializeSendButtonListener() {
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message = sendMessageView.getText().toString();
                sendMessage(message);
            }
        });
    }

    private void sendMessage(String message) {
        if(message.length() > 0) {
           // messages.add(message);
            Log.d(TAG, "SENDING: " + message);
            sendMessageView.setText("");
            mSocket.emit("chat message", message);

            SmsManager smsManager = SmsManager.getDefault();
//            smsManager.sendTextMessage("4089817280", null,
        }
    }

    private void displaySocketFailureDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle("Failed to connect to socket!!!!");
        builder.setMessage("Cannot connect to " + SERVER_URI);
        builder.setPositiveButton("Dismiss", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        builder.show();
    }
}
