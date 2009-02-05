package org.hippoecm.hst.site.container;

<<<<<<< .mine
import org.hippoecm.hst.core.component.HstComponent;
=======
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
>>>>>>> .r16280
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.HstRequestProcessor;

public class HstRequestProcessorImpl implements HstRequestProcessor {

<<<<<<< .mine
    public void processAction(HstRequestContext requestContext, HstComponent component) throws Exception {
        // TODO Auto-generated method stub

    }

    public void processBeforeRender(HstRequestContext requestContext, HstComponent component) throws Exception {
=======
    public void action(HstRequestContext requestContext, HstComponentConfiguration component) throws Exception {
>>>>>>> .r16280
        // TODO Auto-generated method stub

    }

<<<<<<< .mine
    public void processRender(HstRequestContext requestContext, HstComponent component) throws Exception {
=======
    public void render(HstRequestContext requestContext, HstComponentConfiguration component) throws Exception {
>>>>>>> .r16280
        // TODO Auto-generated method stub

    }

<<<<<<< .mine
    public void processServeResource(HstRequestContext requestContext, HstComponent component) throws Exception {
=======
    public void serveResource(HstRequestContext requestContext, HstComponentConfiguration component) throws Exception {
>>>>>>> .r16280
        // TODO Auto-generated method stub

    }

}
