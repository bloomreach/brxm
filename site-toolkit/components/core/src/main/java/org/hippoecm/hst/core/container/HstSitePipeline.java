package org.hippoecm.hst.core.container;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.hippoecm.hst.core.request.HstRequestContext;

public class HstSitePipeline implements Pipeline
{
    protected String name;
    protected Valve [] valves;
    
    public HstSitePipeline(String name, Valve [] valves) throws Exception
    {
        this.name = name;
        this.valves = valves;
    }

    public String getName()
    {
        return this.name;
    }

    public void initialize() throws Exception
    {
    }

    public void invoke(ServletRequest servletRequest, ServletResponse servletResponse, HstRequestContext request) throws Exception
    {
        new Invocation(servletRequest, servletResponse, valves).invokeNext(request);
    }
    
    private static final class Invocation implements ValveContext
    {

        private final ServletRequest servletRequest;
        private final ServletResponse servletResponse;
        private final Valve[] valves;

        private int at = 0;

        public Invocation(ServletRequest servletRequest, ServletResponse servletResponse, Valve[] valves)
        {
            this.servletRequest = servletRequest;
            this.servletResponse = servletResponse;
            this.valves = valves;
        }

        public void invokeNext(HstRequestContext request) throws Exception
        {
            if (at < valves.length)
            {
                Valve next = valves[at];
                at++;
                next.invoke(request, this);
            }
        }

        public ServletRequest getServletRequest() {
            return this.servletRequest;
        }

        public ServletResponse getServletResponse() {
            return this.servletResponse;
        }
    }
}
