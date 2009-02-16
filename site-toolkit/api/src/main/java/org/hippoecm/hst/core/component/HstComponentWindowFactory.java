package org.hippoecm.hst.core.component;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;

public interface HstComponentWindowFactory {

    HstComponentWindow create(HstComponentConfiguration compConfig, HstComponentFactory compFactory) throws HstComponentException;

    HstComponentWindow create(HstComponentConfiguration compConfig, HstComponentFactory compFactory, String namespacePrefix) throws HstComponentException;
    
}
