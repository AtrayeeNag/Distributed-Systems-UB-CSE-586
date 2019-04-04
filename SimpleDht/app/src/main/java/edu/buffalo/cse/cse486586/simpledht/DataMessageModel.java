package edu.buffalo.cse.cse486586.simpledht;

public class DataMessageModel {

    String key;
    String message;
    String dataOperationType;


    static final String TYPE = "Data";

    public DataMessageModel(String key, String message, String dataOperationType) {
        this.key = key;
        this.message = message;
        this.dataOperationType = dataOperationType;
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

    public String createDataStream(){
        return  TYPE +
                '~' + key +
                '~' + message +
                '~' + dataOperationType;

    }


}
