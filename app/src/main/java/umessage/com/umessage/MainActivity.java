package umessage.com.umessage;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;

import android.content.Context;
import android.os.Build;

import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {
    private final String TAG = "MainActivity";

    private Socket mSocket;

    private final String SERVER_URI = "https://simple-umessage-server.herokuapp.com/";
    private EditText sendMessageView;
    private EditText enterPhoneNumberView;
    private ListView messageListView;
    private Button sendButton;

    private ArrayList<String> messages;
    private ArrayAdapter<String> messageListAdapter;

    private static final int REQUEST_READ_PHONE_STATE = 0;
    private static final int ALL_PERMISSIONS = 1;

    private String[] PERMISSIONS = {Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS, Manifest.permission.SEND_SMS};

    private SmsManager smsmanage;

    private String phoneNumber;

    private String thisPhoneNumber;

    private TelephonyManager tMgr;

    private static MainActivity inst = MainActivity.instance();

    public static MainActivity instance() {
        return inst;
    }

    public void updateList(final String smsMessage) {
        messageListAdapter.insert(smsMessage, 0);
        messageListAdapter.notifyDataSetChanged();
        System.out.println("updated list");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        messages = new ArrayList<>();

        setContentView(R.layout.activity_main);

        sendMessageView = (EditText) findViewById(R.id.message_input);
        enterPhoneNumberView = (EditText) findViewById(R.id.phone_num_input);
        messageListView = (ListView) findViewById(R.id.messages_view);

        messageListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, messages);
        messageListView.setAdapter(messageListAdapter);

        sendButton = (Button) findViewById(R.id.send_button);
       // requestSMSPermission(4);
        smsmanage = SmsManager.getDefault();

        permissionCheck();

        tMgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

    }

    @Override
    public void onStart() {
        super.onStart();

        if(hasPermissions(this, PERMISSIONS)) {
            try{
                thisPhoneNumber = tMgr.getLine1Number();

                if(thisPhoneNumber.length() == 11) {
                    thisPhoneNumber = thisPhoneNumber.substring(1);
                }

                Log.d(TAG, "My phone number: " + thisPhoneNumber);
            } catch (SecurityException e) {
                permissionCheck();
            }

            connectSocket();

        } else {
            permissionCheck();
        }
        inst = this;

        initializeSendButtonListener();
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    private void permissionCheck() {
        if(!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, ALL_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_READ_PHONE_STATE:
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    try {
                        thisPhoneNumber = tMgr.getLine1Number();
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    }
                    sendPhoneNumberToServer();
                }
                break;

            default:
                break;
        }

        connectSocket();
    }

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

    private Emitter.Listener onConnectionSuccess = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            if(hasPermissions(getApplicationContext(), PERMISSIONS)) {
                Log.d(TAG, "Sending phone number " + thisPhoneNumber + " to server.");
                sendPhoneNumberToServer();
            }
        }
    };

    private Emitter.Listener onDisconnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.d(TAG, "Disconnected");
            mSocket.disconnect();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    displaySocketDisconnectDialog();
                }
            });
        }
    };

    private Emitter.Listener onNewMessage = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            Log.d(TAG, "Received a message");

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String message = (String) args[0];
                    messages.add(message);
                    // add sendText here current format: phonenumber:textmessage

                    if(message.indexOf(':') != -1) {
                        phoneNumber = message.split(":")[0];
                        try {
                            sendText(phoneNumber, message.split(":")[1]);
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                    messageListAdapter.notifyDataSetChanged();
                }
            });
        }
    };


    private Emitter.Listener onWebToSMS = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.d(TAG, "Received a text sending command!" + args[0].toString());
            JSONObject fullTokens = (JSONObject) args[0];

            //Format: SMS:originNumber:destNumber:message


            String destinationNumber = "";
            String message = "";
            try{
                destinationNumber = fullTokens.getString("destNumber");
                message = fullTokens.getString("msg");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Log.d(TAG, "Sending msg to " + destinationNumber);
            sendText(destinationNumber, message);
        }
    };

    private void connectSocket() {
        try {
            mSocket = IO.socket(SERVER_URI);

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        //Event listeners.
        mSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectionFailure);
        mSocket.on(Socket.EVENT_CONNECT, onConnectionSuccess);
        mSocket.on(Socket.EVENT_DISCONNECT, onDisconnect);
        mSocket.on("chat message", onNewMessage);
        mSocket.on("web to sms", onWebToSMS);

        mSocket.connect();

        Log.d(TAG, "Attempting connection...");
    }

    private void sendPhoneNumberToServer() {
        mSocket.emit("handshake phone", thisPhoneNumber);
    }

    private void initializeSendButtonListener() {
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message = sendMessageView.getText().toString();
                phoneNumber = enterPhoneNumberView.getText().toString().trim();

                Log.d(TAG, " Hello " + thisPhoneNumber);

                try{
                    sendText(phoneNumber, message);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });
    }

    private void sendServerMessage(String message) {
        if(message.length() > 0) {
           // messages.add(message);
            Log.d(TAG, "SENDING: " + message);
            mSocket.emit("chat message", message);
            mSocket.emit("handshake phone", phoneNumber);
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

    private void displaySocketDisconnectDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle("Disconnected!");
        builder.setMessage("Your phone has disconnected from " + SERVER_URI +
                        ". Restart uMessage to reestablish your connection. " +
                        "You can still send texts, but you will be unable to send " +
                        "through the uMessage web application until you restart and " +
                        "reestablish your connection.");
        builder.setPositiveButton("Dismiss", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        builder.show();
    }

    //Here is the Sendtext number!
    //Call this method to send text
    //It needs phone number and message
    //Hard code the phone number for now
    void sendText(final String phoneNumber, final String msg){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            permissionCheck();
        }

        String message = msg;
        System.out.println(phoneNumber + message);


        smsmanage.sendTextMessage(phoneNumber, null, message, null, null);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                messageListAdapter.insert("SMS To: +1" + phoneNumber + "\n" + msg, 0);
                messageListAdapter.notifyDataSetChanged();

                enterPhoneNumberView.setText("");
                sendMessageView.setText("");
                Toast.makeText(getApplicationContext(), "Message Sent!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
