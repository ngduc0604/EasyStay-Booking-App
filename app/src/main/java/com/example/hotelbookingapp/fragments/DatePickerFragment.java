package com.example.hotelbookingapp.fragments;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.util.Calendar;


public class DatePickerFragment extends DialogFragment {

    private static final String ARG_MIN_DATE = "min_date";

    private final DatePickerDialog.OnDateSetListener listener;

    public DatePickerFragment(DatePickerDialog.OnDateSetListener listener) {
        this.listener = listener;
    }

    public static DatePickerFragment newInstance(DatePickerDialog.OnDateSetListener listener, long minDateMillis) {
        DatePickerFragment fragment = new DatePickerFragment(listener);
        Bundle args = new Bundle();
        args.putLong(ARG_MIN_DATE, minDateMillis);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Calendar c = Calendar.getInstance();
        long minDate = getArguments() != null ? getArguments().getLong(ARG_MIN_DATE, System.currentTimeMillis() - 1000) : System.currentTimeMillis() - 1000;
        c.setTimeInMillis(minDate);

        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(), listener,
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.getDatePicker().setMinDate(minDate);
        return datePickerDialog;
    }
}


