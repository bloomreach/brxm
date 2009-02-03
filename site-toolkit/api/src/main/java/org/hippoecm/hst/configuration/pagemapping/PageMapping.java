package org.hippoecm.hst.configuration.pagemapping;

import org.hippoecm.hst.configuration.pagemapping.components.Component;
import org.hippoecm.hst.service.Service;

public interface PageMapping extends Service{

    public Component getComponent(String component);
    
}
