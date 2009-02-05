package org.hippoecm.hst.configuration.components;

import javax.jcr.Node;

public class HstComponentConfigurationRootService extends HstComponentConfigurationService{

    public HstComponentConfigurationRootService(HstComponentsConfiguration pageMappingService, Node jcrNode) {
        super(pageMappingService, null ,jcrNode);
    }

}
