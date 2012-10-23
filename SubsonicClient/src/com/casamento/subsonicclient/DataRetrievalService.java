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

import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.ResultReceiver;

abstract class DataRetrievalService extends IntentService {
    DataRetrievalService() {
        super("DataRetrievalService");
    }

    static final String IN_RESULT_RECEIVER = "result_receiver";
    static final String IN_ACCESS_INFO = "access_info";
    static final String IN_FOLDER = "folder";

    static final String OUT_RESULTS = "results";
    static final String OUT_EXCEPTION = "exception";

    static final int RESULT_CODE_EXCEPTION = -1;
    static final int RESULT_CODE_OK = RESULT_CODE_EXCEPTION + 1;

    @Override
    protected void onHandleIntent(final Intent intent) {
        final Uri uri = intent.getData();
        final ResultReceiver receiver = intent.getParcelableExtra(IN_RESULT_RECEIVER);
        final DataSource.AccessInformation accessInfo = intent.getParcelableExtra(IN_ACCESS_INFO);

        final Bundle out = new Bundle();

        // Retrieve data from the source
        switch (DataSource.getCommandType(uri)) {
            case TOP_LEVEL_FOLDERS: {
                try {
                    final Folder[] results = retrieveTopLevelFolders(accessInfo);
                    out.putParcelableArray(OUT_RESULTS, results);
                    receiver.send(RESULT_CODE_OK, out);
                } catch (final Exception e) {
                    out.putSerializable(OUT_EXCEPTION, e);
                    receiver.send(RESULT_CODE_EXCEPTION, out);
                }
            } break;

            case FOLDER_CONTENTS: {
                final Folder folder = intent.getParcelableExtra(IN_FOLDER);

                try {
                    final FilesystemEntry[] results = retrieveFolderContents(accessInfo, folder);
                    out.putParcelableArray(OUT_RESULTS, results);
                    receiver.send(RESULT_CODE_OK, out);
                } catch (final Exception e) {
                    out.putSerializable(OUT_EXCEPTION, e);
                    receiver.send(RESULT_CODE_EXCEPTION, out);
                }
            } break;

            default:
                throw new IllegalArgumentException("Invalid URI: " + uri);
        }
    }

    // TODO: throw more specific exceptions
    abstract Folder[] retrieveTopLevelFolders(DataSource.AccessInformation accessInfo) throws Exception;
    abstract FilesystemEntry[] retrieveFolderContents(DataSource.AccessInformation accessInfo, final Folder f) throws Exception;
}
