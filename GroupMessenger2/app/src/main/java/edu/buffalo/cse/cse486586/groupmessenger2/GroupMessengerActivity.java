package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.Context;
import android.content.ContentValues;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import android.net.Uri;
import java.net.SocketTimeoutException;
import java.util.Map;


public class GroupMessengerActivity extends Activity {

    static final String TAG = GroupMessengerActivity.class.getSimpleName();

//    static Integer[] portArr = {11108, 11112, 11116, 11120, 11124};


    List<Integer> PORT_LIST = new ArrayList<Integer>() {{
        add(11108);
        add(11112);
        add(11116);
        add(11120);
        add(11124);
    }};


    static final int SERVER_PORT = 10000;

    /* Counter for proposed messages by each avd. */
    int messageSequence = 0;

    /* Sequence for storing messages in content provider. */
    protected int saveSequence = 0;

    /* Counter for creating a unique identifier for messages. */
    int localMessageSequence = 0;

    protected int failedPort;
    String myPort = null;

    /* Priority queue to store and deliver incoming messages in each avd in FIFO order. */
    protected PriorityQueue<MessageOrderModel> deliveryQueue = new PriorityQueue<MessageOrderModel>(100, new MessageModelComparator());

    /* Map to store the message identifier as key and the proposal suggested, incoming port as values.  */
    Map<Integer, HashMap<Integer, Integer>> msgProposalMap = new HashMap<Integer, HashMap<Integer, Integer>>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        // PA2B Start

        /* Job to deliver the messages which has received the agreedProposal and are marked ready from the priority queue */
        // Reference : https://stackoverflow.com/questions/11123621/running-code-in-main-thread-from-another-thread

        final Handler mainHandler = new Handler(Looper.getMainLooper());

        mainHandler.post(new DeliverMessageJob(mainHandler));

        // PA2B End

