package org.hippoecm.hst.core.container;

import java.util.Iterator;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.core.container.ValveContext;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.HstRequestProcessor;

public class AggregationValve extends AbstractValve {
    
    protected HstRequestProcessor requestProcessor;
    
    public AggregationValve(HstRequestProcessor requestProcessor) {
        this.requestProcessor = requestProcessor;
    }
    
    @Override
    public void invoke(HstRequestContext request, ValveContext context) throws Exception {
        
        if (!isResourceRequest()) {
            HstComponentConfiguration root = null;
            
            //
            //Page page = request.getPage();
            //root = page.getRootComponent();
            
            if (root != null) {
                aggregateAndProcessBeforeRender(request, root);
                aggregateAndProcessRender(request, root);
            }
        }
        
        // continue
        context.invokeNext(request);
    }

    protected void aggregateAndProcessBeforeRender(HstRequestContext context, HstComponentConfiguration component) throws Exception {
        
        for(Iterator<HstComponentConfiguration> it = component.getChildren().iterator(); it.hasNext();) {
            aggregateAndProcessBeforeRender(context, it.next());
        }
        
        this.requestProcessor.processBeforeRender(context, component);

    }
    
    protected void aggregateAndProcessRender(HstRequestContext context, HstComponentConfiguration component) throws Exception {
        for(Iterator<HstComponentConfiguration> it = component.getChildren().iterator(); it.hasNext();) {
            aggregateAndProcessRender(context, it.next());
        }
    
        this.requestProcessor.processRender(context, component);
    }
}
