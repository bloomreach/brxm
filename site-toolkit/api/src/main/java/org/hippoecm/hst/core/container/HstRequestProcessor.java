package org.hippoecm.hst.core.container;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public interface HstRequestProcessor {

    void processRequest(ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException;

}
