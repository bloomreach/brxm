package org.hippoecm.hst.core.request;

public interface HstRequestContextComponent
{
    /**
     * Creates a request context for the given servlet request.
     * 
     * @param req
     * @param resp
     * @param config
     * @return
     */
    HstRequestContext create();

    /**
     * Release a request context back to the context pool.
     * 
     * @param context
     */
    void release(HstRequestContext context);
    
}
