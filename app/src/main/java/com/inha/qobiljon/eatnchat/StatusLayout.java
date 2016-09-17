package com.inha.qobiljon.eatnchat;

import android.content.Context;
import android.support.v7.app.AppCompatDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;

public class StatusLayout extends AppCompatDialog implements android.view.View.OnClickListener {
    public StatusLayout(Context context) {
        super(context);
    }

    // region Variables
    public boolean done = false;
    private boolean changed = false;

    private EditText statusText;
    private Presence presence;
    // endregion

    private void initializeVariables() {
        Button ok = (Button) findViewById(R.id.confirm_button_status);
        ok.setOnClickListener(this);

        Roster roster = Roster.getInstanceFor(Tools.connection);
        presence = roster.getPresence(Tools.connection.getUser());

        statusText = (EditText) findViewById(R.id.status_text_status);
        statusText.setText(presence.getStatus());
        statusText.setSelection(statusText.getText().length());

        statusText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                changed = true;
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        setContentView(R.layout.layout_status);
        getWindow().setBackgroundDrawableResource(R.drawable.dialog_drawable);
        getWindow().setWindowAnimations(R.style.PopupAnimation);
        setTitle(R.string.layout_status_title);

        initializeVariables();
    }

    @Override
    public void onClick(View view) {
        if (changed)
            try {
                presence.setStatus(statusText.getText().toString());
                Tools.connection.sendStanza(presence);
                done = true;
            } catch (SmackException.NotConnectedException e) {
                e.printStackTrace();
            }
        dismiss();
    }
}
