package edu.buffalo.cse.cse486586.simpledht;

public class DataMessageModel {

    String key;
    String message;
    String dataOperationType;
    String position;
    String targetNode;


    static final String TYPE = "Data";

    public DataMessageModel(String key, String message, String dataOperationType, String position, String targetNode) {
        this.key = key;
        this.message = message;
        this.dataOperationType = dataOperationType;
        this.position = position;
        this.targetNode = targetNode;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDataOperationType() {
        return dataOperationType;
    }

    public void setDataOperationType(String dataOperationType) {
        this.dataOperationType = dataOperationType;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getTargetNode() {
        return targetNode;
    }

    public void setTargetNode(String targetNode) {
        this.targetNode = targetNode;
    }

    public String createDataStream(){
        return  TYPE +
                '~' + key +
                '~' + message +
                '~' + dataOperationType +
                '~' + position +
                '~' + targetNode;

    }

    public static DataMessageModel createDataModel(String[] strReceived){
        DataMessageModel dataMessageModel = new DataMessageModel(strReceived[1], strReceived[2], strReceived[3], strReceived[4], strReceived[5]);
        return dataMessageModel;

    }

}
