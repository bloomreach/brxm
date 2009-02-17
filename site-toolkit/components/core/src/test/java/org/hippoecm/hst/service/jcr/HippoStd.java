package org.hippoecm.hst.service.jcr;

import org.hippoecm.hst.service.ServiceNamespace;

@ServiceNamespace(prefix = "hippostd")
public interface HippoStd {
    public String getState();
}
