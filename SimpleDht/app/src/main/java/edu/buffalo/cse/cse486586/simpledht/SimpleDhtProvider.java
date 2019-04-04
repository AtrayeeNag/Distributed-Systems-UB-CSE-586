package edu.buffalo.cse.cse486586.simpledht;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;
import java.util.*;

public class SimpleDhtProvider extends ContentProvider {


    static final String TAG = SimpleDhtProvider.class.getSimpleName();

    static final int SERVER_PORT = 10000;
    static final Integer LEADER_NODE_PORT = 11108;
    static final Integer LEADER_NODE_ID = 5554;

    String myEmulatorId = null;
    String myPort = null;
    String successorId = null;
    String predecessorId = null;
    TreeSet<NodeMessageModel> nodeSet = new TreeSet<NodeMessageModel>();


    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO Auto-generated method stub

        String dataKey = (String) values.get("key");
        String dataValue = (String) values.get("value");

        DataMessageModel dataMessageModel = new DataMessageModel(dataKey, dataValue, DhtOperationTypeConstant.DATA_INSERT);
        new ClientTaskData().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, dataMessageModel);


//        FileOutputStream outputStream;
//
//        try {
//            outputStream = getContext().openFileOutput(filename, Context.MODE_PRIVATE);
//            outputStream.write(contentVal.getBytes());
//            outputStream.close();
//        } catch (Exception e) {
//            Log.e(TAG, "File write failed");
//        }

        Log.v("insert", values.toString());
        return uri;
    }

    @Override
    public boolean onCreate() {

        TelephonyManager tel = (TelephonyManager) this.getContext().getSystemService(Context.TELEPHONY_SERVICE);
        myEmulatorId = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPort = String.valueOf(Integer.parseInt(myEmulatorId) * 2);


        try {

            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);

        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
        }


        // Send request for join
        NodeMessageModel nodeMessageModel = new NodeMessageModel(myEmulatorId, myEmulatorId, myEmulatorId, Boolean.FALSE, DhtOperationTypeConstant.NODE_JOIN);
        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, nodeMessageModel);


        return false;

    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        // TODO Auto-generated method stub

        String[] mColumns = {"key", "value"};
        MatrixCursor mCursor = new MatrixCursor(mColumns);
        FileInputStream inputStream;

        try {
            inputStream = getContext().openFileInput(selection);
            BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream));
            String mValues = rd.readLine();

            mCursor.addRow(new String[]{selection, mValues});

        } catch (FileNotFoundException e) {
            Log.e(TAG, "File not found");
        } catch (IOException e) {
            Log.e(TAG, "IO Exception");
        }

        Log.v("query", selection);
        return mCursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
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


                    if (line != null) {

                        String strReceived = line.trim();
                        Log.d(TAG, "recieved::" + strReceived);
                        String[] strReceivedArr = strReceived.split("~");


                        if (strReceivedArr[0].equalsIgnoreCase("Node")) {

                            NodeMessageModel nodeMessageModel = NodeMessageModel.createNodeModel(strReceivedArr);

                            if (nodeMessageModel.getNodeOperation().equalsIgnoreCase(DhtOperationTypeConstant.NODE_JOIN)) {

                                Log.i(TAG, "Join Request" + "~" + nodeMessageModel.toString());
                                joinNodeToRing(nodeMessageModel, nodeSet);

                            } else {

                                Log.i(TAG, "Update Request" + "~" + nodeMessageModel.toString());
                                successorId = nodeMessageModel.getSuccessorId();
                                predecessorId = nodeMessageModel.getPredecessorId();

                            }


                        } else {
                            // TODO for DataModel
                        }

//                        ContentValues keyValueToInsert = new ContentValues();
//                        keyValueToInsert.put("key", String.valueOf(messageSequence));
//                        keyValueToInsert.put("value", line);
//
//                        Uri uri = Uri.parse("content://edu.buffalo.cse.cse486586.groupmessenger1.provider");
//                        getContentResolver().insert(uri, keyValueToInsert);
//
//                        publishProgress(line);
//                        messageSequence++;
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

    }


    private class ClientTask extends AsyncTask<NodeMessageModel, Void, Void> {

        @Override
        protected Void doInBackground(NodeMessageModel... nodes) {
            try {

                NodeMessageModel nodeMessageModel = nodes[0];

                Socket socket = null;

                if(nodeMessageModel.getNodeOperation().equalsIgnoreCase(DhtOperationTypeConstant.NODE_JOIN))

                    socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), LEADER_NODE_PORT);

                else
                    socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(nodeMessageModel.getNodeId()) * 2);


                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println(nodeMessageModel.createNodeStream());


