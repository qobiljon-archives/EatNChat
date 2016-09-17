package com.inha.qobiljon.eatnchat;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smackx.search.ReportedData;
import org.jivesoftware.smackx.search.UserSearchManager;
import org.jivesoftware.smackx.xdata.Form;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        InitializeComponents();
        //TODO: applyLang(); for initially inflated layouts

        int var = 4;
    }


    // region VARIABLES
    // region Constants
    private final String CONTACTS_TODAY = "TodayContacts";
    private final String CONTACTS_MEDIA = "MediaContacts";
    private final String CONTACTS_ALL = "AllContacts";
    private final String SETTINGS = "Settings";
    private final String PROFILE = "MyProfile";
    private final String CONTACTS_SEARCH = "SearchContacts";
    // endregion

    // region UI Components
    private HashMap<String, ViewGroup> contactLists = new HashMap<>();
    private ScrollView[] containers;
    private ImageButton controlButton;
    private View[] controlViews;
    private TextView pageName;

    // region Dialogs
    private StatusLayout statusLayout;
    private AvailabilityLayout availabilityLayout;
    private LangChoiceLayout langChoiceLayout;
    // endregion
    // endregion

    // region The rest
    private boolean controlButtonsVisible = false;
    private int visibleContainerIndex;
    // endregion

    // region Listeners
    private View.OnClickListener buddyClickListener;
    //endregion
    // endregion


    private void InitializeComponents() {
        Buddy.init(Tools.connection);

        // region Set up dialogs
        DialogInterface.OnDismissListener presenceChangedListener = new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (statusLayout.done) {
                    Roster roster = Roster.getInstanceFor(Tools.connection);
                    Presence presence = roster.getPresence(Tools.connection.getUser());
                    setPresence(presence);
                }
            }
        };

        statusLayout = new StatusLayout(this);
        availabilityLayout = new AvailabilityLayout(this);
        langChoiceLayout = new LangChoiceLayout(this);

        statusLayout.setOnDismissListener(presenceChangedListener);
        availabilityLayout.setOnDismissListener(presenceChangedListener);
        langChoiceLayout.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                ImageView langFlag = (ImageView) findViewById(R.id.languageflag_image_settings);
                TextView langName = (TextView) findViewById(R.id.languagename_text_settings);

                int id = getResources().getIdentifier(String.format("flag_%s", Lang.getLang()), "drawable", getPackageName());
                if (langFlag != null) {
                    langFlag.setImageResource(id);
                }

                if (langName != null) {
                    langName.setText(R.string.language_name);
                }

                recreate();
            }
        });
        // endregion

        // region Set control buttons
        controlButton = (ImageButton) findViewById(R.id.authorize_button_main);
        ViewGroup mainContainer = (ViewGroup) findViewById(R.id.container_main);
        pageName = (TextView) findViewById(R.id.pagename_text_main);
        controlViews = new View[]{
                findViewById(R.id.shadow_view_main),
                findViewById(R.id.allcontactscontrol_button_main),
                findViewById(R.id.mediacontrol_button_main),
                findViewById(R.id.profilecontrol_button_main),
                findViewById(R.id.todaycontrol_button_main),
                findViewById(R.id.settingscontrol_button_main)
        };
        // endregion

        // region Set buddyClickListener
        buddyClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Buddy buddy = (Buddy) v.getTag();

                buddy.addFriend();
                Bundle args = new Bundle();
                args.putString("Username", buddy.getUsername());

                Intent intent = new Intent(getApplicationContext(), MessagingActivity.class);
                intent.putExtras(args);

                startActivity(intent);
                // overridePendingTransition(R.anim.fade_out, R.anim.fade_out_reverse);
            }
        };
        // endregion

        // region Load all buddies available from server & Set my status UI
        ArrayList<Buddy> buddies = new ArrayList<>();
        Roster roster = Roster.getInstanceFor(Tools.connection);
        Set<RosterEntry> entrySet = roster.getEntries();
        for (RosterEntry entry : entrySet) {
            buddies.add(Buddy.newBuddy(entry.getUser()));
        }

        Presence presence = roster.getPresence(Tools.connection.getUser());
        setPresence(presence);
        // endregion

        // region Inflate buddy_default_avatar search layout
        containers = new ScrollView[controlViews.length];
        getLayoutInflater().inflate(R.layout.view_container, mainContainer);
        ScrollView container = containers[0] = (ScrollView) mainContainer.getChildAt(mainContainer.getChildCount() - 1);
        getLayoutInflater().inflate(R.layout.layout_search, container);
        contactLists.put(CONTACTS_SEARCH, (ViewGroup) container.findViewById(R.id.content_layout));
        // endregion

        // region Inflate other contacts, settings, and profile layouts
        for (int n = 1; n < containers.length; n++) {
            String type = (String) controlViews[n].getTag();

            getLayoutInflater().inflate(R.layout.view_container, mainContainer);
            container = containers[n] = (ScrollView) mainContainer.getChildAt(mainContainer.getChildCount() - 1);

            // inflate a proper xml layout inside a container
            switch (type) {
                case SETTINGS:
                    getLayoutInflater().inflate(R.layout.layout_settings, container);
                    pageName.setText(R.string.settings);
                    break;
                case PROFILE:
                    getLayoutInflater().inflate(R.layout.layout_profile, container);
                    pageName.setText(R.string.profile);
                    break;
                case CONTACTS_TODAY:
                    getLayoutInflater().inflate(R.layout.layout_contacts, container);
                    container.setVisibility(View.VISIBLE);
                    visibleContainerIndex = n;
                    contactLists.put(type, (ViewGroup) container.findViewById(R.id.content_layout));
                    pageName.setText(R.string.contacts_today);
                    break;
                case CONTACTS_ALL:
                    getLayoutInflater().inflate(R.layout.layout_contacts, container);
                    ViewGroup contactList;
                    contactLists.put(type, contactList = (ViewGroup) container.findViewById(R.id.content_layout));
                    for (Buddy buddy : buddies)
                        buddy.inflateBuddy(this, contactList, buddyClickListener);
                    pageName.setText(R.string.contacts_all);
                    break;
                case CONTACTS_MEDIA:
                    getLayoutInflater().inflate(R.layout.layout_contacts, container);
                    contactLists.put(type, (ViewGroup) container.findViewById(R.id.content_layout));
                    pageName.setText(R.string.contacts_media);
                    break;
                default:
                    Log.e("SKIP", "CONTROL BUTTON ACTION FOR BUTTON (TAG): " + type);
                    break;
            }

            container.setTag(type);
            controlViews[n].setTag(n);
        }
        // endregion
    }


    public void toggleControlButtons(View view) {
        controlButton.setImageResource(controlButtonsVisible ? R.drawable.forwardarrow_icon : R.drawable.backwardarrow_icon);

        int visibility = controlButtonsVisible ? View.GONE : View.VISIBLE;
        for (View controlView : controlViews)
            controlView.setVisibility(visibility);

        controlButtonsVisible = !controlButtonsVisible;
    }

    public void controlButtonClick(View view) {
        controlButton.performClick();
        int index = (int) view.getTag();

        containers[visibleContainerIndex].setVisibility(View.GONE);
        containers[visibleContainerIndex = index].setVisibility(View.VISIBLE);

        String type = (String) containers[visibleContainerIndex].getTag();
        pageName.setText(type);
    }

    public void showContactAvatar(View view) {
        Log.e("PROCESS", "AVATAR CLICKED");

        Bitmap bmp = null;
        try {
            bmp = BitmapFactory.decodeStream(getAssets().open("buddy_default_avatar.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (view instanceof ImageButton)
            ((ImageButton) view).setImageBitmap(bmp);
    }

    public void showContactNotificationActions(View view) {
        Log.e("PROCESS", "NOTIFICATION CLICKED");
    }


    public void openSearchWindow(View view) {
        containers[visibleContainerIndex].setVisibility(View.GONE);
        containers[visibleContainerIndex = 0].setVisibility(View.VISIBLE);
        pageName.setText(R.string.search_new_friends);
    }

    public void searchContacts(View view) {
        EditText searchText = (EditText) findViewById(R.id.search_text_search);
        String text = searchText.getText().toString();

        try {
            UserSearchManager userSearchManager = new UserSearchManager(Tools.connection);
            Form searchForm = userSearchManager.getSearchForm("search." + Tools.connection.getServiceName());
            Form responseForm = searchForm.createAnswerForm();
            responseForm.setAnswer("Username", true);
            responseForm.setAnswer("Name", true);
            responseForm.setAnswer("search", text);

            ReportedData reportedData;
            reportedData = userSearchManager.getSearchResults(responseForm, "search." + Tools.connection.getServiceName());

            ViewGroup container = (ViewGroup) containers[visibleContainerIndex].findViewById(R.id.content_layout);
            container.removeAllViews();

            List<ReportedData.Row> res = reportedData.getRows();
            int count = 0;
            for (ReportedData.Row contact : res) {
                final String username = contact.getValues("Username").get(0);
                if (username.equals("admin")) {
                    count--;
                    continue;
                }
                final String name = contact.getValues("Name").get(0);

                Buddy.inflateTempBuddy(getLayoutInflater(), container, username, name, null, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Buddy buddy = Buddy.newBuddy(username);
                        buddy.addFriend();
                        buddy.inflateBuddy(MainActivity.this, contactLists.get(CONTACTS_ALL), buddyClickListener);
                        buddy.inflateBuddy(MainActivity.this, contactLists.get(CONTACTS_TODAY), buddyClickListener);
                        //TODO: translate "BUDDY ADDED SUCCESSFULLY"
                        Toast.makeText(getApplicationContext(), "BUDDY ADDED SUCCESSFULLY", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            pageName.setText(String.format("%d %s (%s)", count, getString(R.string.search_results), text));
        } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException | SmackException.NotConnectedException e) {
            e.printStackTrace();
        }
    }

    public void openStatusChangeDialog(View view) {
        statusLayout.show();
    }

    public void openAvailabilityChangeDialog(View view) {
        availabilityLayout.show();
    }

    public void openLanguagesChangeDialog(View view) {
        langChoiceLayout.show();
    }

    @Override
    public void onBackPressed() {
        if (controlButtonsVisible)
            controlButton.performClick();
        else {
            setResult(Tools.CODE_FINISH);
            super.onBackPressed();
        }
    }

    public void setPresence(Presence presence) {
        TextView statusText = (TextView) findViewById(R.id.status_text_main);
        statusText.setText(presence.getStatus());
    }

    public void logoutClick(View view) {
        Tools.connection.disconnect();
        onBackPressed();
    }
}
