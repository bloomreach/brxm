package org.hippoecm.hst.core.container;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;


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
    
    public void beforeInvoke(ServletContext servletContext, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException {
        invokeValves(servletContext, servletRequest, servletResponse, preInvokingValves);
    }

    public void invoke(ServletContext servletContext, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException {
        invokeValves(servletContext, servletRequest, servletResponse, invokingValves);
    }
    
    public void afterInvoke(ServletContext servletContext, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException {
        invokeValves(servletContext, servletRequest, servletResponse, postInvokingValves);
    }
    
    private void invokeValves(ServletContext servletContext, ServletRequest servletRequest, ServletResponse servletResponse, Valve [] valves) throws ContainerException {
        if (valves != null && valves.length > 0) {
            new Invocation(servletContext, servletRequest, servletResponse, valves).invokeNext();
        }
    }

    public void destroy() throws ContainerException {
    }    
    
    private static final class Invocation implements ValveContext
    {

        private final Valve[] valves;

        private final ServletContext servletContext;
        private final ServletRequest servletRequest;
        private final ServletResponse servletResponse;
        private HstComponentWindow rootComponentWindow;

        private int at = 0;

        public Invocation(ServletContext servletContext, ServletRequest servletRequest, ServletResponse servletResponse, Valve[] valves) {
            this.servletContext = servletContext;
            this.servletRequest = servletRequest;
            this.servletResponse = servletResponse;
            this.valves = valves;
        }

        public void invokeNext() throws ContainerException {
            if (at < valves.length)
            {
                Valve next = valves[at];
                at++;
                next.invoke(this);
            }
        }
        
        public ServletContext getServletContext() {
            return this.servletContext;
        }

        public ServletRequest getServletRequest() {
            return this.servletRequest;
        }

        public ServletResponse getServletResponse() {
            return this.servletResponse;
        }

        public void setRootComponentWindow(HstComponentWindow rootComponentWindow) {
            this.rootComponentWindow = rootComponentWindow;
        }
        
        public HstComponentWindow getRootComponentWindow() {
            return this.rootComponentWindow;
        }
    }
}
