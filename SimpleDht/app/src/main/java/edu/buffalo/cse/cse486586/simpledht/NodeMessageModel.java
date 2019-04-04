package edu.buffalo.cse.cse486586.simpledht;

import java.util.Objects;

public class NodeMessageModel implements Comparable<NodeMessageModel>{
    String nodeId;
    String successorId;
    String predecessorId;
    Boolean hasJoined;
    String nodeOperation;



    static final String TYPE = "Node";

    public NodeMessageModel(String nodeId, String successorId, String predecessorId, Boolean hasJoined, String nodeOperation) {
        this.nodeId = nodeId;
        this.successorId = successorId;
        this.predecessorId = predecessorId;
        this.hasJoined = hasJoined;
        this.nodeOperation = nodeOperation;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getSuccessorId() {
        return successorId;
    }

    public void setSuccessorId(String successorId) {
        this.successorId = successorId;
    }

    public String getPredecessorId() {
        return predecessorId;
    }

    public void setPredecessorId(String predecessorId) {
        this.predecessorId = predecessorId;
    }

    public Boolean getHasJoined() {
        return hasJoined;
    }

    public void setHasJoined(Boolean hasJoined) {
        this.hasJoined = hasJoined;
    }

    public String getNodeOperation() {
        return nodeOperation;
    }

    public void setNodeOperation(String nodeOperation) {
        this.nodeOperation = nodeOperation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NodeMessageModel)) return false;
        NodeMessageModel nodeMessageModel = (NodeMessageModel) o;
        return getNodeId() == nodeMessageModel.getNodeId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getNodeId());
    }

    @Override
    public String toString() {
        return "NodeMessageModel{" +
                "nodeId='" + nodeId + '\'' +
                ", successorId='" + successorId + '\'' +
                ", predecessorId='" + predecessorId + '\'' +
                ", hasJoined=" + hasJoined +
                ", nodeOperation='" + nodeOperation + '\'' +
                '}';
    }

    public String createNodeStream(){
        return  TYPE +
                '~' + nodeId +
                '~' + successorId +
                '~' + predecessorId +
                '~' + Boolean.valueOf(hasJoined) +
                '~' + nodeOperation ;

    }

    public static NodeMessageModel createNodeModel(String[] strReceived){
        NodeMessageModel nodeMessage = new NodeMessageModel(strReceived[1], strReceived[2], strReceived[3], Boolean.parseBoolean(strReceived[4]), strReceived[5]);
        return nodeMessage;

    }

    public int compareTo(NodeMessageModel node){
        try {
            return SimpleDhtUtil.genHash(this.nodeId).compareTo(SimpleDhtUtil.genHash(node.nodeId));
        } catch(Exception e){

        }
        return 0;
    }

}
