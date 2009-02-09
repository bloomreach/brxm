package org.hippoecm.hst.core.container;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.hippoecm.hst.core.request.HstRequestContext;

public class HstSitePipeline implements Pipeline
{

    protected Valve [] preInvokingValves;
    protected Valve [] invokingValves;
    protected Valve [] postInvokingValves;
    
    public HstSitePipeline() throws Exception
    {
    }

    public void setPreInvokingValves(Valve [] preInvokingValves) {
        this.preInvokingValves = preInvokingValves;
    }
    
    public void setInvokingValves(Valve [] invokingValves) {
        this.invokingValves = invokingValves;
    }
    
    public void setPostInvokingValves(Valve [] postInvokingValves) {
        this.postInvokingValves = postInvokingValves;
    }

    public void initialize() throws ContainerException {
    }
    
    public void beforeInvoke(ServletRequest servletRequest, ServletResponse servletResponse, HstRequestContext request) throws ContainerException {
        invokeValves(servletRequest, servletResponse, request, preInvokingValves);
    }

    public void invoke(ServletRequest servletRequest, ServletResponse servletResponse, HstRequestContext request) throws ContainerException {
        invokeValves(servletRequest, servletResponse, request, invokingValves);
    }
    
    public void afterInvoke(ServletRequest servletRequest, ServletResponse servletResponse, HstRequestContext request) throws ContainerException {
        invokeValves(servletRequest, servletResponse, request, postInvokingValves);
    }
    
    private void invokeValves(ServletRequest servletRequest, ServletResponse servletResponse, HstRequestContext request, Valve [] valves) throws ContainerException {
        if (valves != null && valves.length > 0) {
            new Invocation(servletRequest, servletResponse, valves).invokeNext(request);
        }
    }

    public void destroy() throws ContainerException {
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

        public void invokeNext(HstRequestContext request) throws ContainerException
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
