package org.hippoecm.hst.content.beans.standard;

import org.hippoecm.hst.content.beans.Node;

@Node(jcrType="hippo:document")
public class HippoDocument extends HippoNode{

    public String getStateSummary() {
        return valueProvider.getString("hippo:stateSummary");
    }
    
    public String getState() {
        return valueProvider.getString("hippo:state");
    }
}
