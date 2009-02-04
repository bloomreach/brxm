package org.hippoecm.hst.core.container;

import org.hippoecm.hst.configuration.components.HstComponent;
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

        HstComponent root = null;
        //Page page = request.getPage();
        //root = page.getRootComponent();
        
        if (root != null) {
            aggregateAndRender(request, root);
        }
        
        // continue
        context.invokeNext(request);
    }

    protected void aggregateAndRender(HstRequestContext context, HstComponent component) throws Exception {
        HstComponent [] childComponents = (HstComponent []) component.getChildServices();

        if (childComponents != null) {
            for (HstComponent child : childComponents)
            {
                aggregateAndRender(context, child);
            }
        }
        
        this.requestProcessor.render(context, component);
    }
}
