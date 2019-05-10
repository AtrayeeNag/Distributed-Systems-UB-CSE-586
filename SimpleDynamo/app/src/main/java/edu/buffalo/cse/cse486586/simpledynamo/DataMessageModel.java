package edu.buffalo.cse.cse486586.simpledynamo;

public class DataMessageModel implements Comparable<DataMessageModel>{

    String key;
    String message;
    String dataOperationType;
    String position;
    String targetNode;
    String originNode;
    long insertTimeStamp;

    static final String TYPE = "Data";

    public DataMessageModel() {
    }

    public DataMessageModel(String key) {
        this.key = key;
    }

    public DataMessageModel(String key, String message, String dataOperationType, String position, String targetNode, String originNode, long insertTimeStamp) {
        this.key = key;
        this.message = message;
        this.dataOperationType = dataOperationType;
        this.position = position;
        this.targetNode = targetNode;
        this.originNode = originNode;
        this.insertTimeStamp =insertTimeStamp;
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

    public long getInsertTimeStamp() {
        return insertTimeStamp;
    }

    public void setInsertTimeStamp(long insertTimeStamp) {
        this.insertTimeStamp = insertTimeStamp;
    }

    public String createDataStream(){
        return  TYPE +
                '~' + key +
                '~' + message +
                '~' + dataOperationType +
                '~' + position +
                '~' + targetNode +
                '~' + originNode +
                '~' + Long.toString(insertTimeStamp) ;

    }

    public void createDataModel(String[] strReceived){

        this.key = strReceived[1];
        this.message = strReceived[2];
        this.dataOperationType = strReceived[3];
        this.position = strReceived[4];
        this.targetNode = strReceived[5];
        this.originNode = strReceived[6];
        this.insertTimeStamp = Long.parseLong(strReceived[7]);

    }

    @Override
    public boolean equals(Object o) {
        try {
            if (this == o) return true;
            if (!(o instanceof DataMessageModel)) return false;
            DataMessageModel node = (DataMessageModel) o;
            return getKey() == node.getKey();
        } catch(Exception e){

        }
        return Boolean.FALSE;
    }

    public int compareTo(DataMessageModel node){
        try {
            return SimpleDhtUtil.genHash(this.key).compareTo(SimpleDhtUtil.genHash(node.key));
        } catch(Exception e){
            e.printStackTrace();

        }
        return 0;
    }


    @Override
    public String toString() {
        return "DataMessageModel{" +
                "key='" + key + '\'' +
                ", message='" + message + '\'' +
                ", dataOperationType='" + dataOperationType + '\'' +
                ", position='" + position + '\'' +
                ", targetNode='" + targetNode + '\'' +
                ", originNode='" + originNode + '\'' +
                ", insertTimeStamp=" + insertTimeStamp +
                '}';
    }
}
