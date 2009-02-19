package org.hippoecm.hst.core.component;

import org.hippoecm.hst.core.container.HstContainerURL;


public interface HstURLFactory {

    HstURL createURL(String type, String parameterNamespace, HstContainerURL base);
    
}
