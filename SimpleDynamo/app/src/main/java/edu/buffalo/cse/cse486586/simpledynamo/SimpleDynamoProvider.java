package edu.buffalo.cse.cse486586.simpledynamo;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.*;
import java.util.TreeSet;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.Date;
import java.net.SocketTimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SimpleDynamoProvider extends ContentProvider {


	static final String TAG = SimpleDynamoProvider.class.getSimpleName();

	static final int SERVER_PORT = 10000;
	static final String[] EMULATOR_ARR = {"5554", "5556", "5558", "5560", "5562"};

	String myEmulatorId = null;
	String myPort = null;

	TreeSet<DataMessageModel> nodeSet = new TreeSet<DataMessageModel>();

    ReadWriteLock lock = new ReentrantReadWriteLock();
    Lock writeLock = lock.writeLock();
    Lock readLock = lock.readLock();


	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {

		if(selection.equals("*")){

			for(DataMessageModel node : nodeSet){

				DataMessageModel dataMessageModel = new DataMessageModel(null, null, DhtOperationTypeConstant.DATA_DELETE_GLOBAL, null, node.getKey(), null, 0L);

                try {
                    new ClientTaskDataList().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, dataMessageModel.createDataStream());
                }
                catch(Exception e){
                    Log.e(TAG, "Interrupted Exception in delete");
                }

			}

		} else if(selection.equals("@")){

			DataMessageModel dataMessageModel = new DataMessageModel(null, null, DhtOperationTypeConstant.DATA_DELETE_LOCAL, null, myEmulatorId, null, 0L);
			deleteDataFromLocal(dataMessageModel);

		} else {

			DataMessageModel dataMessageModel = new DataMessageModel(selection, null, DhtOperationTypeConstant.DATA_DELETE_KEY, null, null, myEmulatorId, 0L);
			List<String> targetList = getTargetList(dataMessageModel);

			for (String node : targetList) {

                dataMessageModel.setTargetNode(node);
                new ClientTaskDataList().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, dataMessageModel.createDataStream());

			}
		}

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

		Date date = new Date();
		long insertTimeStamp = date.getTime();

		DataMessageModel dataMessageModel = new DataMessageModel(dataKey, dataValue, DhtOperationTypeConstant.DATA_INSERT, null, myEmulatorId, myEmulatorId, insertTimeStamp);


        List<String> targetList = getTargetList(dataMessageModel);

		for(String target : targetList) {

            dataMessageModel.setTargetNode(target);
            new ClientTaskDataList().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, dataMessageModel.createDataStream());

		}

		Log.v("insert", values.toString());
		return uri;
	}

	@Override
	public boolean onCreate() {
		TelephonyManager tel = (TelephonyManager) this.getContext().getSystemService(Context.TELEPHONY_SERVICE);
		myEmulatorId = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
		myPort = String.valueOf(Integer.parseInt(myEmulatorId) * 2);


        lock = new ReentrantReadWriteLock();
        writeLock = lock.writeLock();
        readLock = lock.readLock();

        DataMessageModel messageModelForDelete = new DataMessageModel(myEmulatorId, "from recovery", DhtOperationTypeConstant.DATA_DELETE_LOCAL, null, myEmulatorId, null, 0L);
        deleteDataFromLocal(messageModelForDelete);

		try {

			ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
			new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);

		} catch (IOException e) {
			Log.e(TAG, "Can't create a ServerSocket");
		}

		for(String emulatorId : EMULATOR_ARR){
			DataMessageModel dataMessageModel = new DataMessageModel(emulatorId);
			nodeSet.add(dataMessageModel);
		}

        final Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(new SyncMessagesJob(mainHandler));

        return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		String[] mColumns = {"key", "value"};

		List<DataMessageModel> dataList = new ArrayList<DataMessageModel>();
		List<DataMessageModel> filteredList = new ArrayList<DataMessageModel>();
		MatrixCursor mCursor = new MatrixCursor(mColumns);


		if(selection.equals("*")){
			for(DataMessageModel node : nodeSet){

				DataMessageModel dataMessageModel = new DataMessageModel(null, null, DhtOperationTypeConstant.DATA_QUERY_GLOBAL, null, node.getKey(), null, 0L);

                try {
                    String resultData = new ClientTaskDataList().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, dataMessageModel.createDataStream()).get();

                    dataList.addAll(convertStreamToDataList(resultData));
                }
                catch(Exception e){
                    Log.e(TAG, "Interrupted Exception in query *");
                }

			}

			filteredList.addAll(filterPrevVersions(dataList));


			for(DataMessageModel data : filteredList) {
				mCursor.newRow()
						.add("key", data.getKey())
						.add("value", data.getMessage());
			}

			return mCursor;

		} else if(selection.equals("@")){

            List<String> targetList = getReplicaTargetList(myEmulatorId);


            for(String tg : targetList){

                DataMessageModel dataMessageModel = new DataMessageModel(null, null, DhtOperationTypeConstant.DATA_QUERY_GLOBAL, null, tg, null, 0L);

                try {
                    String resultData = new ClientTaskDataList().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, dataMessageModel.createDataStream()).get();

                    dataList.addAll(convertStreamToDataList(resultData));
                }
                catch(Exception e){
                    Log.e(TAG, "Interrupted Exception in query *");
                }

            }

            DataMessageModel dataMessageModel = new DataMessageModel(null, null, DhtOperationTypeConstant.DATA_QUERY_LOCAL, null, myEmulatorId, null, 0L);
            dataList.addAll(queryDataFromLocal(dataMessageModel));

            filteredList.addAll(filterPrevVersions(dataList));

            List<DataMessageModel> myFilteredList = new ArrayList<DataMessageModel>();

            for (DataMessageModel filterData : filteredList){

                List<String> myTarget = getTargetList(filterData);

                if(myTarget.contains(myEmulatorId)){

                    myFilteredList.add(filterData);

                }

            }


            for(DataMessageModel data : myFilteredList) {
                mCursor.newRow()
                        .add("key", data.getKey())
                        .add("value", data.getMessage());
            }

            return mCursor;






		} else{

			DataMessageModel dataMessageModel = new DataMessageModel(selection, null, DhtOperationTypeConstant.DATA_QUERY_KEY, null, null, myEmulatorId, 0L);

			List<String> targetList = getTargetList(dataMessageModel);

			for(String node : targetList){

                try {

                    dataMessageModel.setTargetNode(node);
                    String resultData = new ClientTaskDataList().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, dataMessageModel.createDataStream()).get();

                    List<DataMessageModel> temList = convertStreamToDataList(resultData);

                    dataList.addAll(temList);

                } catch(Exception e){
                    Log.e(TAG, "Interrupted Exception in query key");
                }

			}

			filteredList.addAll(filterPrevVersions(dataList));

			for(DataMessageModel data : filteredList) {
				mCursor.newRow()
						.add("key", data.getKey())
						.add("value", data.getMessage());
			}

			return mCursor;

		}
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
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


						if(strReceivedArr[0].equalsIgnoreCase("Data")){

							if(strReceivedArr.length >1) {

								DataMessageModel dataMessageModel = new DataMessageModel();
								dataMessageModel.createDataModel(strReceivedArr);

								if (DhtOperationTypeConstant.DATA_INSERT.equalsIgnoreCase(dataMessageModel.getDataOperationType())) {

									Log.i(TAG, "Insert Request" + "~" + dataMessageModel.createDataStream());

                                    insertDatatoLocal(dataMessageModel);
								}
								else if (DhtOperationTypeConstant.DATA_QUERY_KEY.equalsIgnoreCase(dataMessageModel.getDataOperationType())
											|| DhtOperationTypeConstant.DATA_QUERY_GLOBAL.equalsIgnoreCase(dataMessageModel.getDataOperationType())) {

									// query all data

									Log.d(TAG,"get msg::"+line);
									Log.d(TAG,"my id::"+myEmulatorId);


									List<DataMessageModel> dataList = queryDataFromLocal(dataMessageModel);

									DataOutputStream out = new DataOutputStream(socket.getOutputStream());
									out.writeUTF(convertDataListToStream(dataList));
									out.flush();
									out.close();

								} else if(DhtOperationTypeConstant.DATA_DELETE_KEY.equalsIgnoreCase(dataMessageModel.getDataOperationType())
											|| DhtOperationTypeConstant.DATA_DELETE_GLOBAL.equals(dataMessageModel.getDataOperationType())){

									deleteDataFromLocal(dataMessageModel);

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


	private class ClientTaskDataList extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... line) {

			String result = "";
			try {


				String strReceived = line[0].trim();
				String[] strReceivedArr = strReceived.split("~");

				DataMessageModel dataMessageModel = new DataMessageModel();
				dataMessageModel.createDataModel(strReceivedArr);

				if(dataMessageModel.getDataOperationType().equalsIgnoreCase(DhtOperationTypeConstant.DATA_INSERT)){

					Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(dataMessageModel.getTargetNode())*2);

                    socket.setSoTimeout(2000);

					PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
					out.println(dataMessageModel.createDataStream());

					socket.close();

				} else if(dataMessageModel.getDataOperationType().equalsIgnoreCase(DhtOperationTypeConstant.DATA_QUERY_KEY)
						|| dataMessageModel.getDataOperationType().equalsIgnoreCase(DhtOperationTypeConstant.DATA_QUERY_GLOBAL)){

					Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(dataMessageModel.getTargetNode())*2);

                    socket.setSoTimeout(2000);

					PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
					out.println(line[0]);

					DataInputStream ds = new DataInputStream(socket.getInputStream());
					result = ds.readUTF();

					ds.close();
					socket.close();

				} else if(dataMessageModel.getDataOperationType().equalsIgnoreCase(DhtOperationTypeConstant.DATA_DELETE_KEY)
						|| dataMessageModel.getDataOperationType().equalsIgnoreCase(DhtOperationTypeConstant.DATA_DELETE_GLOBAL)){

					Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(dataMessageModel.getTargetNode())*2);

                    socket.setSoTimeout(2000);

					PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
					out.println(dataMessageModel.createDataStream());

					socket.close();

				}

			} catch (SocketTimeoutException e) {
			    Log.e(TAG, "ClientTask SocketTimeoutException");
            } catch (UnknownHostException e) {
				Log.e(TAG, "ClientTask UnknownHostException");
			} catch (IOException e) {
				Log.e(TAG, "ClientTask socket IOException");
			}

			return result;
		}

	}


	private List<String> getReplicaTargetList(String targetNode){

		DataMessageModel first = nodeSet.first();

		List<String> targetList = new ArrayList<String>();
		targetList.add(targetNode);

		if(nodeSet.last().getKey().equals(targetNode)){

			targetList.add(first.getKey());
			targetList.add(nodeSet.higher(first).getKey());

		} else{

			for (DataMessageModel node : nodeSet) {

				if(node.getKey().equals(targetNode)){

					DataMessageModel next = nodeSet.higher(node);
					targetList.add(next.getKey());

					if(next.getKey().equals(nodeSet.last().getKey()))
						targetList.add(first.getKey());

					else
						targetList.add(nodeSet.higher(next).getKey());

				}

			}

		}

		return targetList;
	}


    private List<String> getTargetList(DataMessageModel dataMessageModel){

        List<String> targetList = new ArrayList<String>();

        try {

            DataMessageModel target = nodeSet.higher(dataMessageModel) != null?
                    nodeSet.higher(dataMessageModel): nodeSet.first();

            targetList.add(target.getKey());

            DataMessageModel target1 = nodeSet.higher(target) != null?
                    nodeSet.higher(target): nodeSet.first();

            targetList.add(target1.getKey());

            DataMessageModel target2 = nodeSet.higher(target1) != null?
                    nodeSet.higher(target1): nodeSet.first();

            targetList.add(target2.getKey());

            return targetList;

        } catch(Exception e){
            Log.e(TAG, "Error generating Hash");

        }
        return null;
    }


    private List<String> getNeighborList(DataMessageModel dataMessageModel){

        List<String> targetList = new ArrayList<String>();

        try {

            DataMessageModel nextNode = nodeSet.higher(dataMessageModel) != null?
                    nodeSet.higher(dataMessageModel): nodeSet.first();

            targetList.add(nextNode.getKey());

            DataMessageModel prevNode = nodeSet.lower(dataMessageModel) != null?
                    nodeSet.lower(dataMessageModel): nodeSet.last();

            targetList.add(prevNode.getKey());


            return targetList;

        } catch(Exception e){
            Log.e(TAG, "Error generating Hash");

        }
        return null;
    }

	private void insertDatatoLocal(DataMessageModel dataMessageModel){

			try {

                writeLock.lock();


                SharedPreferences sharedPreference = getContext().getSharedPreferences(DhtOperationTypeConstant.PREFERENCE_FILE, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreference.edit();

                editor.putString(dataMessageModel.getKey() + "~" + dataMessageModel.insertTimeStamp,
                        dataMessageModel.getMessage());

                editor.apply();




			} catch (Exception e) {
				Log.e(TAG, "File write failed");
			} finally {
			    writeLock.unlock();
            }


	}


    private void insertDataListtoLocal(List<DataMessageModel> dataMessageModelList){

        try {
            writeLock.lock();

            for(DataMessageModel data : dataMessageModelList){


                List<String> targets = getTargetList(data);

                if(targets.contains(myEmulatorId)){

                    SharedPreferences sharedPreference = getContext().getSharedPreferences(DhtOperationTypeConstant.PREFERENCE_FILE, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreference.edit();

                    editor.putString(data.getKey() + "~" + data.getInsertTimeStamp(),
                            data.getMessage());

                    editor.apply();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "File write failed");
        } finally {
            writeLock.unlock();
        }


    }



	private List<DataMessageModel> queryDataFromLocal(DataMessageModel dataMessageModel) {


        readLock.lock();

		FileInputStream inputStream;
		List<DataMessageModel> dataList = new ArrayList<DataMessageModel>();


		if (dataMessageModel.getDataOperationType().equals(DhtOperationTypeConstant.DATA_QUERY_GLOBAL) || DhtOperationTypeConstant.DATA_QUERY_LOCAL.equals(dataMessageModel.getDataOperationType())){


            SharedPreferences sharedPreference = getContext().getSharedPreferences(DhtOperationTypeConstant.PREFERENCE_FILE, Context.MODE_PRIVATE);

            Map<String, ?> allEntries = sharedPreference.getAll();

            for (Map.Entry<String, ?> entry : allEntries.entrySet()) {

                String[] keyToMatch = entry.getKey().trim().split("~");
                DataMessageModel messageModel = new DataMessageModel(keyToMatch[0], entry.getValue().toString(), dataMessageModel.getDataOperationType(), null, null, null, Long.parseLong(keyToMatch[1]));
                dataList.add(messageModel);

            }







		} else{

            SharedPreferences sharedPreference = getContext().getSharedPreferences(DhtOperationTypeConstant.PREFERENCE_FILE, Context.MODE_PRIVATE);

            Map<String, ?> allEntries = sharedPreference.getAll();

            for (Map.Entry<String, ?> entry : allEntries.entrySet()) {

                String[] keyToMatch = entry.getKey().trim().split("~");

                if(keyToMatch[0].equals(dataMessageModel.getKey())) {
                    DataMessageModel messageModel = new DataMessageModel(keyToMatch[0], entry.getValue().toString(), dataMessageModel.getDataOperationType(), null, null, null, Long.parseLong(keyToMatch[1]));
                    dataList.add(messageModel);
                }


            }





		}


        readLock.unlock();

		return filterPrevVersions(dataList);

	}


	private List<DataMessageModel> convertStreamToDataList(String stream){

		if(stream != null){

			List<DataMessageModel> dataList = new ArrayList<DataMessageModel>();
			String strReceived = stream.trim();

//			Log.d(TAG, "recieved::" + strReceived);
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


	private List<DataMessageModel> filterPrevVersions(List<DataMessageModel> dataList){

		List<DataMessageModel> filteredList = new ArrayList<DataMessageModel>();

		Collections.sort(dataList, new Comparator<DataMessageModel>(){
			public int compare(DataMessageModel d1, DataMessageModel d2) {
                int keyCmp = d1.getKey().compareTo(d2.getKey());
                if (keyCmp != 0) {
                    return keyCmp;
                }
                return Long.compare(d1.getInsertTimeStamp(), d2.getInsertTimeStamp());
			}
		});

		if(!dataList.isEmpty()){
			Iterator<DataMessageModel> itr = dataList.iterator();

			DataMessageModel prev = itr.next();
			DataMessageModel curr;

			while(itr.hasNext()) {
				curr = itr.next();

				if(!curr.getKey().equals(prev.getKey()))
					filteredList.add(prev);

				prev = curr;
			}

			filteredList.add(prev);
		}

		return filteredList;
	}


	private void deleteDataFromLocal(DataMessageModel dataMessageModel){

        writeLock.lock();

        Log.w(TAG,"sync: delete fired!!"+ dataMessageModel.toString());
		if(dataMessageModel.getDataOperationType().equals(DhtOperationTypeConstant.DATA_DELETE_GLOBAL)
				|| dataMessageModel.getDataOperationType().equals(DhtOperationTypeConstant.DATA_DELETE_LOCAL)){


            SharedPreferences sharedPreference = getContext().getSharedPreferences(DhtOperationTypeConstant.PREFERENCE_FILE, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreference.edit();
            editor.clear();
            editor.apply();

		} else{

		    // https://stackoverflow.com/questions/22089411/how-to-get-all-keys-of-sharedpreferences-programmatically-in-android

            SharedPreferences sharedPreference = getContext().getSharedPreferences(DhtOperationTypeConstant.PREFERENCE_FILE, Context.MODE_PRIVATE);

            List<String> toBeDelete = new ArrayList<String>();

            Map<String, ?> allEntries = sharedPreference.getAll();

            for (Map.Entry<String, ?> entry : allEntries.entrySet()) {

                String[] keyToMatch = entry.getKey().trim().split("~");

                if(keyToMatch[0].equals(dataMessageModel.getKey()))
                    toBeDelete.add(entry.getKey());

            }

            SharedPreferences.Editor editor = sharedPreference.edit();

            Iterator<String> itr = toBeDelete.iterator();

            while(itr.hasNext())
                editor.remove(itr.next());


            editor.apply();

		}
        writeLock.unlock();

	}



    private void recoverDataAfterFailure(){

        DataMessageModel messageModelForDelete = new DataMessageModel(myEmulatorId, "from recovery", DhtOperationTypeConstant.DATA_DELETE_LOCAL, null, myEmulatorId, null, 0L);

        List<String> targetList = getNeighborList(messageModelForDelete);
        List<DataMessageModel> dataList = new ArrayList<DataMessageModel>();
        List<DataMessageModel> filteredList = new ArrayList<DataMessageModel>();

        for(String target : targetList){

            DataMessageModel msgModelForRetrieval = new DataMessageModel(null, null, DhtOperationTypeConstant.DATA_QUERY_GLOBAL, null, target, null, 0L);

            try {
                String resultData = new ClientTaskDataList().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgModelForRetrieval.createDataStream()).get();

                dataList.addAll(convertStreamToDataList(resultData));
            }
            catch(Exception e){
                Log.e(TAG, "Interrupted Exception recovery");
            }

        }
        filteredList.addAll(filterPrevVersions(dataList));

        insertDataListtoLocal(filteredList);
    }



    private class SyncMessagesJob implements Runnable {

        protected Handler mainHandler;

        public SyncMessagesJob(Handler mainHandler) {
            this.mainHandler = mainHandler;
        }

        public void run() {
            recoverDataAfterFailure();

            mainHandler.postDelayed(this, 10000);
        }
    }



}
