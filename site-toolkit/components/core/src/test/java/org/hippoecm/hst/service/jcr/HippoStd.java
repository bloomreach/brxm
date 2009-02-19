package org.hippoecm.hst.service.jcr;

import org.hippoecm.hst.service.UnderlyingServiceAware;
import org.hippoecm.hst.service.ServiceNamespace;

@ServiceNamespace(prefix = "hippostd")
public interface HippoStd extends UnderlyingServiceAware {
    public String getState();
}
