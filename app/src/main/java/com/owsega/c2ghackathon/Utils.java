package com.owsega.c2ghackathon;

import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.EditText;

/**
 * @author Seyi Owoeye. Created on 5/29/17.
 */

class Utils {

    static void snack(View root, @StringRes int stringId) {
        Snackbar.make(root, stringId, Snackbar.LENGTH_SHORT).show();
    }

    static void setError(EditText editText, String error) {
        editText.setError(error);
        editText.requestFocus();
    }
}
