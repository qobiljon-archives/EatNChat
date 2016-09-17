package com.inha.qobiljon.eatnchat;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

class Lang {
    public static void setLang(String language, Context context) throws NullPointerException {
        SharedPreferences.Editor editor = Tools.sprefs.edit();
        editor.putString("language", language);
        editor.apply();
    }

    public static String getLang() {
        return Tools.sprefs.getString("language", null);
    }
}

class MessageHistory {

    // region Variables
    private static SQLiteDatabase database;
    public static boolean initialized = false;
    // endregion

    public static void initDatabase(Context context) {
        database = context.openOrCreateDatabase("Messaging", Context.MODE_PRIVATE, null);

        database.execSQL("create table if not exists Buddies(username varchar(200), avatarFileName varchat(200));");

        Cursor cursor = database.query("Buddies", new String[]{"*"}, null, null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                Buddy buddy = Buddy.newBuddy(cursor.getString(cursor.getColumnIndex("username")));
            }
            while (cursor.moveToNext());
        }
        cursor.close();

        initialized = true;
    }

    public static void inflateMessage(final Activity activity, final ViewGroup container, final ScrollView scrollView, final Message message, final boolean isUserMessage, final boolean sent) {
        final Animation userAnimation, serverAnimation;
        userAnimation = AnimationUtils.loadAnimation(activity.getApplicationContext(), R.anim.user_message_inflate);
        serverAnimation = AnimationUtils.loadAnimation(activity.getApplicationContext(), R.anim.server_message_inflate);

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.getLayoutInflater().inflate(isUserMessage ? R.layout.item_message_user : R.layout.item_message_server, container);
                container.getChildAt(container.getChildCount() - 1).startAnimation(isUserMessage ? userAnimation : serverAnimation);
                View messageView = container.getChildAt(container.getChildCount() - 1);
                TextView messageText = (TextView) messageView.findViewById(R.id.text_message);
                messageText.setText(message.getBody());

                if (isUserMessage) {
                    ImageView stateBubble = (ImageView) messageView.findViewById(R.id.statebubble_message);
                    if (sent)
                        stateBubble.setVisibility(View.VISIBLE);
                }

                scrollView.scrollTo(0, scrollView.getBottom());
            }
        });
    }

    public static void inflateLog(final Activity activity, final ViewGroup container, final String logData) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.getLayoutInflater().inflate(R.layout.item_text_log, container);
                TextView logText = (TextView) container.getChildAt(container.getChildCount() - 1);
                logText.setText(logData);
            }
        });
    }
}

class Buddy {

    // region Constructor
    private Buddy(String username) {
        this.username = username;
        this.userJid = convertToUserJid(username, connection);
    }

    public static Buddy newBuddy(String username) {
        if (buddies.containsKey(username))
            return buddies.get(username);
        else
            return new Buddy(username);
    }
    //endregion

    // region Variables
    // region Static objects
    private static AbstractXMPPConnection connection;
    private static Roster roster;
    private static HashMap<String, Buddy> buddies = new HashMap<>();
    // endregion

    // region Messaging objects
    private ArrayList<Message> readMessages = new ArrayList<>();
    private ArrayList<Message> unreadMessages = new ArrayList<>();
    private int unreadCount = 0;
    // endregion

    // region User information variables
    private String username;
    private String userJid;
    // endregion

    // region UI variables
    private ArrayList<BuddyUIComponents> buddyUIComponents = new ArrayList<>();
    // endregion

    // region MessageAction variables
    private List<MessageAction> messageActions = new ArrayList<>();
    // endregion
    // endregion

