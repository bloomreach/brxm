package org.hippoecm.hst.container;

import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;


public final class HstContainerPortletContext {
    
    private static final ThreadLocal<PortletRequest> tlRequest = new ThreadLocal<PortletRequest>();
    private static final ThreadLocal<PortletResponse> tlResponse = new ThreadLocal<PortletResponse>();

    public static final PortletRequest getCurrentRequest() {
        return tlRequest.get();
    }
    
    public static final PortletResponse getCurrentResponse() {
        return tlResponse.get();
    }
    
    protected static void reset(PortletRequest portletRequest, PortletResponse portletResponse) {
        tlRequest.set(portletRequest);
        tlResponse.set(portletResponse);
    }
    
}
