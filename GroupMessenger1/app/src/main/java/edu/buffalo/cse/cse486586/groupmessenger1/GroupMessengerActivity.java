package edu.buffalo.cse.cse486586.groupmessenger1;

import android.app.Activity;
import android.content.Context;
import android.content.ContentValues;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import android.net.Uri;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 */
public class GroupMessengerActivity extends Activity {

    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String[] portArr = {"11108", "11112", "11116", "11120", "11124"};
    static final int SERVER_PORT = 10000;
    static int messageSequence = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        /* Creating server socket and Async task to listen on the server port. */
        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
        }

        final EditText editText = (EditText) findViewById(R.id.editText1);

        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        /* OnClickListener for the "Send" button to display the message and create a client thread
         * which will send the string to the other avds over the network.
         *
         * Referenced from the SimpleMessenger code.
         */
        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "On Send click");
                String msg = editText.getText().toString() + "\n";
                editText.setText(""); // This is one way to reset the input box.
                TextView localTextView = (TextView) findViewById(R.id.textView1);
                localTextView.append("\t" + msg); // This is one way to display a string.
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }


    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];

            /*
             * References: https://docs.oracle.com/javase/tutorial/networking/sockets/readingWriting.html
             * https://docs.oracle.com/javase/tutorial/networking/sockets/clientServer.html
             */
            try {
                while(true) {
                    Log.e(TAG, "Inside Server Task");
                    /* Listens and accepts to a connection to be established with the socket. */
                    Socket socket = serverSocket.accept();

                    /* Gets the socket's input stream and opens a BufferedReader on it and reads it. */
                    BufferedReader rd = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String line = rd.readLine();

                    /* Stores each message string in a key value pair in the content provider and publishes the input stream to the UI thread. */
                    if(line != null) {

                        /* Code referenced from PA2 PartA specification doc. */
                        ContentValues keyValueToInsert = new ContentValues();
                        keyValueToInsert.put("key", String.valueOf(messageSequence));
                        keyValueToInsert.put("value", line);

                        /* Inserts a row in the content provider.
                         * Code reference: https://stackoverflow.com/questions/40440733/inserting-a-row-with-android-content-provider */
                        Uri uri = Uri.parse("content://edu.buffalo.cse.cse486586.groupmessenger1.provider");
                        getContentResolver().insert(uri, keyValueToInsert);

                        publishProgress(line);
                        messageSequence++;
                    }

                    /* Closing the reader and socket. */
                    socket.close();
                    rd.close();
                }

            } catch (UnknownHostException e) {
                Log.e(TAG, "ServerTask UnknownHostException");
            } catch(IOException e){
                Log.e(TAG, "ServerTask socket IOException");
            }

            return null;
        }

        protected void onProgressUpdate(String...strings) {
            /*
             * The following code displays what is received in doInBackground().
             */
            String strReceived = strings[0].trim();
            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            remoteTextView.append(strReceived + "\t\n");
        }
    }

    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            try {
                Log.e(TAG, "Inside Client task");

                for(String port : portArr) {
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(port));
                    String msgToSend = msgs[0];

                    /* Gets the socket's output stream and opens a PrintWriter on it. */
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    out.println(msgToSend);

                    /* Closing the output stream and socket. */
                    socket.close();
                    out.close();
                }

            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }

            return null;
        }
    }
}
