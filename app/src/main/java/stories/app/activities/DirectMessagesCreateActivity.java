package stories.app.activities;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import stories.app.R;
import stories.app.models.Message;
import stories.app.services.ChatService;
import stories.app.services.MessagingService;
import stories.app.utils.LocalStorage;

public class DirectMessagesCreateActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_direct_messages_create);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        setTitle("Enviar Mensaje Nuevo");
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        Button sendMessageButton = this.findViewById(R.id.sendMessageButton);
        sendMessageButton.setOnClickListener(new DirectMessagesCreateActivity.SendMessageHandler());
    }

    protected class SendMessageHandler implements View.OnClickListener {
        public void onClick(View v){
            EditText destination = findViewById(R.id.createMessageDestination);
            EditText messageText = findViewById(R.id.createMessageText);

            Message message = new Message();
            message.from_username = LocalStorage.getUser().username;
            message.to_username = destination.getText().toString();
            message.message = messageText.getText().toString();
            message.timestamp = System.currentTimeMillis();

            new DirectMessagesCreateActivity.SendMessageTask().execute(message);
        }
    }

    protected class SendMessageTask extends AsyncTask<Message, Void, Message> {
        private MessagingService messagingService = new MessagingService();

        protected void onPreExecute() {
            Button sendMessageButton = findViewById(R.id.sendMessageButton);
            sendMessageButton.setEnabled(false);
        }

        protected Message doInBackground(Message... params) {
            return messagingService.sendMessage(params[0]);
        }

        protected void onPostExecute(Message result) {
            Button sendMessageButton = findViewById(R.id.sendMessageButton);
            sendMessageButton.setEnabled(true);

            TextView sendMessageResult = findViewById(R.id.sendMessageResult);

            if (result == null) {
                sendMessageResult.setText(R.string.error_invalid_message);
            } else {
                Intent navigationIntent = new Intent(DirectMessagesCreateActivity.this, DirectMessagesConversationActivity.class);
                navigationIntent.putExtra("friendUsername", result.to_username);
                startActivity(navigationIntent);
            }
        }
    }
}
