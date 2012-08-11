/*
 * Copyright (c) 2012, Joseph Casamento
 * All rights reserved.
 *
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

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import static android.content.DialogInterface.OnCancelListener;

public class LoadingDialogFragment extends DialogFragment {
	String message;
	OnCancelListener cancelListener;

	public LoadingDialogFragment(final String message) {
		this.message = message;
	}

	public LoadingDialogFragment(final String message, OnCancelListener cancelListener) {
		this(message);
		this.cancelListener = cancelListener;
	}

	// bug fix as per https://code.google.com/p/android/issues/detail?id=19917
	@Override
	public void onSaveInstanceState(final Bundle outState) {
		if (outState.isEmpty()) {
			outState.putBoolean("bug:fix", true);
		}
		super.onSaveInstanceState(outState);
	}

	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState) {
		final ProgressDialog dialog = new ProgressDialog(this.getActivity());

		dialog.setTitle(R.string.please_wait);
		dialog.setMessage(this.message);
		dialog.setIndeterminate(true);
		dialog.setCancelable(true);
		dialog.setOnCancelListener(this.cancelListener);

		return dialog;
	}
}
