/*
 * Giua App
 * Android app to view data from the giua@school workbook
 * Copyright (C) 2021 - 2025 Hiem, Franck1421 and contributors
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

package com.giua.app.ui.fragments.disciplinary_notices;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.res.ResourcesCompat;

import com.giua.app.AppUtils;
import com.giua.app.R;
import com.giua.objects.DisciplinaryNotices;
import com.giua.pages.VotesPage;

import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Vector;
public class NoteView extends ConstraintLayout{
    private final String date;
    private final String type;
    private final String details;
    private final String authorOfDetails;
    private final String measures;
    private final int maxQuarterly;

    public NoteView(@NonNull @NotNull Context context, @Nullable @org.jetbrains.annotations.Nullable AttributeSet attrs, List<String> allQuarterliesNames, List<DisciplinaryNotices> allDisciplinaryNotices, String date, String type, String details, String authorOfDetails, String measures) {
        super(context, attrs);

        this.date = date;
        this.type = type;
        this.details = details;
        this.authorOfDetails = authorOfDetails;
        this.measures = measures;

        maxQuarterly = allQuarterliesNames.size();

        initializeComponent(context);

    }

    private void initializeComponent(Context context) {
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        layoutInflater.inflate(R.layout.view_note, this);

        TextView tvType = findViewById(R.id.text_view_notetype);
    }
}