//                DataInputStream ds = new DataInputStream(socket.getInputStream());
//                String ack = ds.readUTF();
//
//
//                if (ack.equals("Acknowledge")) {
//                    Log.e(TAG, "Ack received");
//                }


                socket.close();

            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException");
            }

            return null;
        }
    }


    private class ClientTaskData extends AsyncTask<DataMessageModel, Void, Void> {

        @Override
        protected Void doInBackground(DataMessageModel... datas) {
            try {

                DataMessageModel dataMessageModel = datas[0];

                Socket socket = socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), LEADER_NODE_PORT);

                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println(dataMessageModel.createDataStream());


//                DataInputStream ds = new DataInputStream(socket.getInputStream());
//                String ack = ds.readUTF();
//
//
//                if (ack.equals("Acknowledge")) {
//                    Log.e(TAG, "Ack received");
//                }


                socket.close();

            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException");
            }

            return null;
        }

    }


    // Reference : https://stackoverflow.com/questions/17806543/find-and-return-an-element-from-a-treeset-in-java

    private NodeMessageModel searchNode(TreeSet<NodeMessageModel> nodeSet, NodeMessageModel key) {

        NodeMessageModel ceil  = nodeSet.ceiling(key); // least elt >= key
        NodeMessageModel floor = nodeSet.floor(key);   // highest elt <= key
        return ceil == floor? ceil : null;

    }


    private void joinNodeToRing(NodeMessageModel nodeMessageModel, TreeSet<NodeMessageModel> nodeSet){

        nodeSet.add(nodeMessageModel);

        if (!nodeSet.isEmpty() && nodeSet.size() > 1) {

            NodeMessageModel curr = null;
            NodeMessageModel prev = null;
            NodeMessageModel next = null;

            if(searchNode(nodeSet, nodeMessageModel) != null){

                curr = nodeMessageModel;

                if(curr.equals(nodeSet.first())){

                    prev = nodeSet.last();
                    next = nodeSet.higher(curr);

                } else if(curr.equals(nodeSet.last())){

                    prev = nodeSet.lower(curr);
                    next = nodeSet.first();

                } else{

                    prev = nodeSet.lower(curr);
                    next = nodeSet.higher(curr);

                }

                prev.setSuccessorId(curr.getNodeId());
                curr.setPredecessorId(prev.getNodeId());
                curr.setSuccessorId(next.getNodeId());
                next.setPredecessorId(curr.getNodeId());

            }

            prev.setNodeOperation(DhtOperationTypeConstant.NODE_UPDATE);
            curr.setNodeOperation(DhtOperationTypeConstant.NODE_UPDATE);
            next.setNodeOperation(DhtOperationTypeConstant.NODE_UPDATE);

            nodeSet.add(prev);
            nodeSet.add(curr);
            nodeSet.add(next);

            if(nodeSet.size() == 1){

                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, curr);


            } else if(nodeSet.size() == 2){

                if(curr.equals(nodeSet.first())){

                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, curr);
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, next);

                } else{

                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, prev);
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, curr);

                }
            } else{

                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, prev);
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, curr);
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, next);

            }

        }


    }





}