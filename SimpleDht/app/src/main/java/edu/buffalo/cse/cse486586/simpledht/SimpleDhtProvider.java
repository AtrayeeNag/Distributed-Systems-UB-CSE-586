package edu.buffalo.cse.cse486586.simpledht;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
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
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.util.Log;
import java.util.*;

public class SimpleDhtProvider extends ContentProvider {


    static final String TAG = SimpleDhtProvider.class.getSimpleName();

    static final int SERVER_PORT = 10000;
    static final Integer LEADER_NODE_PORT = 11108;

    String myEmulatorId = null;
    String myPort = null;
    String successorId = null;
    String predecessorId = null;
    TreeSet<NodeMessageModel> nodeSet = new TreeSet<NodeMessageModel>();


    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub

        getContext().deleteFile(selection);

        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {

        String dataKey = (String) values.get("key");
        String dataValue = (String) values.get("value");

        if(dataKey != null && dataValue != null) {
            DataMessageModel dataMessageModel = new DataMessageModel(dataKey, dataValue, DhtOperationTypeConstant.DATA_INSERT, null, myEmulatorId, null);

            String targetNode = lookUpDataPosition(dataMessageModel);

            if (targetNode.equals(myEmulatorId)) {

                insertDatatoLocal(dataMessageModel);

            } else {

                dataMessageModel.setTargetNode(targetNode);
                new ClientTaskDataList().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, dataMessageModel.createDataStream());
            }
        }

