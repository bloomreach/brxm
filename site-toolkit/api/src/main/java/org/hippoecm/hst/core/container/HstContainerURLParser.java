package org.hippoecm.hst.core.container;

import javax.servlet.ServletRequest;

public interface HstContainerURLParser {

    HstContainerURL parseURL(ServletRequest servletRequest);
    
}
