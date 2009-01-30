package org.hippoecm.hst.core.request;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
    HstRequestContext create(HttpServletRequest req, HttpServletResponse resp, ServletConfig config);

    /**
     * Release a request context back to the context pool.
     * 
     * @param context
     */
    void release(HstRequestContext context);
    
}
