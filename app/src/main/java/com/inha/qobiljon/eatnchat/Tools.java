package com.inha.qobiljon.eatnchat;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.provider.Telephony;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.jivesoftware.smack.AbstractXMPPConnection;

@SuppressWarnings("unused")
public class Tools {
    public static final int
            CODE_DEFAULT = 0,
            CODE_FINISH = 1;

    public static String username;
    public static AbstractXMPPConnection connection;
    public static String delimiter = "|";

    public static boolean isFirstTime;
    public static SharedPreferences sprefs;
    public static Context context;

    public static void init(Context context) {
        Tools.context = context;
        sprefs = context.getSharedPreferences("Settings", Context.MODE_PRIVATE);
    }
}

class Credentials {
    private static SQLiteDatabase db;
    private static boolean initialized = false;
    private static Context context;

    private static String username = "username";
    private static String password = "password";


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


    public static void setUsername(String newUsername) {
        if (usernameExists())
            db.execSQL(String.format("update settings set value='%s' where name='%s'", newUsername, username));
        else
            save(username, newUsername);
    }

    public static void setPassword(String newPassword) {
        if (passwordExists())
            db.execSQL(String.format("update settings set value='%s' where name='%s'", newPassword, password));
        else
            save(password, newPassword);
    }


    public static String getUsername() {
        return get(username);
    }

    public static String getPassword() {
        return get(password);
    }


    public static void removeUsername() {
        if (usernameExists())
            remove(username);
    }

    public static void removePassword() {
        if (passwordExists())
            remove(password);
    }


    public static boolean usernameExists() {
        return get(username) != null;
    }

    public static boolean passwordExists() {
        return get(password) != null;
    }


    private static void save(String name, String value) {
        db.execSQL("insert into settings(name, value) values('" + name + "', '" + value + "')");
    }

    private static void remove(String name) {
        db.execSQL(String.format("delete from settings where name='%s'", name));
    }

    private static String get(String name) {
        String ret;
        Cursor cursor = db.rawQuery("select value from settings where name='" + name + "'", new String[]{});

        if (cursor.moveToFirst())
            ret = cursor.getString(cursor.getColumnIndex("value"));
        else ret = null;
        cursor.close();

        return ret;
    }
}

