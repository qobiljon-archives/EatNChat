package com.inha.qobiljon.eatnchat;

import android.content.Context;
import android.support.v7.app.AppCompatDialog;
import android.view.View;
import android.view.ViewGroup;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;

public class AvailabilityLayout extends AppCompatDialog implements android.view.View.OnClickListener {

    public AvailabilityLayout(Context context) {
        super(context);
    }

    // region Variables
    private Presence presence;
    private Presence.Mode origMode;
    // endregion

    private void initializeComponents() {
        Roster roster = Roster.getInstanceFor(Tools.connection);
        presence = roster.getPresence(Tools.connection.getUser());
        origMode = presence.getMode();

        ViewGroup root = (ViewGroup) findViewById(R.id.root_layout_availability);
        for (int n = 0; n < root.getChildCount(); n++)
            root.getChildAt(n).setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        setContentView(R.layout.layout_availability);
        getWindow().setBackgroundDrawableResource(R.drawable.dialog_drawable);
        getWindow().setWindowAnimations(R.style.PopupAnimation);
        setTitle(getContext().getString(R.string.layout_availability_title));

        initializeComponents();
    }

    @Override
    public void onClick(View view) {
        Presence.Mode mode = Presence.Mode.fromString((String) view.getTag());
        if (mode != origMode)
            try {
                presence.setMode(mode);
                Tools.connection.sendStanza(presence);
            } catch (SmackException.NotConnectedException e) {
                e.printStackTrace();
            }
        dismiss();
    }
}
