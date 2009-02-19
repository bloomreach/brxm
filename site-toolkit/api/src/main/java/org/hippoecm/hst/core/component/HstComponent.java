package org.hippoecm.hst.core.component;

import javax.servlet.ServletConfig;

import org.hippoecm.hst.configuration.components.HstComponentConfigurationBean;

/**
 * A HstComponent can be invoked by a HstComponent container
 * during three different request lifecycle phases: ACTION, RESOURCE and RENDER
 */
public interface HstComponent {
    
    void init(ServletConfig servletConfig, HstComponentConfigurationBean componentConfig) throws HstComponentException;
    
    void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException;
    
    void doAction(HstRequest request, HstResponse response) throws HstComponentException;
    
    void doBeforeServeResource(HstRequest request, HstResponse response) throws HstComponentException;
    
    void destroy() throws HstComponentException;
    
}
