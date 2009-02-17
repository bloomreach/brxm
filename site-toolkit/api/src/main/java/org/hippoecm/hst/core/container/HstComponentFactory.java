package org.hippoecm.hst.core.container;

import javax.servlet.ServletConfig;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.component.HstComponentException;

public interface HstComponentFactory {
    
    void setServletConfig(ServletConfig servletConfig);
    
    HstComponent getComponentInstance(HstComponentConfiguration compConfig) throws HstComponentException;
    
}
