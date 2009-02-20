package org.hippoecm.hst.core.container;

import javax.servlet.ServletConfig;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.request.HstRequestContext;

public interface HstComponentWindowFactory {

    HstComponentWindow create(ServletConfig servletConfig, HstRequestContext requestContext, HstComponentConfiguration compConfig, HstComponentFactory compFactory) throws HstComponentException;

    HstComponentWindow create(ServletConfig servletConfig, HstRequestContext requestContext, HstComponentConfiguration compConfig, HstComponentFactory compFactory, HstComponentWindow parentWindow) throws HstComponentException;
    
}