        Log.v("insert", values.toString());
        return uri;
    }


    @Override
    public boolean onCreate() {

        TelephonyManager tel = (TelephonyManager) this.getContext().getSystemService(Context.TELEPHONY_SERVICE);
        myEmulatorId = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPort = String.valueOf(Integer.parseInt(myEmulatorId) * 2);
        predecessorId = myEmulatorId;
        successorId = myEmulatorId;


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

        String[] mColumns = {"key", "value"};

        FileInputStream inputStream;

        if(selection.equals("*")){

            List<DataMessageModel> dataList = new ArrayList<DataMessageModel>();

            if(myEmulatorId.equals(successorId) && myEmulatorId.equals(predecessorId)) {

                Log.i(TAG, "********One node in ring*********");

                MatrixCursor mCursor = new MatrixCursor(mColumns);

                for (String file : getContext().fileList()) {
                    try {
                        inputStream = getContext().openFileInput(file);

                        BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream));
                        String mValues = rd.readLine();

                        Log.e(TAG, "file name::" + file);

                        mCursor.newRow()
                                .add("key", file)
                                .add("value", mValues);

                    } catch (FileNotFoundException e) {
                        Log.e(TAG, "File not found");
                    } catch (IOException e) {
                        Log.e(TAG, "IO Exception");
                    }

                }

                return mCursor;

            } else {

                MatrixCursor mCursor = new MatrixCursor(mColumns);

                Log.i(TAG, "************More than one node in ring************");
                dataList.addAll(queryAllDataFromRing(myEmulatorId));

                for(DataMessageModel data : dataList) {
                    mCursor.newRow()
                            .add("key", data.getKey())
                            .add("value", data.getMessage());
                }

                return mCursor;
            }


        } else if(selection.equals("@")){

            MatrixCursor mCursor = new MatrixCursor(mColumns);

            for(String file : getContext().fileList()){
                try {
                    inputStream = getContext().openFileInput(file);
                    BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream));
                    String mValues = rd.readLine();

                    Log.e(TAG,"file name::" + file);

                    mCursor.newRow()
                            .add("key", file)
                            .add("value", mValues);


                }catch (FileNotFoundException e) {
                    Log.e(TAG, "File not found");
                }catch (IOException e){
                    Log.e(TAG, "IO Exception");
                }

            }

            return mCursor;

        } else{

            DataMessageModel dataMessageModel = new DataMessageModel(selection, null, DhtOperationTypeConstant.DATA_QUERY, null, myEmulatorId, myEmulatorId);

            String targetNode = lookUpDataPosition(dataMessageModel);

            if (targetNode.equals(myEmulatorId)) {

                MatrixCursor mCursor = new MatrixCursor(mColumns);

                try {
                    inputStream = getContext().openFileInput(selection);
                    BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream));
                    String mValues = rd.readLine();

                    Log.e(TAG,"file name::" + selection);

                    mCursor.newRow()
                            .add("key", selection)
                            .add("value", mValues);

                }catch (FileNotFoundException e) {
                    Log.e(TAG, "File not found");
                }catch (IOException e){
                    Log.e(TAG, "IO Exception");
                }

                return mCursor;

            } else{

                MatrixCursor mCursor = new MatrixCursor(mColumns);

                dataMessageModel.setTargetNode(targetNode);
                DataMessageModel resultModel = queryDataFromRing(dataMessageModel);

                mCursor.newRow()
                        .add("key", resultModel.getKey())
                        .add("value", resultModel.getMessage());

                return mCursor;
            }

        }


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

                        } else if(strReceivedArr[0].equalsIgnoreCase("Data")){

                            if(strReceivedArr.length >1) {

                                DataMessageModel dataMessageModel = new DataMessageModel();
                                dataMessageModel.createDataModel(strReceivedArr);

                                if (DhtOperationTypeConstant.DATA_INSERT.equalsIgnoreCase(dataMessageModel.getDataOperationType())) {

                                    Log.i(TAG, "Insert Request" + "~" + dataMessageModel.createDataStream());

                                    insertDataToRing(dataMessageModel);

                                } else if (DhtOperationTypeConstant.DATA_QUERY.equalsIgnoreCase(dataMessageModel.getDataOperationType())) {

                                    DataMessageModel resultModel = queryDataFromRing(dataMessageModel);

                                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                                    out.writeUTF(resultModel.createDataStream());
                                    out.flush();
                                    out.close();

                                } else if(DhtOperationTypeConstant.DATA_QUERY_GLOBAL.equalsIgnoreCase(dataMessageModel.getDataOperationType())) {

                                    // query all data

                                    Log.d(TAG,"get msg::"+line);
                                    Log.d(TAG,"my id::"+myEmulatorId);


                                    if(dataMessageModel.getOriginNode().equalsIgnoreCase(myEmulatorId)){

                                        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                                        out.writeUTF(" ");
                                        out.flush();
                                        out.close();

                                    } else{

                                        List<DataMessageModel> dataList = queryAllDataFromRing(dataMessageModel.getOriginNode());

                                        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                                        out.writeUTF(convertDataListToStream(dataList));
                                        out.flush();
                                        out.close();

                                    }

                                }

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

                socket.close();

            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException");
            }

            return null;
        }
    }


    private class ClientTaskDataList extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... line) {

            String result = "";
            try {

                String strReceived = line[0].trim();
                Log.d(TAG, "recieved::" + strReceived);
                String[] strReceivedArr = strReceived.split("~");

                DataMessageModel dataMessageModel = new DataMessageModel();
                dataMessageModel.createDataModel(strReceivedArr);

                if(dataMessageModel.getDataOperationType().equalsIgnoreCase(DhtOperationTypeConstant.DATA_INSERT)){

                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(dataMessageModel.getTargetNode())*2);

                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    out.println(dataMessageModel.createDataStream());

                    socket.close();

                } else{
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(successorId)*2);

                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    out.println(line[0]);

                    DataInputStream ds = new DataInputStream(socket.getInputStream());
                    result = ds.readUTF();

                    ds.close();
                    socket.close();

                }

            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException");
            }

            return result;
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

            if(searchNode(nodeSet, nodeMessageModel) != null ){

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


    private void insertDataToRing(DataMessageModel dataMessageModel) {

        String targetNode = lookUpDataPosition(dataMessageModel);

        if(targetNode.equals(myEmulatorId)) {

            insertDatatoLocal(dataMessageModel);

        }else {

            dataMessageModel.setTargetNode(targetNode);
            new ClientTaskDataList().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, dataMessageModel.createDataStream());
        }

    }


    private void insertDatatoLocal(DataMessageModel dataMessageModel){

        FileOutputStream outputStream;

        try {

            outputStream = getContext().openFileOutput(dataMessageModel.getKey(), Context.MODE_PRIVATE);
            outputStream.write(dataMessageModel.getMessage().getBytes());
            outputStream.close();

        } catch (Exception e) {
            Log.e(TAG, "File write failed");
        }

    }


    private String lookUpDataPosition(DataMessageModel dataMessageModel){

        try {

            Boolean isfirstNode = Boolean.FALSE;

            String hashPredId = SimpleDhtUtil.genHash(predecessorId);
            String hashMyEmulatId = SimpleDhtUtil.genHash(myEmulatorId);
            String hashDataMessageKey = SimpleDhtUtil.genHash(dataMessageModel.getKey());

            if(hashPredId.compareTo(hashMyEmulatId) > 0)
                isfirstNode = Boolean.TRUE;


            if(isfirstNode){
                if(hashDataMessageKey.compareTo(hashMyEmulatId) > 0
                        && hashDataMessageKey.compareTo(hashPredId)<0)

                    return successorId;

                else

                    return myEmulatorId;

            }else{
                if(hashDataMessageKey.compareTo(hashPredId) > 0
                        && hashDataMessageKey.compareTo(hashMyEmulatId) <= 0)

                    return myEmulatorId;

                else

                    return successorId;

            }

        } catch(Exception e){
            Log.e(TAG, "Error generating Hash");

        }
        return null;
    }


    private String convertDataListToStream(List<DataMessageModel> dataList){

        if(!dataList.isEmpty()){
            StringBuilder strBuild = new StringBuilder();

            for(DataMessageModel data : dataList){

                strBuild.append(data.createDataStream());
                strBuild.append("#");

            }

            return strBuild.toString().replaceAll("#$", "");

        }

        return " ";
    }


    private List<DataMessageModel> convertStreamToDataList(String stream){

        if(stream != null){

            List<DataMessageModel> dataList = new ArrayList<DataMessageModel>();
            String strReceived = stream.trim();

            Log.d(TAG, "recieved::" + strReceived);
            String[] strReceivedArr = strReceived.split("#");

            for(String dataString : strReceivedArr){

                if(dataString.trim().length() >0) {
                    String[] dataStream = dataString.trim().split("~");
                    if(dataStream.length > 1) {
                        DataMessageModel dataMessageModel = new DataMessageModel();
                        dataMessageModel.createDataModel(dataStream);
                        dataList.add(dataMessageModel);
                    }
                }

            }

            return dataList;
        }

        return null;

    }


    private List<DataMessageModel> queryAllDataFromRing(String originId){

        FileInputStream inputStream;
        List<DataMessageModel> dataList = new ArrayList<DataMessageModel>();


        for (String file : getContext().fileList()) {
            try {
                inputStream = getContext().openFileInput(file);

                BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream));
                String mValues = rd.readLine();

                Log.e(TAG, "file name::" + file);

                if(file != null && mValues != null) {
                    DataMessageModel dataMessageModel = new DataMessageModel(file, mValues, DhtOperationTypeConstant.DATA_QUERY, null, null, originId);
                    dataList.add(dataMessageModel);
                }

            } catch (FileNotFoundException e) {
                Log.e(TAG, "File not found");
            } catch (IOException e) {
                Log.e(TAG, "IO Exception");
            }

        }

        DataMessageModel dataMessageModel = new DataMessageModel(null, null, DhtOperationTypeConstant.DATA_QUERY_GLOBAL, null,null, originId);

        try {
            String resultData = new ClientTaskDataList().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, dataMessageModel.createDataStream()).get();

            dataList.addAll(convertStreamToDataList(resultData));
        } catch(Exception e){
            Log.e(TAG, "Interrupted Exception");
        }

        Log.e(TAG,myEmulatorId+":::"+dataList.size());

        return dataList;

    }


    private DataMessageModel queryDataFromRing(DataMessageModel dataMessageModel){

        String targetNode = lookUpDataPosition(dataMessageModel);
        String[] mColumns = {"key", "value"};
        DataMessageModel resultModel = new DataMessageModel();

        FileInputStream inputStream;

        if (targetNode.equals(myEmulatorId)) {
            try {
                inputStream = getContext().openFileInput(dataMessageModel.getKey());
                BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream));
                String mValues = rd.readLine();

                Log.e(TAG,"file name::" + dataMessageModel.getKey());
                dataMessageModel.setMessage(mValues);
                resultModel = dataMessageModel;


            }catch (FileNotFoundException e) {
                Log.e(TAG, "File not found");
            }catch (IOException e){
                Log.e(TAG, "IO Exception");
            }
        } else {
            try {
                String resultData = new ClientTaskDataList().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, dataMessageModel.createDataStream()).get();

                resultModel.createDataModel(resultData.trim().split("~"));

            } catch(Exception e){
                Log.e(TAG, "Interrupted Exception");
            }


        }
        return resultModel;
    }

}