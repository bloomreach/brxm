package org.hippoecm.hst.core.request;

import javax.jcr.Repository;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class HstRequestContextComponentImpl implements HstRequestContextComponent
{
    
    protected Repository repository;
    
    public HstRequestContextComponentImpl(Repository repository)
    {
        this.repository = repository;
    }
    
    public HstRequestContext create(HttpServletRequest req, HttpServletResponse res, ServletConfig config)
    {
        HstRequestContextImpl context = new HstRequestContextImpl();
        // TODO:
        HstRequest hstRequest = null;
        HstResponse hstResponse = null;
        context.setRequest(hstRequest);
        context.setResponse(hstResponse);
        context.setRepository(this.repository);
        return context;
    }

    public void release(HstRequestContext context)
    {
    }
}
