package com.inha.qobiljon.eatnchat;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.iqregister.AccountManager;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        InitializeComponents();
    }


    // region Variables
    private LinearLayout loginActionChoiceLayout;
    private LinearLayout userInputLayout;
    private LinearLayout mainLayout;
    private CheckBox saveCheckBox;
    private Button buttonAction;
    private ProgressBar progress;
    private boolean inputScreenShown = false;
    private boolean performLogin = false;
    // endregion


    private void InitializeComponents() {
        Tools.init(getApplicationContext());
        Credentials.init(getApplicationContext());

        if (Tools.sprefs.contains("language")) {
            Lang.setLang(Tools.sprefs.getString("language", null), this);
        } else {

            String defaultLanguage = getString(R.string.default_language);
            Lang.setLang(defaultLanguage, this);
        }

        mainLayout = (LinearLayout) findViewById(R.id.main_layout_login);
        loginActionChoiceLayout = (LinearLayout) LoginActivity.this.findViewById(R.id.loginaction_layout_login);
        userInputLayout = (LinearLayout) LoginActivity.this.findViewById(R.id.userinput_layout_login);
        buttonAction = (Button) LoginActivity.this.findViewById(R.id.authorize_button_main);
        saveCheckBox = (CheckBox) LoginActivity.this.findViewById(R.id.savecredentials_checkbox_login);
        progress = (ProgressBar) LoginActivity.this.findViewById(R.id.progress_progress_login);

        if (Tools.sprefs.contains("username") && Tools.sprefs.contains("password")) {
            EditText usernameText = (EditText) findViewById(R.id.username_text_login);
            EditText passwordText = (EditText) findViewById(R.id.password_text_login);

            usernameText.setText(Tools.sprefs.getString("username", null));
            passwordText.setText(Tools.sprefs.getString("password", null));

            performLogin = true;
            buttonAction.performClick();
        }
    }

    public void showPassword(View view) {
        EditText password = (EditText) LoginActivity.this.findViewById(R.id.password_text_login);

        if (password.getInputType() == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)
            password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        else
            password.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);

        password.setSelection(password.length());
    }

    private void toggleLoginPage(boolean performLogin) {
        final boolean animBool = performLogin;
        if (inputScreenShown) {
            Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_out_reverse);
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    Animation animation1 = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_in_reverse);
                    userInputLayout.setVisibility(View.GONE);
                    loginActionChoiceLayout.setVisibility(View.VISIBLE);
                    mainLayout.clearAnimation();
                    mainLayout.startAnimation(animation1);

                    userInputLayout.findViewById(R.id.name_layout_login).setVisibility(inputScreenShown || !animBool ? View.VISIBLE : View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            mainLayout.startAnimation(animation);
        } else {
            userInputLayout.findViewById(R.id.name_layout_login).setVisibility(inputScreenShown || !animBool ? View.VISIBLE : View.GONE);

            Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_out);
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    Animation animation1 = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_in);
                    loginActionChoiceLayout.setVisibility(View.GONE);
                    userInputLayout.setVisibility(View.VISIBLE);
                    mainLayout.clearAnimation();
                    mainLayout.startAnimation(animation1);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            mainLayout.startAnimation(animation);
        }

        if (!inputScreenShown)
            buttonAction.setText(performLogin ? R.string.log_in : R.string.create_profile);

        inputScreenShown = !inputScreenShown;
    }

    public void actionButtonClick(View view) {
        switch ((String) view.getTag()) {
            case "Login":
                toggleLoginPage(performLogin = true);
                break;
            case "Register":
                toggleLoginPage(performLogin = false);
                break;
            default:
                Log.e("PROCESS", "ACTION FOR BUTTON (TAG): " + view.getTag());
                break;
        }
    }

    @Override
    public void onBackPressed() {
        if (inputScreenShown)
            toggleLoginPage(true);
        else
            super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        /*switch (resultCode) {
            case Tools.CODE_FINISH:
                super.onBackPressed();
                break;
            default:
                break;
        }*/
    }

    public void authorizeClick(View view) {
        EditText usernameText = (EditText) findViewById(R.id.username_text_login);
        EditText passwordText = (EditText) findViewById(R.id.password_text_login);
        EditText nameText = (EditText) findViewById(R.id.name_text_login);

        final String username = usernameText.getText().toString();
        final String password = passwordText.getText().toString();
        final String name = nameText.getText().toString();

        Executors.newCachedThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                // region Set up connection to server
                final String host = getString(R.string.host);
                final String port = getString(R.string.port);
                final String service = getString(R.string.service);

                XMPPTCPConnectionConfiguration connConfig = XMPPTCPConnectionConfiguration
                        .builder()
                        .setHost(host)
                        .setPort(Integer.parseInt(port))
                        .setServiceName(service)
                        .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)
                        .build();

                Tools.connection = new XMPPTCPConnection(connConfig);
                try {
                    Tools.connection.connect();
                    Log.i("PROCESS", "Connected to " + Tools.connection.getHost());
                } catch (XMPPException | SmackException | IOException ex) {
                    ex.printStackTrace();
                    Log.e("PROCESS", "Failed to connect to " + Tools.connection.getHost());
                    return;
                }
                // endregion

                // TODO: show loading progressview until logged in or profile created

                if (performLogin) {
                    // region Perform logon operations
                    try {
                        Tools.connection.login(username, password);
                        Log.i("PROCESS", "Logged in as " + Tools.connection.getUser());

                        Presence presence = new Presence(Presence.Type.available, "Online", 1, Presence.Mode.available);
                        Tools.connection.sendStanza(presence);
                        Log.i("PROCESS", "Status is set to available (online)");

                        startActivityForResult(new Intent(getApplicationContext(), MainActivity.class), Tools.CODE_DEFAULT);
                    } catch (XMPPException | IOException | SmackException ex) {
                        Log.e("PROCESS", "Failed to log in as " + username);
                    }
                    // endregion
                } else {
                    // region Perform registration operations
                    try {
                        HashMap<String, String> attr = new HashMap<>();
                        attr.put("name", name);

                        AccountManager manager = AccountManager.getInstance(Tools.connection);
                        manager.createAccount(username, password, attr);

                        performLogin = true;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                buttonAction.performClick();
                            }
                        });
                    } catch (Exception e) {
                        // TODO: show that registration has failed
                        Log.e("ERROR", "Failed to create account! " + e.getMessage());
                    }
                    // endregion
                }

                if (saveCheckBox.isChecked()) {

                }
            }
        });
    }

    private static class Credentials {
        public static SQLiteDatabase db;
        public static boolean initialized = false;
        public static Context context;

        public static void init(Context context) throws SQLiteException {
            if (initialized)
                return;

            Credentials.context = context;
            Credentials.db = context.openOrCreateDatabase("UserSettings.db", MainActivity.MODE_PRIVATE, null);

            db.execSQL("create table if not exists settings(name varchar(200) primary key, value varchar(200));");

            initialized = true;
        }

        public static void close() {
            db.close();
            db = null;
        }

        public static boolean set(String name, String value) {
            boolean res;

            if (res = get(name) != null)
                db.execSQL("update settings set value='" + value + "' where name='" + name + "'", new String[]{});

            return res;
        }

        public static boolean save(String name, String value) {
            boolean res;

            if (res = get(name) == null)
                db.execSQL("insert into settings(name, value) values('" + name + "', '" + value + "')");

            return res;
        }

        public static String get(String name) {
            String ret;
            Cursor cursor = db.rawQuery("select value from settings where name='" + name + "'", new String[]{});

            if (cursor.moveToFirst())
                ret = cursor.getString(cursor.getColumnIndex("value"));
            else ret = null;
            cursor.close();

            return ret;
        }
    }
}
