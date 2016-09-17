package com.inha.qobiljon.eatnchat;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.packet.Message;

import java.util.ArrayList;

public class MessagingActivity extends AppCompatActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messaging);

        InitializeComponents(getIntent().getExtras());
    }


    // region VARIABLES
    private Buddy buddy;
    private EditText inputText;
    private ImageButton sendButton;
    private ViewGroup messagesList;
    private ScrollView scrollView;
    //endregion


    private void InitializeComponents(Bundle args) {
        TextView textName = (TextView) findViewById(R.id.name_text_messaging);
        scrollView = (ScrollView) findViewById(R.id.scroll_scroll_messaging);

        buddy = Buddy.getBuddy(args.getString("Username"));
        textName.setText(buddy.getName());

        buddy.addMessageListener(new MessageAction() {
            @Override
            public void doWork(Buddy buddy) {
                ArrayList<Message> messages = buddy.readUnreadMessages();
                for (Message message : messages){
                    MessageHistory.inflateMessage(MessagingActivity.this, messagesList, scrollView, message, false, false);
                }
            }
        });

        messagesList = (ViewGroup) findViewById(R.id.content_layout);
        sendButton = (ImageButton) findViewById(R.id.send_button_messaging);
        inputText = (EditText) findViewById(R.id.messageinput_text_messaging);

        inputText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String message = inputText.getText().toString();
                //todo: check for a whitespace message
                if (message.length() > 0)
                    sendButton.setVisibility(View.VISIBLE);
                else
                    sendButton.setVisibility(View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    public void showContactProfileInfo(View view) {

    }

    public void showContactAvatar(View view) {

    }

    public void closeMessaging(View view) {

    }

    public void showSmileKeyboard(View view) {

    }

    public void focusOnMessagingScreen(View view) {

    }

    public void sendMessage(View view) {
        String to = buddy.getUserJid();
        String messageBody = inputText.getText().toString();

        Message message = new Message(to, Message.Type.chat);

        message.setBody(messageBody);
        try {
            Tools.connection.sendStanza(message);
            Log.i("Successful", "Message: [" + messageBody + "] sent to [" + to + "]");
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

        MessageHistory.inflateMessage(this, messagesList, scrollView, message, true, true);
        inputText.setText("");
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();

        buddy.clearMessageListeners();
    }
}
