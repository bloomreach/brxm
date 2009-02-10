package org.hippoecm.hst.core.component;

import java.util.Map;

import org.hippoecm.hst.core.request.HstRequestContext;

public class DefaultHstComponentImpl implements HstComponent {
    
    protected HstComponent delegated;
    
    public DefaultHstComponentImpl(HstComponent delegated) {
        this.delegated = delegated;
    }

    public void init(Map<String, Object> properties) {
        this.delegated.init(properties);
    }

    public void destroy() {
        this.delegated.destroy();
    }

    public void doAction(HstRequestContext requestContext, HstRequest request, HstResponse response) {
        this.delegated.doAction(requestContext, request, response);
    }

    public void doBeforeRender(HstRequestContext requestContext, HstRequest request, HstResponse response) {
        this.delegated.doBeforeRender(requestContext, request, response);
    }

    public void doBeforeServeResource(HstRequestContext requestContext, HstRequest request, HstResponse response) {
        this.delegated.doBeforeServeResource(requestContext, request, response);
    }

}