        try {

            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);

            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);

        } catch (IOException e) {

            Log.e(TAG, "Can't create a ServerSocket");

        }

        final EditText editText = (EditText) findViewById(R.id.editText1);

        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String msg = editText.getText().toString();
                editText.setText("");

                /* Create Message Model when sending the message for the first time on the send button event. */

                MessageOrderModel newMessage = new MessageOrderModel(Integer.MIN_VALUE, Integer.parseInt(myPort), msg, Integer.MIN_VALUE, false,
                        false, Integer.parseInt(myPort), false, localMessageSequence, false);

                localMessageSequence++;

                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, newMessage);
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

            try {

                while (true) {

                    Socket socket = serverSocket.accept();

                    BufferedReader rd = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String line = rd.readLine();
                    MessageOrderModel msg = getMessageModelFromStream(line);

                    /* Check if message sent to avd is a Heartbeat message or a MessageOrderModel. Heartbeat message is sent to
                        check if the port is still alive */

                    if (msg.getDummy()) {

                        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                        out.writeUTF("Acknowledge");
                        out.flush();

                    } else {

                        /* Check if the message is received by the avd for the first time and send a proposal for the message
                            to the incoming port . Proposal sequence is maintained by the messageSequence variable counter. */

                        if (!Boolean.valueOf(msg.getProposal()) && !Boolean.valueOf(msg.getAgreement())) {

                            msg.setSequenceNo(messageSequence);
                            msg.setProposal(true);
                            msg.setAgreement(false);
                            msg.setProposalPort(Integer.parseInt(myPort));

                            deliveryQueue.add(msg);

                            /* Reference : https://www.tutorialspoint.com/java/io/java_io_dataoutputstream.htm */
                            // Could not make this work using BufferedReader(new InputStreamReader()), changed to DataOutputStream/DataInputStream.

                            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                            out.writeUTF(msg.createMessageStream());
                            out.flush();

                            messageSequence++;

                        }
                        /* Perform task if the message is of type agreement and send the client an acknowledgement that
                           the node is still alive. */
                        else {

                            if (line != null) {

                                publishProgress(line);
                                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                                out.writeUTF("Acknowledge");
                                out.flush();

                            }
                        }
                    }

                    socket.close();

                }

            } catch (UnknownHostException e) {

                Log.e(TAG, "ServerTask UnknownHostException");

            } catch (IOException e) {

                Log.e(TAG, "ServerTask socket IOException");

            }

            return null;
        }

        protected void onProgressUpdate(String... strings) {

            String strReceived = strings[0];
            MessageOrderModel msg = getMessageModelFromStream(strReceived);

            /* If the message is an agreed priority, sent by the initial avd to all the avds, the message sequence number for each of the avds
               are changed to that of the final priority and are marked as delivered in the priority queue. */

            if (msg.getAgreedProposal() > Integer.MIN_VALUE && Boolean.valueOf(msg.getAgreement())) {

                /* Message is removed by checking equality on the overriden equals method of MessageOrderModel
                   on the basis of localsequence, message content and initial port. */

                deliveryQueue.remove(msg);


                MessageOrderModel newMessage = new MessageOrderModel(msg.getAgreedProposal(), msg.getProposalPort(), msg.getMessage(),
                        msg.getAgreedProposal(), false, false, msg.getMyPort(), true, msg.getLocalMessageSequence(), false);


                deliveryQueue.add(newMessage);

                /* To handle the proposed sequence number so that it is larger than all observed priorities
                   and larger than the previously proposed (by self) priority. */

                if (messageSequence <= newMessage.getSequenceNo())

                    messageSequence = newMessage.getSequenceNo() + 1;



            }

        }
    }

    private class ClientTask extends AsyncTask<MessageOrderModel, Void, Void> {

        @Override
        protected Void doInBackground(MessageOrderModel... msgModels) {
//            String failedPort = null;

            MessageOrderModel msgModel = msgModels[0];

            List<Integer> newPortList = new ArrayList<Integer>(PORT_LIST);

            if (msgModel.getDummy()) {

                PORT_LIST.clear();

                PORT_LIST.add(msgModel.getMyPort());

            }

            for (Integer port : PORT_LIST) {

                try {

                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            port);

                    socket.setSoTimeout(1000);

                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                    out.println(msgModel.createMessageStream());

                    /* Implementing full duplex TCP send and receive acknowledgement to check if port is still alive. */
                    DataInputStream ds = new DataInputStream(socket.getInputStream());

                    String ack = ds.readUTF();

                    if (ack.equals("Acknowledge")) {

                        Log.e(TAG, "Ack received");

                    } else {

                        MessageOrderModel msg = getMessageModelFromStream(ack);

                        /* If the message is of proposal type received from each of the avds for a particular message, then the proposalMap for
                            that particular message seqeunce is added to the map along with the port of the avd who sent the proposal. */

                        if (Boolean.valueOf(msg.getProposal()) && msg.getAgreedProposal() == Integer.MIN_VALUE) {

                            if (msgProposalMap.containsKey(msg.getLocalMessageSequence())) {

                                HashMap<Integer, Integer> proposalMap = msgProposalMap.get(msg.getLocalMessageSequence());

                                proposalMap.put(msg.getProposalPort(), msg.getSequenceNo());

                                msgProposalMap.put(msg.getLocalMessageSequence(), proposalMap);

                            } else {

                                HashMap<Integer, Integer> proposalMap = new HashMap<Integer, Integer>();

                                proposalMap.put(msg.getProposalPort(), msg.getSequenceNo());

                                msgProposalMap.put(msg.getLocalMessageSequence(), proposalMap);

                            }

                        }
                    }


                    ds.close();

                    socket.close();

                }

                /* When an avd is failed, an exception is caught from the receive acknowledgement and that particular port is removed from the portlist array. */

                catch (SocketTimeoutException e) {

                    Log.e(TAG, "ClientTask SocketTimeoutException");

                    failedPort = port;

                    newPortList.remove(port);

                } catch (UnknownHostException e) {

                    Log.e(TAG, "ClientTask UnknownHostException");

                }
                // Ideally very rarely getting this exception.
                catch (EOFException e) {

                    Log.e(TAG, "ClientTask EOFException");

                    failedPort = port;
                    newPortList.remove(port);

                } catch (IOException e) {

                    Log.e(TAG, "ClientTask IOException");

                    failedPort = port;
                    newPortList.remove(port);

                }

            }

            PORT_LIST = new ArrayList<Integer>(newPortList);

            if (!msgModel.getDummy() && msgProposalMap.containsKey(msgModel.getLocalMessageSequence())) {

                HashMap<Integer, Integer> proposedMap = msgProposalMap.get(msgModel.getLocalMessageSequence());

                sendAgreement(proposedMap, msgModel);
            }

            return null;
        }
    }


    /* The avd checks of the highest sequence:port combination in the proposal list for the message and
        suggests the same as the agreedProposal and sends the same to all the avds. */
    private void sendAgreement(Map<Integer, Integer> proposalMap, MessageOrderModel newMessage) {

        if (proposalMap.size() >= PORT_LIST.size()) {


            TreeMap<Integer, Integer> proposalTMap = new TreeMap<Integer, Integer>(proposalMap);


            int maxSequence = 0;
            int maxPort = 0;

            for (Map.Entry<Integer, Integer> entry : proposalTMap.entrySet()) {

                if (entry.getValue() >= maxSequence) {

                    maxSequence = entry.getValue();
                    if (entry.getKey() >= maxPort) {

                        maxPort = entry.getKey();
                    }
                }
            }


            MessageOrderModel messageOrderModel = new MessageOrderModel(newMessage.getSequenceNo(), maxPort, newMessage.getMessage(), maxSequence,
                    false, true, newMessage.getMyPort(), newMessage.getReadyToDeliver(), newMessage.getLocalMessageSequence(), false);

            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, messageOrderModel);

            /* Message which has been agreed upon with a final proposal has been removed from the proposalMap. */

            msgProposalMap.remove(messageOrderModel.getLocalMessageSequence());

        }
    }


    /* Conversion from Output/Input stream to  MessageOrderModel to avoid redundancy.*/

    private MessageOrderModel getMessageModelFromStream(String stream) {

        String strReceived = stream.trim();

        String[] msgComp = strReceived.split("~");

        int sequenceNo = Integer.parseInt(msgComp[0]);

        int proposalPort = Integer.parseInt(msgComp[1]);

        String message = msgComp[2];

        int agreedProposal = Integer.parseInt(msgComp[3]);

        Boolean isProposal = Boolean.parseBoolean(msgComp[4]);

        Boolean isAgreement = Boolean.parseBoolean(msgComp[5]);

        int my_Port = Integer.parseInt(msgComp[6]);

        Boolean readyToDeliver = Boolean.parseBoolean(msgComp[7]);

        int localSequenceNo = Integer.parseInt(msgComp[8]);

        Boolean isDummy = Boolean.parseBoolean(msgComp[9]);

        MessageOrderModel newMessage = new MessageOrderModel(sequenceNo, proposalPort, message, agreedProposal, isProposal, isAgreement, my_Port, readyToDeliver, localSequenceNo, isDummy);

        return newMessage;

    }


    private class DeliverMessageJob implements Runnable {

        protected Handler mainHandler;

        public DeliverMessageJob(Handler mainHandler) {
            this.mainHandler = mainHandler;
        }

        public void run() {

            if (!deliveryQueue.isEmpty()) {

                MessageOrderModel head = deliveryQueue.peek();

                /* Remove message from priority-queue when the port it originated from has failed and
                   message has not yet received an agreement. */

                if (head.getMyPort() == failedPort && !head.getReadyToDeliver()) {

                    deliveryQueue.poll();

                }

                /* Deliver the message head from priority-queue when message is ready and save the
                   message in the avd local storage in key-value format. */

                else if (head.getReadyToDeliver()) {

                    ContentValues keyValueToInsert = new ContentValues();

                    keyValueToInsert.put("key", String.valueOf(saveSequence));

                    keyValueToInsert.put("value", head.getMessage());


                    /* Inserts a row in the content provider.
                     * Code reference: https://stackoverflow.com/questions/40440733/inserting-a-row-with-android-content-provider */

                    Uri uri = Uri.parse("content://edu.buffalo.cse.cse486586.groupmessenger2.provider");

                    getContentResolver().insert(uri, keyValueToInsert);

                    TextView remoteTextView = (TextView) findViewById(R.id.textView1);

                    remoteTextView.append(saveSequence + "~" + head.createMessageStream() + "\t\n");

                    saveSequence++;

                    deliveryQueue.poll();

                }

                /* Check if the port is still alive by sending heartbeat, when message is not yet ready to deliver */
                else {

                    head.setDummy(true);

                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, head);

                }
            }

            mainHandler.postDelayed(this, 700);
        }
    }

}
