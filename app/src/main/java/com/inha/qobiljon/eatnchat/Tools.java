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
