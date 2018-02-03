package umessage.com.umessage;

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Manager;
import com.github.nkzawa.socketio.client.Socket;

import java.net.URI;
import java.net.URISyntaxException;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "MainActivity";

    private Socket mSocket;

    private final String SERVER_URI = "http://169.234.95.87:3000";
    private EditText sendMessageView;
    private TextView currentReceivedMessageView;
    private Button sendButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sendMessageView = (EditText) findViewById(R.id.send_message_view);
        currentReceivedMessageView = (TextView) findViewById(R.id.display_message_view);
        sendButton = (Button) findViewById(R.id.send_button);
    }

    @Override
    public void onStart() {
        super.onStart();
        connectSocket();
        initializeSendButtonListener();
    }

    private void connectSocket() {
        try {

            mSocket = IO.socket(SERVER_URI);
            mSocket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
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
            });

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

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
            Log.d(TAG, "SENDING: " + message);
            sendMessageView.setText("");
            mSocket.emit("chat message", message);
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