    // region Initialization and working on buddy objects
    public static void init(AbstractXMPPConnection connection) {
        Buddy.connection = connection;
        roster = Roster.getInstanceFor(connection);

        connection.addSyncStanzaListener(new StanzaListener() {
            @Override
            public void processPacket(Stanza Stanza) throws SmackException.NotConnectedException {
                Message message = (Message) Stanza;
                if (message.getBody() != null) {
                    String fromName = message.getFrom();
                    fromName = fromName.substring(0, fromName.indexOf('@'));
                    buddies.get(fromName).addNewMessage(message);
                    Log.i("PROCESS", "Got text [" + message.getBody() + "] from [" + fromName + "]");
                }
            }
        }, MessageTypeFilter.CHAT);

        roster.addRosterListener(new RosterListener() {
            @Override
            public void entriesAdded(Collection<String> addresses) {

            }

            @Override
            public void entriesUpdated(Collection<String> addresses) {

            }

            @Override
            public void entriesDeleted(Collection<String> addresses) {

            }

            @Override
            public void presenceChanged(Presence presence) {
                String senderJid = presence.getFrom();

                if (presence.getError() == null) {
                    String sender = convertToUsername(senderJid);
                    Buddy buddy = buddies.get(sender);
                    buddy.setPresence(presence);
                } else {
                    Log.e("XMPPError", presence.getError().toString());
                }
            }
        });
    }

    public static Buddy getBuddy(String username) {
        return buddies.get(username);
    }

    public void inflateBuddy(Activity activity, ViewGroup container, View.OnClickListener listener) {
        activity.getLayoutInflater().inflate(R.layout.item_contact, container);
        ViewGroup contactView = (ViewGroup) container.getChildAt(container.getChildCount() - 1);
        TextView nameText = (TextView) contactView.findViewById(R.id.name_text_contact);
        TextView statusText = (TextView) contactView.findViewById(R.id.status_text_contact);

        nameText.setText(getName());

        statusText.setText(getStatus());

        contactView.setTag(this);
        contactView.setOnClickListener(listener);

        buddyUIComponents.add(new BuddyUIComponents(activity, contactView));
    }

    public static void inflateTempBuddy(LayoutInflater inflater, ViewGroup container, String username, String name, byte[] avatar, View.OnClickListener listener) {
        inflater.inflate(R.layout.item_contact, container);
        ViewGroup contactView = (ViewGroup) container.getChildAt(container.getChildCount() - 1);
        TextView nameText = (TextView) contactView.findViewById(R.id.name_text_contact);
        TextView statusText = (TextView) contactView.findViewById(R.id.status_text_contact);

        nameText.setText(name);
        statusText.setText("Send friend request");
        contactView.setTag(username);
        contactView.setOnClickListener(listener);
    }
    // endregion

    // region Operations on message listeners
    public void addMessageListener(MessageAction action) {
        messageActions.add(action);
    }

    public void clearMessageListeners() {
        messageActions.clear();
    }

    public void removeMessageListener(Runnable messageListener) {
        messageActions.remove(messageListener);
    }
    // endregion

    // region Actions on messages
    public void addNewMessage(Message message) {
        unreadMessages.add(message);
        setUnreadCount();

        for (MessageAction action : messageActions)
            action.doWork(this);
    }

    public void addNewMessages(ArrayList<Message> messages) {
        unreadMessages.addAll(messages);
        setUnreadCount();

        for (MessageAction action : messageActions)
            action.doWork(this);
    }

    public Message readUnreadMessage() {
        if (unreadMessages.size() > 0) {
            Message message = unreadMessages.remove(0);
            readMessages.add(message);
            setUnreadCount();
            return message;
        } else return null;
    }

    public ArrayList<Message> readUnreadMessages() {
        if (unreadMessages.size() > 0) {
            readMessages.addAll(unreadMessages);
            ArrayList<Message> theMessages = (ArrayList<Message>) unreadMessages.clone();
            unreadMessages.clear();
            setUnreadCount();
            return theMessages;
        } else return null;
    }

