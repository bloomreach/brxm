package org.hippoecm.hst.container;


public final class HstContainerPortletContext {
    
    private static final ThreadLocal<Object> tlRequest = new ThreadLocal<Object>();
    private static final ThreadLocal<Object> tlResponse = new ThreadLocal<Object>();

    protected HstContainerPortletContext(Object request, Object response) {
        tlRequest.set(request);
        tlResponse.set(response);
    }
    
    public static final <T> T getCurrentRequest() {
        return (T) tlRequest.get();
    }
    
    public static final <T> T getCurrentResponse() {
        return (T) tlResponse.get();
    }
    
    protected static void reset() {
        tlRequest.set(null);
        tlResponse.set(null);
    }
}
