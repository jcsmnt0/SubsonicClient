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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import com.actionbarsherlock.app.SherlockDialogFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;

// TODO: implement .show(FragmentActivity) instead of using fragmentActivity.showDialog(dialog)
class AlertDialogFragment extends SherlockDialogFragment {
	private final String title, message, positiveString, negativeString, neutralString;
	private final DialogInterface.OnClickListener onClickListener;

	private AlertDialogFragment(final Builder b) {
		this.onClickListener = b.onClickListener;

		this.title = b.title;
		this.message = b.message;
		this.positiveString = b.positiveString;
		this.negativeString = b.negativeString;
		this.neutralString = b.neutralString;
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

	public AlertDialogFragment(final Context context, final int title, final int message) {
		this(new Builder(context)
				.setTitle(title)
				.setMessage(message)
				.setNeutralButton(R.string.ok));
	}

	void show(SherlockFragmentActivity fragmentActivity) {
		this.show(fragmentActivity.getSupportFragmentManager(), "dialog");
	}

	protected static class Builder {
		private String title = null, message = null, positiveString = null, negativeString = null, neutralString = null;
		private DialogInterface.OnClickListener onClickListener = null;
		private final Context context;

		Builder(Context context) {
			this.context = context;
		}

		Builder setTitle(String title) {
			this.title = title;
			return this;
		}
		Builder setTitle(int resId) {
			this.setTitle(context.getString(resId));
			return this;
		}

		Builder setMessage(String message) {
			this.message = message;
			return this;
		}
		Builder setMessage(int resId) {
			this.setMessage(context.getString(resId));
			return this;
		}

		Builder setPositiveButton(String positiveString) {
			this.positiveString = positiveString;
			return this;
		}
		Builder setPositiveButton(int resId) {
			this.setPositiveButton(context.getString(resId));
			return this;
		}

		Builder setNegativeButton(String negativeString) {
			this.negativeString = negativeString;
			return this;
		}
		Builder setNegativeButton(int resId) {
			this.setNegativeButton(context.getString(resId));
			return this;
		}

		Builder setNeutralButton(String neutralString) {
			this.neutralString = neutralString;
			return this;
		}
		Builder setNeutralButton(int resId) {
			this.setNeutralButton(context.getString(resId));
			return this;
		}

		Builder setOnClickListener(DialogInterface.OnClickListener onClickListener) {
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
		AlertDialog.Builder b = new AlertDialog.Builder(this.getSherlockActivity());

		// string resources take precedence over string literals, because why not
		if (this.title != null)
			b.setTitle(this.title);

		if (this.message != null)
			b.setMessage(this.message);

		if (this.positiveString != null)
			b.setPositiveButton(this.positiveString, this.onClickListener);

		if (this.negativeString != null)
			b.setNegativeButton(this.negativeString, this.onClickListener);

		if (this.neutralString != null)
			b.setNeutralButton(this.neutralString,  this.onClickListener);

		return b.create();
	}
}
