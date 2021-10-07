/*
 * Giua App
 * Android app to view data from the giua@school workbook
 * Copyright (C) 2021 - 2021 Hiem, Franck1421 and contributors
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see https://www.gnu.org/licenses/.
 */

package com.giua.app.ui.fragments.absences;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.res.ResourcesCompat;

import com.giua.app.GlobalVariables;
import com.giua.app.LoggerManager;
import com.giua.app.R;
import com.giua.objects.Absence;
import com.giua.webscraper.GiuaScraper;

public class AbsenceView extends ConstraintLayout {

    public Absence absence;
    public String justifyText = "";
    private final Context context;
    private final OnClickListener onJustifyClick;

    public AbsenceView(@NonNull Context context, @Nullable AttributeSet attrs, Absence absence, OnClickListener onJustifyClick) {
        super(context, attrs);

        this.absence = absence;
        this.context = context;
        this.onJustifyClick = onJustifyClick;

        initializeComponent(context, onJustifyClick);
    }

    private void initializeComponent(Context context, OnClickListener onJustifyClick) {
        LoggerManager loggerManager = new LoggerManager("AbsenceView", context);

        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        layoutInflater.inflate(R.layout.view_absence, this);

        TextView tvDate = findViewById(R.id.absence_view_date);
        TextView tvType = findViewById(R.id.absence_view_type);
        TextView tvText = findViewById(R.id.absence_view_text);
        TextView tvJustify = findViewById(R.id.absence_view_btn_justife);
        EditText etJustifyText = findViewById(R.id.absence_view_justify_text);

        tvDate.setText(absence.date);
        tvType.setText(absence.type);
        tvText.setText(absence.notes);
        tvDate.setBackground(null);
        tvType.setBackground(null);
        tvText.setBackground(null);
        tvJustify.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.corner_radius_10dp, context.getTheme()));

        if (absence.notes.length() == 0)
            tvText.setVisibility(GONE);

        if (absence.isJustified) {
            if (absence.isModificable && GlobalVariables.gS.getUserTypeEnum() == GiuaScraper.userTypes.PARENT) {
                tvJustify.setText("Modifica");
                tvJustify.setBackgroundTintList(getResources().getColorStateList(R.color.main_color, context.getTheme()));
                tvJustify.setForeground(ResourcesCompat.getDrawable(getResources(), R.drawable.ripple_effect, context.getTheme()));
                tvJustify.setOnClickListener(this::onJustify);
            } else {
                tvJustify.setText("Giustificata");
                etJustifyText.setVisibility(INVISIBLE);
            }
        } else {
            if (GlobalVariables.gS.getUserTypeEnum() == GiuaScraper.userTypes.PARENT) {
                tvJustify.setText("Giustifica");
                tvJustify.setBackgroundTintList(getResources().getColorStateList(R.color.bad_vote_lighter, context.getTheme()));
                tvJustify.setForeground(ResourcesCompat.getDrawable(getResources(), R.drawable.ripple_effect, context.getTheme()));
                tvJustify.setOnClickListener(this::onJustify);
            } else {
                tvJustify.setText("Da giustificare");
                tvJustify.setBackgroundTintList(getResources().getColorStateList(R.color.bad_vote_lighter, context.getTheme()));
                etJustifyText.setVisibility(INVISIBLE);
            }
        }

    }

    private void onJustify(View view) {
        justifyText = ((EditText) findViewById(R.id.absence_view_justify_text)).getText().toString();
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        onJustifyClick.onClick(this);
    }
}
