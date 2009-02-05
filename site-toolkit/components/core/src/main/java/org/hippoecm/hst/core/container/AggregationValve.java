package org.hippoecm.hst.core.container;

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
        HstComponentConfiguration [] children = null;
        
        this.requestProcessor.processBeforeRender(context, component);

        if (children != null) {
            for (HstComponentConfiguration child : children)
            {
                aggregateAndProcessRender(context, child);
            }
        }
    }
    
    protected void aggregateAndProcessRender(HstRequestContext context, HstComponentConfiguration component) throws Exception {
        HstComponentConfiguration [] children = null;

        if (children != null) {
            for (HstComponentConfiguration child : children)
            {
                aggregateAndProcessRender(context, child);
            }
        }
        
        this.requestProcessor.processRender(context, component);
    }
}
