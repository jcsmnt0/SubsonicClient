/*
 * Copyright (c) 2012, Joseph Casamento
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.casamento.subsonicclient;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import com.actionbarsherlock.app.SherlockDialogFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;

class AlertDialogFragment extends SherlockDialogFragment {
    private final String title, message, positiveString, negativeString, neutralString;
    private final DialogInterface.OnClickListener onClickListener;

    private AlertDialogFragment(final Builder b) {
        onClickListener = b.onClickListener;

        title = b.title;
        message = b.message;
        positiveString = b.positiveString;
        negativeString = b.negativeString;
        neutralString = b.neutralString;
    }

    AlertDialogFragment(final Context context, final String title, final String message) {
        this(new Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setNeutralButton(R.string.ok));
    }
    AlertDialogFragment(final Context context, final int title, final String message) {
        this(new Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setNeutralButton(R.string.ok));
    }

    AlertDialogFragment(final Context context, final int title, final int message) {
        this(new Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setNeutralButton(R.string.ok));
    }

    void show(final SherlockFragmentActivity fragmentActivity) {
        show(fragmentActivity.getSupportFragmentManager(), "dialog");
    }

    protected static class Builder {
        private String title, message, positiveString, negativeString, neutralString;
        private DialogInterface.OnClickListener onClickListener;
        private final Context context;

        Builder(final Context context) {
            this.context = context;
        }

        Builder setTitle(final String title) {
            this.title = title;
            return this;
        }
        Builder setTitle(final int resId) {
            setTitle(context.getString(resId));
            return this;
        }

        Builder setMessage(final String message) {
            this.message = message;
            return this;
        }
        Builder setMessage(final int resId) {
            setMessage(context.getString(resId));
            return this;
        }

        Builder setPositiveButton(final String positiveString) {
            this.positiveString = positiveString;
            return this;
        }
        Builder setPositiveButton(final int resId) {
            setPositiveButton(context.getString(resId));
            return this;
        }

        Builder setNegativeButton(final String negativeString) {
            this.negativeString = negativeString;
            return this;
        }
        Builder setNegativeButton(final int resId) {
            setNegativeButton(context.getString(resId));
            return this;
        }

        Builder setNeutralButton(final String neutralString) {
            this.neutralString = neutralString;
            return this;
        }
        Builder setNeutralButton(final int resId) {
            setNeutralButton(context.getString(resId));
            return this;
        }

        Builder setOnClickListener(final DialogInterface.OnClickListener onClickListener) {
            this.onClickListener = onClickListener;
            return this;
        }

        AlertDialogFragment create() {
            return new AlertDialogFragment(this);
        }
    }

    // fix as per https://code.google.com/p/android/issues/detail?id=19917
    @Override
    public void onSaveInstanceState(final Bundle outState) {
        if (outState.isEmpty()) {
            outState.putBoolean("bug:fix", true);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        final AlertDialog.Builder b = new AlertDialog.Builder(getSherlockActivity());

        // string resources take precedence over string literals, because why not
        if (title != null)
            b.setTitle(title);

        if (message != null)
            b.setMessage(message);

        if (positiveString != null)
            b.setPositiveButton(positiveString, onClickListener);

        if (negativeString != null)
            b.setNegativeButton(negativeString, onClickListener);

        if (neutralString != null)
            b.setNeutralButton(neutralString, onClickListener);

        return b.create();
    }
}
