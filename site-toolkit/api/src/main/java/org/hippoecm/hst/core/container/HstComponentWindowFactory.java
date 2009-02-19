package org.hippoecm.hst.core.container;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.request.HstRequestContext;

public interface HstComponentWindowFactory {

    HstComponentWindow create(HstRequestContext requestContext, HstComponentConfiguration compConfig, HstComponentFactory compFactory) throws HstComponentException;

    HstComponentWindow create(HstRequestContext requestContext, HstComponentConfiguration compConfig, HstComponentFactory compFactory, HstComponentWindow parentWindow) throws HstComponentException;
    
}
