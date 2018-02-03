package umessage.com.umessage;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

public class MainActivity extends AppCompatActivity {

    private Socket mSocket;
    private TextView currentReceivedMessageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        currentReceivedMessageView = (TextView) findViewById(R.id.display_message_view);
    }

    @Override
    public void onStart() {
        super.onStart();
        try {
            mSocket = IO.socket("http://169.234.95.87:3000");

        } catch(URISyntaxException e) {
            e.printStackTrace();
        }

        mSocket.connect();
    }
}
