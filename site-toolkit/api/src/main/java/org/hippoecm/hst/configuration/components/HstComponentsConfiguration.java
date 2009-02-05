package org.hippoecm.hst.configuration.components;

import org.hippoecm.hst.service.Service;

public interface HstComponentsConfiguration extends Service{

    public HstComponentConfiguration getComponent(String path);
    
}
