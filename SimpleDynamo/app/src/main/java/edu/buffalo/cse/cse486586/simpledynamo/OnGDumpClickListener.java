package edu.buffalo.cse.cse486586.simpledynamo;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.View;
import android.widget.TextView;

public class OnGDumpClickListener implements View.OnClickListener {




    private final TextView mTextView;
    private final ContentResolver mContentResolver;
    private final Uri mUri;

    private static final String KEY_FIELD = "key";
    private static final String VALUE_FIELD = "value";

    public OnGDumpClickListener(TextView _tv, ContentResolver _cr) {
        mTextView = _tv;
        mContentResolver = _cr;
        mUri = buildUri("content", "edu.buffalo.cse.cse486586.simpledynamo.provider");

    }

    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

    @Override
    public void onClick(View v) {
        new OnGDumpClickListener.Task().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private class Task extends AsyncTask<Void, String, Void> {

        @Override
        protected Void doInBackground(Void... params) {

//            mContentResolver.delete(mUri, "key14",null);

            Cursor resultCursor = mContentResolver.query(mUri, null,
                    "*", null, null);

            int count =1;

            if (resultCursor.moveToFirst()) {

                while (!resultCursor.isAfterLast()) {

                    String data = count++ + resultCursor.getString(resultCursor.getColumnIndex(KEY_FIELD)) +
                            "~" + resultCursor.getString(resultCursor.getColumnIndex(VALUE_FIELD));


                    publishProgress(data);

                    resultCursor.moveToNext();
                }
            }
            resultCursor.close();

            return null;
        }

        protected void onProgressUpdate(String...strings) {
            mTextView.append(strings[0]+ "\n");

            return;
        }



    }
}
