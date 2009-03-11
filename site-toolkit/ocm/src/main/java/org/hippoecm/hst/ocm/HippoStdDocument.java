package org.hippoecm.hst.ocm;

import org.apache.jackrabbit.ocm.mapper.impl.annotation.Field;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node;

@Node(jcrType="hippo:document")
public class HippoStdDocument extends HippoStdNode {

    protected String stateSummary;
    protected String state;

    @Field(jcrName="hippostd:stateSummary") 
    public String getStateSummary() {
        return this.stateSummary;
    }
    
    public void setStateSummary(String stateSummary) {
        this.stateSummary = stateSummary;
    }
    
    @Field(jcrName="hippostd:state") 
    public String getState() {
        return this.state;
    }
    
    public void setState(String state) {
        this.state = state;
    }
    
}