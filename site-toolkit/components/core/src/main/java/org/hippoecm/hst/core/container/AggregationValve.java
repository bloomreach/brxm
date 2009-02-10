package org.hippoecm.hst.core.container;

import java.util.Iterator;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.core.request.HstRequestContext;

public class AggregationValve extends AbstractValve {
    
    protected HstRequestProcessor requestProcessor;
    
    public AggregationValve(HstRequestProcessor requestProcessor) {
        this.requestProcessor = requestProcessor;
    }
    
    @Override
    public void invoke(HstRequestContext request, ValveContext context) throws ContainerException {
        
        if (!context.getServletResponse().isCommitted() && !isResourceRequest()) {
            HstComponentConfiguration root = null;
            
            //
            //Page page = request.getPage();
            //root = page.getRootComponent();
            
            if (root != null) {
                aggregateAndProcessBeforeRender(context.getServletRequest(), context.getServletResponse(), request, root);
                aggregateAndProcessRender(context.getServletRequest(), context.getServletResponse(), request, root);
            }
        }
        
        // continue
        context.invokeNext(request);
    }

    protected void aggregateAndProcessBeforeRender(ServletRequest servletRequest, ServletResponse servletResponse, HstRequestContext context, HstComponentConfiguration component) throws ContainerException {
        
        this.requestProcessor.processBeforeRender(servletRequest, servletResponse, context, component);
        
        for(Iterator<HstComponentConfiguration> it = component.getChildren().values().iterator(); it.hasNext();) {
            aggregateAndProcessBeforeRender(servletRequest, servletResponse, context, it.next());
        }

    }
    
    protected void aggregateAndProcessRender(ServletRequest servletRequest, ServletResponse servletResponse, HstRequestContext context, HstComponentConfiguration component) throws ContainerException {
        for(Iterator<HstComponentConfiguration> it = component.getChildren().values().iterator(); it.hasNext();) {
            aggregateAndProcessRender(servletRequest, servletResponse, context, it.next());
        }
    
        this.requestProcessor.processRender(servletRequest, servletResponse, context, component);
    }
}
