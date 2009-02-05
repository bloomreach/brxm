package org.hippoecm.hst.core.container;

<<<<<<< .mine
import org.hippoecm.hst.core.component.HstComponent;
=======
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
>>>>>>> .r16280
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

        HstComponentConfiguration root = null;
        //Page page = request.getPage();
        //root = page.getRootComponent();
        
        if (root != null) {
            aggregateAndProcessBeforeRender(request, root);
            aggregateAndProcessRender(request, root);
        }
        
        // continue
        context.invokeNext(request);
    }

<<<<<<< .mine
    protected void aggregateAndProcessBeforeRender(HstRequestContext context, HstComponent component) throws Exception {
        HstComponent [] childComponents = null;
        
        this.requestProcessor.processBeforeRender(context, component);
=======
    protected void aggregateAndRender(HstRequestContext context, HstComponentConfiguration component) throws Exception {
        HstComponentConfiguration [] childComponents = (HstComponentConfiguration []) component.getChildServices();
>>>>>>> .r16280

        if (childComponents != null) {
            for (HstComponentConfiguration child : childComponents)
            {
                aggregateAndProcessRender(context, child);
            }
        }
    }
    
    protected void aggregateAndProcessRender(HstRequestContext context, HstComponent component) throws Exception {
        HstComponent [] childComponents = null;

        if (childComponents != null) {
            for (HstComponent child : childComponents)
            {
                aggregateAndProcessRender(context, child);
            }
        }
        
        this.requestProcessor.processRender(context, component);
    }
}
