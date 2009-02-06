package org.hippoecm.hst.configuration.components;

import javax.jcr.Node;

public class HstComponentConfigurationRootService extends HstComponentConfigurationService{

    public HstComponentConfigurationRootService(HstComponentsConfigurationService pageMappingService, Node jcrNode) {
        super(pageMappingService, null ,jcrNode);
    }

}
