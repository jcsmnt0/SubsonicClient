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

import android.os.Parcel;
import android.os.Parcelable;

public class Download implements Parcelable {
	private boolean mStarted = false, mCompleted = false;
	long progress;
	final String url, name, savePath, username, password;

	void setStarted() {
		mStarted = true;
	}

	void setCompleted() {
		mCompleted = true;
	}

	Download(final String url, final String name, final String savePath, final String username,
	         final String password) {
		this.url = url;
		this.name = name;
		this.savePath = savePath;
		this.username = username;
		this.password = password;
	}

	String getProgressString() {
		if (progress >= ByteSize.TB.size) {
				return String.format("%.2fTB", (double) progress / ByteSize.TB.size);
			} else if (progress >= ByteSize.GB.size) {
				return String.format("%.2fGB", (double) progress / ByteSize.GB.size);
			} else if (progress >= ByteSize.MB.size) {
				return String.format("%.2fMB", (double) progress / ByteSize.MB.size);
			} else if (progress >= ByteSize.KB.size) {
				return String.format("%.2fKB", (double) progress / ByteSize.KB.size);
			}
			else return String.format("%dB", progress);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(url);
		out.writeString(name);
		out.writeString(savePath);
		out.writeString(username);
		out.writeString(password);
		out.writeLong(progress);
		out.writeInt(mCompleted ? 1 : 0);
	}

	public static final Parcelable.Creator<Download> CREATOR = new Parcelable.Creator<Download>() {
		@Override
		public Download createFromParcel(Parcel in) {
			return new Download(in);
		}

		@Override
		public Download[] newArray(int size) {
			return new Download[size];
		}
	};

	private Download(Parcel in) {
		url = in.readString();
		name = in.readString();
		savePath = in.readString();
		username = in.readString();
		password = in.readString();
		progress = in.readLong();
		mCompleted = in.readInt() == 1;
	}

	private static enum ByteSize {
		KB(1L << 10),
		MB(1L << 20),
		GB(1L << 30),
		TB(1L << 40); // hopefully nobody is downloading files bigger than 1024 terabytes

		private final long size;

		private ByteSize(long size) {
			this.size = size;
		}
	}
}
