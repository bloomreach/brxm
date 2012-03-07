package org.hippoecm.hst.core.container;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.request.HstRequestContext;

/**
 * Interface to implement as a developer to add custom filter for some request to skip or not skip some component for 
 * rendering  (for example skip {@link HstComponent#doBeforeRender(org.hippoecm.hst.core.component.HstRequest, org.hippoecm.hst.core.component.HstResponse)}
 * and its rendering template)
 * 
 */
public interface HstComponentWindowCreationFilter {

    /**
     * @param requestContext the {@link HstRequestContext}
     * @param compConfig the {@link HstComponentConfiguration}
     * @return <code>true<code> when the component window must be skipped
     * @throws HstComponentException
     */
    boolean skipComponentWindow(HstRequestContext requestContext, HstComponentConfiguration compConfig) throws HstComponentException;
    
}
