/*
 * Copyright 2013 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gms.drive.sample.querying;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;
import com.google.android.gms.drive.widget.DataBufferAdapter;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;

/**
 * An activity that demonstrates sample queries to filter the files on the
 * currently authenticated user's Google Drive. Application is only authorized
 * to query the files it has opened or created.
 */
public class HomeActivity extends BaseDriveActivity {
    private static final String TAG = "HomeActivity";

    private static Query[] sQueries = new Query[] {// files not shared with me
            new Query.Builder()
                    .addFilter(Filters.not(Filters.eq(SearchableField.MIME_TYPE, "text/plain")))
                    .build(),

            // files shared with me
            new Query.Builder().addFilter(Filters.sharedWithMe()).build(),

            // files with text/plain mimetype
            new Query.Builder()
                    .addFilter(Filters.eq(SearchableField.MIME_TYPE, "text/plain"))
                    .build(),

            // files with a title containing 'a'
            new Query.Builder().addFilter(Filters.contains(SearchableField.TITLE, "a")).build(),

            // files starred and with text/plain mimetype
            new Query.Builder()
                    .addFilter(Filters.and(Filters.eq(SearchableField.MIME_TYPE, "text/plain"),
                            Filters.eq(SearchableField.STARRED, true)))
                    .build(),

            // files with text/plain or text/html mimetype
            new Query.Builder()
                    .addFilter(Filters.or(Filters.eq(SearchableField.MIME_TYPE, "text/html"),
                            Filters.eq(SearchableField.MIME_TYPE, "text/plain")))
                    .build()};

    /**
     * Main drawer layout.
     */
    private DrawerLayout mMainDrawerLayout;

    /**
     * Index of the selected query.
     */
    private int mSelectedIndex = 0;

    private ResultsAdapter mResultsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        String[] titles = getResources().getStringArray(R.array.titles_array);

        ListView listViewQueries = findViewById(R.id.listViewQueries);
        listViewQueries.setAdapter(new ArrayAdapter<>(this, R.layout.row_query, titles));
        listViewQueries.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int i, long arg3) {
                mMainDrawerLayout.closeDrawers();
                mSelectedIndex = i;
                refresh();
            }
        });

        mResultsAdapter = new ResultsAdapter(this);

        ListView listViewFiles = findViewById(R.id.listViewFiles);
        listViewFiles.setAdapter(mResultsAdapter);
        listViewFiles.setEmptyView(findViewById(R.id.viewEmpty));

        // enable action bar for home button, so we can open
        // the left list view for navigation.

        assert getSupportActionBar() != null;

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        mMainDrawerLayout = findViewById(R.id.drawerLayoutMain);
    }

    @Override
    protected void onDriveClientReady() {
        refresh();
    }

    /**
     * Invokes calls to query user's Google Drive root folder's children with
     * the currently selected query.
     */
    private void refresh() {
        mResultsAdapter.clear();
        getDriveResourceClient()
                .query(sQueries[mSelectedIndex])
                .continueWith(new Continuation<MetadataBuffer, Void>() {
                    @Override
                    public Void then(@NonNull Task<MetadataBuffer> task) throws Exception {
                        MetadataBuffer metadata = task.getResult();
                        Log.d(TAG, "Retrieved file count: " + metadata.getCount());
                        mResultsAdapter.append(metadata);
                        return null;
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error while retrieving files", e);
                        showMessage(getString(R.string.msg_errorretrieval));
                    }
                });
    }

    /**
     * Called when user interacts with the action bar. Handles home clicks to
     * open the navigation drawer for the query selection list view.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            mMainDrawerLayout.openDrawer(Gravity.START);
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * List adapter to provide data to the files list view. If there are no
     * results yet retrieved, it shows no items.
     */
    private class ResultsAdapter extends DataBufferAdapter<Metadata> {
        /**
         * Constructor.
         */
        ResultsAdapter(Context context) {
            super(context, R.layout.row_file);
        }

        /**
         * Inflates the row view for the item at the ith position, renders it
         * with the corresponding item.
         */
        @Override
        public View getView(int i, View convertView, ViewGroup arg2) {
            if (convertView == null) {
                convertView = View.inflate(getBaseContext(), R.layout.row_file, null);
            }
            TextView titleTextView = convertView.findViewById(R.id.textViewTitle);
            TextView descTextView = convertView.findViewById(R.id.textViewDescription);
            Metadata metadata = getItem(i);

            titleTextView.setText(metadata.getTitle());
            descTextView.setText(metadata.getModifiedDate().toString());
            return convertView;
        }
    }
}
