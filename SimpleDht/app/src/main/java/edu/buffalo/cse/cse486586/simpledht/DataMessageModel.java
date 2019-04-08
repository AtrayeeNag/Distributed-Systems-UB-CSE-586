package edu.buffalo.cse.cse486586.simpledht;

public class DataMessageModel {

    String key;
    String message;
    String dataOperationType;
    String position;
    String targetNode;
    String originNode;


    static final String TYPE = "Data";

    public DataMessageModel() {
    }

    public DataMessageModel(String key, String message, String dataOperationType, String position, String targetNode, String originNode) {
        this.key = key;
        this.message = message;
        this.dataOperationType = dataOperationType;
        this.position = position;
        this.targetNode = targetNode;
        this.originNode = originNode;
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

    public String getOriginNode() {
        return originNode;
    }

    public void setOriginNode(String originNode) {
        this.originNode = originNode;
    }

    public String createDataStream(){
        return  TYPE +
                '~' + key +
                '~' + message +
                '~' + dataOperationType +
                '~' + position +
                '~' + targetNode +
                '~' + originNode;

    }

    public void createDataModel(String[] strReceived){

        this.key = strReceived[1];
        this.message = strReceived[2];
        this.dataOperationType = strReceived[3];
        this.position = strReceived[4];
        this.targetNode = strReceived[5];
        this.originNode = strReceived[6];

    }

}
