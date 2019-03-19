package edu.buffalo.cse.cse486586.groupmessenger2;


import java.util.Objects;

public class MessageOrderModel {

    private int sequenceNo;
    private int proposalPort;
    private String message;
    private int agreedProposal;
    private Boolean isProposal;
    private Boolean isAgreement;
    private int myPort;
    private Boolean readyToDeliver;
    private int localMessageSequence;
    private Boolean isDummy;


    public MessageOrderModel(int sequenceNo, int proposalPort, String message, int agreedProposal,
                             Boolean isProposal, Boolean isAgreement, int myPort, Boolean readyToDeliver,
                             int localMessageSequence, Boolean isDummy){

        this.sequenceNo = sequenceNo;
        this.proposalPort = proposalPort;
        this.message = message;
        this.agreedProposal = agreedProposal;
        this.isProposal = isProposal;
        this.isAgreement = isAgreement;
        this.myPort = myPort;
        this.readyToDeliver = readyToDeliver;
        this.localMessageSequence = localMessageSequence;
        this.isDummy = isDummy;
    }

    public int getSequenceNo() {
        return sequenceNo;
    }

    public void setSequenceNo(int sequenceNo) {
        this.sequenceNo = sequenceNo;
    }

    public int getProposalPort() {
        return proposalPort;
    }

    public void setProposalPort(int proposalPort) {
        this.proposalPort = proposalPort;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getAgreedProposal() {
        return agreedProposal;
    }

    public void setAgreedProposal(int agreedProposal) {
        this.agreedProposal = agreedProposal;
    }

    public Boolean getProposal() { return isProposal; }

    public void setProposal(Boolean isProposal) {
        this.isProposal = isProposal;
    }

    public Boolean getAgreement() { return isAgreement; }

    public void setAgreement(Boolean isAgreement) { this.isAgreement = isAgreement; }

    public int getMyPort() {
        return myPort;
    }

    public void setMyPort(int myPort) {
        this.myPort = myPort;
    }

    public Boolean getReadyToDeliver() {
        return readyToDeliver;
    }

    public void setReadyToDeliver(Boolean readyToDeliver) { this.readyToDeliver = readyToDeliver; }

    public int getLocalMessageSequence() {
        return localMessageSequence;
    }

    public void setLocalMessageSequence(int localMessageSequence) { this.localMessageSequence = localMessageSequence; }

    public Boolean getDummy() { return isDummy; }

    public void setDummy(Boolean dummy) { isDummy = dummy; }

    public String createMessageStream(){

        return String.valueOf(sequenceNo) +
                '~' + String.valueOf(proposalPort) +
                '~' + message +
                '~' + String.valueOf(agreedProposal) +
                '~' + Boolean.valueOf(isProposal) +
                '~' + Boolean.valueOf(isAgreement) +
                '~' + String.valueOf(myPort) +
                '~' + Boolean.valueOf(readyToDeliver) +
                '~' + String.valueOf(localMessageSequence) +
                '~' + Boolean.valueOf(isDummy);

    }


    @Override
    public String toString() {

        return "MessageOrderModel{" +
                "sequenceNo=" + sequenceNo +
                ", incomingPort=" + proposalPort +
                ", message='" + message + '\'' +
                ", agreedProposal=" + agreedProposal +
                ", isProposal=" + isProposal +
                ", isAgreement=" + isAgreement +
                ", myPort=" + myPort +
                ", readyToDeliver=" + readyToDeliver +
                ", localMessageSequence=" + localMessageSequence +
                ", isDummy=" + isDummy +
                '}';

    }


    @Override
    public boolean equals(Object o) {

        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        MessageOrderModel that = (MessageOrderModel) o;

        return myPort == that.myPort &&
                localMessageSequence == that.localMessageSequence &&
                Objects.equals(message, that.message);

    }


}
