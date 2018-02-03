package umessage.com.umessage;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by danielshih on 2/3/18.
 */

public class MessageAdapter extends ArrayAdapter<Message> {
    private static class MessageViewHolder {
        public ImageView thumbnailImageView;
        public TextView senderView;
        public TextView bodyView;
    }

    public MessageAdapter(Context context, ArrayList<Message> message) {
        super(context, 0, message);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        MessageViewHolder holder;

        // if there is not already a view created for an item in the Message list.

        if (convertView == null){
            LayoutInflater messageInflater = (LayoutInflater) getContext().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

            // create a view out of our `message.xml` file
            convertView = messageInflater.inflate(R.layout.message, null);

            // create a MessageViewHolder
            holder = new MessageViewHolder();

            // set the holder's properties to elements in `message.xml`
            holder.senderView = (TextView) convertView.findViewById(R.id.message_sender);
            holder.bodyView = (TextView) convertView.findViewById(R.id.message_body);

            // assign the holder to the view we will return
            convertView.setTag(holder);
        } else {

            // otherwise fetch an already-created view holder
            holder = (MessageViewHolder) convertView.getTag();
        }

        // get the message from its position in the ArrayList
        Message message = getItem(position);

        // set the elements' contents
        holder.bodyView.setText(message.text);
        holder.senderView.setText(message.name);

        return convertView;

    }


}
