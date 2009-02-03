package org.hippoecm.hst.configuration.pagemapping.component;

import javax.jcr.Node;

import org.hippoecm.hst.configuration.pagemapping.PageMapping;
import org.hippoecm.hst.core.request.HstRequestContext;

public class JCRComponentRootService extends AbstractJCRComponentService{

    public JCRComponentRootService(PageMapping pageMappingService, Node jcrNode) {
        super(pageMappingService, jcrNode);
    }

    public void doAction(HstRequestContext hstRequestContext) {
       // does not exist for the root component
    }

    public void doRender(HstRequestContext hstRequestContext) {
        // does not exist for the root component
    }

}
