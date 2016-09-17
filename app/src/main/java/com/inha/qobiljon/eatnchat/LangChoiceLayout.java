package com.inha.qobiljon.eatnchat;

import android.content.Context;
import android.content.res.Configuration;
import android.support.v7.app.AppCompatDialog;
import android.view.View;
import android.view.ViewGroup;

import java.util.Locale;


public class LangChoiceLayout extends AppCompatDialog implements android.view.View.OnClickListener {
    @Override
    protected void onStart() {
        super.onStart();

        setContentView(R.layout.layout_languages);
        getWindow().setBackgroundDrawableResource(R.drawable.dialog_drawable);
        getWindow().setWindowAnimations(R.style.PopupAnimation);
        setTitle("Set up language");

        initializeComponents();
    }

    public LangChoiceLayout(Context context) {
        super(context);
    }

    private void initializeComponents() {
        ViewGroup root = (ViewGroup) findViewById(R.id.root_layout_languages);
        for (int n = 0; n < root.getChildCount(); n++)
            root.getChildAt(n).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        String language = view.getTag().toString();
        Lang.setLang(language, getContext());

        Locale locale = new Locale(language);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        Context context = getContext();
        context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
        dismiss();
    }
}

