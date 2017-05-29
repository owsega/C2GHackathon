package com.owsega.c2ghackathon;

import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.DatePicker;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * @author Seyi Owoeye. Created on 2/6/17.
 */
public class DatePickerHelper implements OnDateSetListener {

    private TextView tvCallback;
    private Date dateCallback;
    private DateFormat dateFormat2 = new SimpleDateFormat("dd/mm/yyyy");

    void showDatePicker(@NonNull TextView results, Date dateCallback,
                        @Nullable Long minDate, @Nullable Long maxDate) {
        this.tvCallback = results;
        this.dateCallback = dateCallback;

        showDialog(tvCallback.getContext(), this, minDate, maxDate);
    }

    private void showDialog(Context context, OnDateSetListener listener,
                            @Nullable Long minDate, @Nullable Long maxDate) {
        //Use the current date as the default date in the date picker
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dpd = new DatePickerDialog(context, listener, year, month, day);
        DatePicker dp = dpd.getDatePicker();
        if (minDate != null) dp.setMinDate(minDate);
        if (maxDate != null) dp.setMaxDate(maxDate);
        dpd.show();
    }

    public void showDatePicker(Context context, OnDateSetListener listener,
                               @Nullable Long minDate, @Nullable Long maxDate) {
        showDialog(context, listener, minDate, maxDate);
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month);
        c.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        tvCallback.setText(dateFormat2.format(c.getTime()));
        tvCallback = null;
        if (dateCallback != null) dateCallback = c.getTime();
    }
}