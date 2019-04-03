package edu.buffalo.cse.cse486586.simpledht;

import java.security.NoSuchAlgorithmException;
import java.util.Objects;

public class NodeModel implements Comparable<NodeModel>{
    String nodeId;
    String successorId;
    String predecessorId;
    String incomingNodeId;
    Boolean hasJoined;


    static final String TYPE = "Node";

    public NodeModel(String nodeId, String successorId, String predecessorId, String incomingNodeId, Boolean hasJoined) {
        this.nodeId = nodeId;
        this.successorId = successorId;
        this.predecessorId = predecessorId;
        this.incomingNodeId = incomingNodeId;
        this.hasJoined = hasJoined;
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

    public String getIncomingNodeId() {
        return incomingNodeId;
    }

    public void setIncomingNodeId(String incomingNodeId) {
        this.incomingNodeId = incomingNodeId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NodeModel)) return false;
        NodeModel nodeModel = (NodeModel) o;
        return getNodeId() == nodeModel.getNodeId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getNodeId());
    }

    @Override
    public String toString() {
        return "NodeModel{" +
                "nodeId='" + nodeId + '\'' +
                ", successorId='" + successorId + '\'' +
                ", predecessorId='" + predecessorId + '\'' +
                ", incomingNodeId='" + incomingNodeId + '\'' +
                ", hasJoined=" + hasJoined +
                '}';
    }

    public String createNodeStream(){
        return  TYPE +
                '~' + nodeId +
                '~' + successorId +
                '~' + predecessorId +
                '~' + incomingNodeId +
                '~' + Boolean.valueOf(hasJoined);

    }

    public static NodeModel createNodeModel(String[] strReceived){
        NodeModel node = new NodeModel(strReceived[1], strReceived[2], strReceived[3], strReceived[3], Boolean.parseBoolean(strReceived[5]));
        return node;

    }

    public int compareTo(NodeModel node){
        try {
            return SimpleDhtUtil.genHash(this.nodeId).compareTo(SimpleDhtUtil.genHash(node.nodeId));
        } catch(Exception e){

        }
        return 0;
    }

}
