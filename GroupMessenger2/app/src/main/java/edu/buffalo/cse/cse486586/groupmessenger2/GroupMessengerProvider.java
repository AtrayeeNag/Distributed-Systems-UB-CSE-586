package edu.buffalo.cse.cse486586.groupmessenger2;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;

/**
 * GroupMessengerProvider is a key-value table. Once again, please note that we do not implement
 * full support for SQL as a usual ContentProvider does. We re-purpose ContentProvider's interface
 * to use it as a key-value table.
 * 
 * Please read:
 * 
 * http://developer.android.com/guide/topics/providers/content-providers.html
 * http://developer.android.com/reference/android/content/ContentProvider.html
 * 
 * before you start to get yourself familiarized with ContentProvider.
 * 
 * There are two methods you need to implement---insert() and query(). Others are optional and
 * will not be tested.
 *
 *
 */
public class GroupMessengerProvider extends ContentProvider {

    static final String TAG = GroupMessengerProvider.class.getSimpleName();

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // You do not need to implement this.
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {

        /* Used the internal storage to implement the structure in which the message will be stored in the
         * provider. The contentValue key is the filename and the value is written into the file.
         *
         * Code reference: onProgressUpdate of ServerTask of SimpleMessenger.
         */
        String filename = (String) values.get("key");
        String contentVal = (String) values.get("value");
        FileOutputStream outputStream;

        try {
            outputStream = getContext().openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(contentVal.getBytes());
            outputStream.close();
        } catch (Exception e) {
            Log.e(TAG, "File write failed");
        }

        Log.v("insert", values.toString());
        return uri;
    }

    @Override
    public boolean onCreate() {
        // If you need to perform any one-time initialization task, please do it here.
        return false;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {

        /* Used MatrixCursor to implement read functionality from the file using the key=filename
         * as the query parameter to retrieve content/values.
         *
         * Reference: 1. https://stackoverflow.com/questions/9917935/adding-rows-into-cursor-manually
         *            2. https://developer.android.com/reference/android/database/MatrixCursor
         */
        String[] mColumns = {"key", "value"};
        MatrixCursor mCursor = new MatrixCursor(mColumns);
        FileInputStream inputStream;

        try {
            inputStream = getContext().openFileInput(selection);
            BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream));
            String mValues = rd.readLine();

            mCursor.addRow(new String[] {selection, mValues});

        }catch (FileNotFoundException e) {
            Log.e(TAG, "File not found");
        }catch (IOException e){
            Log.e(TAG, "IO Exception");
        }

        Log.v("query", selection);
        return mCursor;
    }
}
