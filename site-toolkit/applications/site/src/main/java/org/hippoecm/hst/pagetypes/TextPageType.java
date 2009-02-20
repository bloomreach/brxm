package org.hippoecm.hst.pagetypes;

import org.hippoecm.hst.service.ServiceNamespace;

@ServiceNamespace(prefix = "testproject")

public interface TextPageType {
    
    public String getTitle();
    public String getSummary();
     
}
