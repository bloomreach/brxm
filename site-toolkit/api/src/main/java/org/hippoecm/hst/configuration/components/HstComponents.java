package org.hippoecm.hst.configuration.components;

import org.hippoecm.hst.service.Service;

public interface HstComponents extends Service{

    public HstComponent getComponent(String path);
    
}
