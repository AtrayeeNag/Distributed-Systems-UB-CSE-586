package edu.buffalo.cse.cse486586.groupmessenger2;

import java.util.Comparator;

public class MessageModelComparator implements Comparator<MessageOrderModel> {

    @Override
    public int compare(MessageOrderModel a, MessageOrderModel b)
    {

        if(a.getSequenceNo() == b.getSequenceNo())
            return a.getProposalPort()- b.getProposalPort();

        else
            return a.getSequenceNo() - b.getSequenceNo();

    }
}
