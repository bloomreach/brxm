package org.hippoecm.hst.hstconfiguration.components.fragments;

import javax.jcr.Node;

import org.hippoecm.hst.configuration.components.HstComponents;
import org.hippoecm.hst.configuration.pagemapping.component.AbstractJCRComponentService;
import org.hippoecm.hst.core.request.HstRequestContext;

public class Navigation extends AbstractJCRComponentService{

    public Navigation(HstComponents pageMapping, Node jcrNode) {
        super(pageMapping, jcrNode);
    }
    
    public void doAction(HstRequestContext hstRequestContext) {
        
    }

    public void doRender(HstRequestContext hstRequestContext) {
    }

}
