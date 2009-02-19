package org.hippoecm.hst.core.component;

import org.hippoecm.hst.core.container.HstContainerURL;


public interface HstURLFactory {

    void setParameterNameComponentSeparator(String parameterNameComponentSeparator);
    
    String getParameterNameComponentSeparator();

    HstURL createURL(String type, String parameterNamespace, HstContainerURL base);
    
}
