package org.hippoecm.hst.component.support.ocm;

import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.ocm.HippoStdNode;

public class HippoStdNodeWrapper {
    private HippoStdNode hippoStdNode;
    private HstRequestContext hstRequestContext;
  
    public HippoStdNodeWrapper(HippoStdNode hippoStdNode, HstRequestContext hstRequestContext) {
        this.hippoStdNode = hippoStdNode;
        this.hstRequestContext = hstRequestContext;
    }
    

    public HippoStdNode getHippoStdNode(){
        return hippoStdNode;
    }
    
    
    
    public HstLink getLink(){
        return null;
    }
    
   
}
