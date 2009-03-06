package org.hippoecm.hst.container;

import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;

public final class HstContainerPortletContext {
    
    private static final ThreadLocal<PortletRequest> tlRequest = new ThreadLocal<PortletRequest>();
    private static final ThreadLocal<PortletResponse> tlResponse = new ThreadLocal<PortletResponse>();

    protected HstContainerPortletContext(PortletRequest request, PortletResponse response) {
        tlRequest.set(request);
        tlResponse.set(response);
    }
    
    public static final PortletRequest getCurrerntRequest() {
        return tlRequest.get();
    }
    
    public static final PortletResponse getCurrentResponse() {
        return tlResponse.get();
    }
    
    protected static void reset() {
        tlRequest.set(null);
        tlResponse.set(null);
    }
}
