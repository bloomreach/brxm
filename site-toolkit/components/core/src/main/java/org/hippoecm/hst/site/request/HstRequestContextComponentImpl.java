package org.hippoecm.hst.site.request;

import javax.jcr.Repository;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.HstRequestContextComponent;

public class HstRequestContextComponentImpl implements HstRequestContextComponent
{
    
    protected Repository repository;
    
    public HstRequestContextComponentImpl(Repository repository)
    {
        this.repository = repository;
    }
    
    public HstRequestContext create(Object request, Object response, Object config)
    {
        HttpServletRequest servletRequest = (HttpServletRequest) request;
        HttpServletResponse servletResponse = (HttpServletResponse) response;
        ServletConfig servletConfig = (ServletConfig) config;

        // TODO:
        HstRequestContextImpl context = new HstRequestContextImpl();
        context.setRepository(this.repository);
        return context;
    }

    public void release(HstRequestContext context)
    {
    }
}
