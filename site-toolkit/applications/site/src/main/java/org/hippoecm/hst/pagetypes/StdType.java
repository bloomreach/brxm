package org.hippoecm.hst.pagetypes;

import org.hippoecm.hst.service.ServiceNamespace;
import org.hippoecm.hst.service.UnderlyingServiceAware;

@ServiceNamespace(prefix = "hippostd")
public interface StdType extends UnderlyingServiceAware{    
    public String getState();    
    public String getStateSummary();    
}
