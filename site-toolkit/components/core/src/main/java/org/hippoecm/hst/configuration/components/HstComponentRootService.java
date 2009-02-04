package org.hippoecm.hst.configuration.components;

import javax.jcr.Node;

public class HstComponentRootService extends HstComponentService{

    public HstComponentRootService(HstComponents pageMappingService, Node jcrNode) {
        super(pageMappingService, jcrNode);
    }

}
