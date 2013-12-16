/*
 * Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.tag;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.container.HstFilter;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManager;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.util.HstRequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base tag support class with HST functionalities
 */
public class HstTagSupport extends TagSupport {

    private static final long serialVersionUID = 1L;
    
    protected static final Logger logger = LoggerFactory.getLogger(HstTagSupport.class);
    
    @Override
    public int doEndTag() throws JspException {
        try {
            final HttpServletRequest servletRequest = (HttpServletRequest) pageContext.getRequest();
            final HttpServletResponse servletResponse = (HttpServletResponse) pageContext.getResponse();
            final HstRequest hstRequest = HstRequestUtils.getHstRequest(servletRequest);
            final HstResponse hstResponse = HstRequestUtils.getHstResponse(servletRequest, servletResponse);
            
            if (hstRequest == null || hstResponse == null) {
                return EVAL_PAGE;
            }
            return doEndTag(hstRequest, hstResponse);
        } finally {
            cleanup();
        }
    }

    /**
     * Subclasses can override cleanup to set their local instance variables to default value during this method invocation.
     * 
     * This {@link #cleanup()} is called right before the {@link #doEndTag()} returns
     */
    protected void cleanup() {
        // nothing to clean up here. Method for subclasses to override
    }

    /**
     * A doEndTag hook for derived classes with HstRequest and HstResponse 
     * parameters that are never null. 
     */
    protected int doEndTag(final HstRequest hstRequest, final HstResponse hstResponse) {
        return EVAL_PAGE;
    }


    /**
     * Get the {@link Mount} for the current 
     */
    protected Mount getMount(final HstRequest request){
        return request.getRequestContext().getResolvedMount().getMount();
    }
    
    /**
     * Get the HST Site object from request.
     */
    protected HstSite getHstSite(final HstRequest request){
        return request.getRequestContext().getResolvedSiteMapItem().getHstSiteMapItem().getHstSiteMap().getSite();
    }
    
    /**
     * Is this a request in preview?
     */
    protected boolean isPreview(final HstRequest request) {
        return request.getRequestContext().isPreview();
    }
    
    /**
     * Get the default Spring configured client component manager.
     * @deprecated since 2.28.00 client component manager should not be used any more. Instead use the core
     * {@link org.hippoecm.hst.site.HstServices#getComponentManager()}
     */
    @Deprecated
    protected ComponentManager getDefaultClientComponentManager() {
        logger.info("Do not use clientComponentManager any more but core HstServices#getComponentManager()");
        ComponentManager clientComponentManager = HstFilter.getClientComponentManager(pageContext.getServletContext());
        if(clientComponentManager == null) {
            logger.warn("Cannot get a client component manager (although deprecated) from servlet context for attribute name '{}'", HstFilter.CLIENT_COMPONENT_MANANGER_DEFAULT_CONTEXT_ATTRIBUTE_NAME);
        }
        return clientComponentManager;
    }

    /**
     * Get the site content base bean, which is the root document bean whithin 
     * preview or live context. 
     */
    protected HippoBean getSiteContentBaseBean(HstRequest request) {
        return request.getRequestContext().getSiteContentBaseBean();
    }
    
    protected String getSiteContentBasePath(HstRequest request){
        return request.getRequestContext().getSiteContentBasePath();
    }
    
    protected ObjectBeanManager getObjectBeanManager(HstRequest request) {
        return request.getRequestContext().getObjectBeanManager();
    }
    
    protected ObjectConverter getObjectConverter()  {
        return RequestContextProvider.get().getContentBeansTool().getObjectConverter();
    }
}