    private void setUnreadCount() {
        unreadCount = unreadMessages.size();

        for (final BuddyUIComponents components : buddyUIComponents)
            components.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (unreadCount == 0) {
                        components.getUnreadCountView().setVisibility(View.GONE);
                    } else {
                        if (components.getUnreadCountView().getVisibility() == View.GONE)
                            components.getUnreadCountView().setVisibility(View.VISIBLE);
                        components.getUnreadCountView().setText(String.valueOf(unreadCount));
                    }
                }
            });
    }
    // endregion

    // region Setters and Getters
    public ArrayList<Message> getReadMessages() {
        return readMessages;
    }

    public ArrayList<Message> getUnreadMessages() {
        return unreadMessages;
    }

    public String getUserJid() {
        return userJid;
    }

    public String getUsername() {
        return username;
    }

    public String getName() {
        return roster.getEntry(username).getName();
    }

    public ViewGroup getContactView(ViewGroup parent) {
        for (BuddyUIComponents components : buddyUIComponents) {
            if (components.getParentView().equals(parent))
                return components.getContactView();
        }
        return null;
    }

    public String getStatus() {
        return roster.getPresence(username).getStatus();
    }

    public boolean isAvailable() {
        return roster.getPresence(username).isAvailable();
    }

    public void setPresence(final Presence presence) {
        if (buddyUIComponents.size() == 0)
            return;

        buddyUIComponents.get(0).getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (BuddyUIComponents components : buddyUIComponents)
                    components.getStatusView().setText(presenceToString(presence));
            }
        });
    }
    // endregion

    // region Actions on friendship
    public boolean addFriend() {
        if (isFriend())
            return false;

        try {
            roster.createEntry(getUsername(), null, null);
            buddies.put(getUsername(), this);
            return true;
        } catch (SmackException | XMPPException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean isFriend() {
        return buddies.get(username) != null;
    }

    public boolean removeFriend() {
        if (!isFriend())
            return false;

        try {
            roster.removeEntry(roster.getEntry(getUsername()));
            buddies.remove(this);
            return true;
        } catch (SmackException | XMPPException e) {
            e.printStackTrace();
            return false;
        }
    }
    // endregion

    // region Useful functions
    public static boolean isValidUserJid(String userJid) {
        return userJid.contains("@");
    }

    public static String convertToUserJid(String username, AbstractXMPPConnection connection) {
        return isValidUserJid(username) ? username : String.format("%s@%s", username, connection.getServiceName());
    }

    public static String convertToUsername(String userJid) {
        return isValidUserJid(userJid) ? userJid.substring(0, userJid.indexOf('@')) : userJid;
    }

    public static String presenceToString(Presence presence) {
        Presence.Mode mode = presence.getMode();
        boolean available = presence.isAvailable();

        String res;

        if (mode == Presence.Mode.dnd)
            res = "Busy";
        else if (mode == Presence.Mode.away || mode == Presence.Mode.xa)
            res = "Away";
        else if (available)
            res = "Online";
        else
            res = "Offline";

        return res;
    }

    public static Presence parsePresence(String status) {
        Presence res = new Presence(Presence.Type.available);

        if (status.equals("Busy"))
            res.setMode(Presence.Mode.dnd);
        else if (status.equals("Away"))
            res.setMode(Presence.Mode.away);
        else if (!status.equals("Online"))
            res.setType(Presence.Type.subscribe);

        return res;
    }

    public static void instantiate(String username, ArrayList<Message> readMessages, ArrayList<Message> unreadMessages) {
        Buddy buddy = new Buddy(username);

        buddy.unreadMessages.addAll(unreadMessages);
        buddy.readMessages.addAll(readMessages);

        buddy.addFriend();
    }

    public static void loadOfflineBuddies(Context context) {
        if(!MessageHistory.initialized)
            MessageHistory.initDatabase(context);
    }
    // endregion
}

class BuddyUIComponents{
    public BuddyUIComponents(Activity activity, ViewGroup contactView) {
        this.activity = activity;
        this.contactView = contactView;
        this.parent = (ViewGroup) contactView.getParent();
        this.unreadCount = (TextView) contactView.findViewById(R.id.unreadcount_text_contact);
        this.statusText = (TextView) contactView.findViewById(R.id.status_text_contact);
        this.avatarButton = (ImageButton) contactView.findViewById(R.id.avatar_button_contact);
    }

    // region Variables
    private Activity activity;
    private ViewGroup contactView;
    private TextView unreadCount;
    private TextView statusText;
    private ImageButton avatarButton;
    private ViewGroup parent;
    // endregion

    public ViewGroup getContactView() {
        return contactView;
    }

    public Activity getActivity() {
        return activity;
    }

    public TextView getUnreadCountView() {
        return unreadCount;
    }

    public ImageButton getAvatarView() {
        return avatarButton;
    }

    public TextView getStatusView() {
        return statusText;
    }

    public ViewGroup getParentView() {
        return parent;
    }
}
