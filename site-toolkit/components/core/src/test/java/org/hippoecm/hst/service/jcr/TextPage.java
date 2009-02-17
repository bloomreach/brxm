package org.hippoecm.hst.service.jcr;

import org.hippoecm.hst.service.ServiceNamespace;

@ServiceNamespace(prefix = "testproject")

public interface TextPage extends HippoStd{
    
    public String getTitle();
    public String getSummary();
     
}
