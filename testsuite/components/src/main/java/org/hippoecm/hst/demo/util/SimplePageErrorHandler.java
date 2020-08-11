/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.demo.util;

import java.io.IOException;

import org.hippoecm.hst.configuration.components.HstComponentInfo;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.container.PageErrorHandler;
import org.hippoecm.hst.core.container.PageErrors;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.util.HstResponseUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SimpleLoggingPageErrorHandler
 * 
 * @version $Id$
 */
public class SimplePageErrorHandler implements PageErrorHandler {

    public static final String PAGE_ERRORS = SimplePageErrorHandler.class.getName() + ".pageErrors";
    public static final String ERROR_PAGE = SimplePageErrorHandler.class.getName() + ".errorPage";
    public static final String REDIRECT_TO_ERROR_PAGE = SimplePageErrorHandler.class.getName() + ".redirectToErrorPage";
    
    protected final static Logger log = LoggerFactory.getLogger(SimplePageErrorHandler.class);
    
    public Status handleComponentExceptions(PageErrors pageErrors, HstRequest hstRequest, HstResponse hstResponse) {
        logWarningsForEachComponentExceptions(pageErrors);
        
        HstRequestContext requestContext = hstRequest.getRequestContext();
        ResolvedSiteMapItem resolvedSiteMapItem = requestContext.getResolvedSiteMapItem();
        String errorPage = resolvedSiteMapItem.getHstComponentConfiguration().getParameter(ERROR_PAGE);
        boolean redirectToErrorPage = Boolean.parseBoolean(resolvedSiteMapItem.getHstComponentConfiguration().getParameter(REDIRECT_TO_ERROR_PAGE));
        
        if (errorPage != null) {
            try {
                if (redirectToErrorPage) {
                    HstResponseUtils.sendRedirect(hstRequest, hstResponse, errorPage);
                } else {
                    requestContext.setAttribute(PAGE_ERRORS, pageErrors);
                    hstResponse.forward(errorPage);
                }
                
                return Status.HANDLED_TO_STOP;
            } catch (IOException e) {
                if (log.isDebugEnabled()) {
                    log.warn("Failed to forward page: " + errorPage, e);
                } else if (log.isWarnEnabled()) {
                    log.warn("Failed to forward page: {}. {}", errorPage, e.toString());
                }
            }
        }
        
        return Status.HANDLED_BUT_CONTINUE;
    }
    
    protected void logWarningsForEachComponentExceptions(PageErrors pageErrors) {
        for (HstComponentInfo componentInfo : pageErrors.getComponentInfos()) {
            for (HstComponentException componentException : pageErrors.getComponentExceptions(componentInfo)) {
                if (log.isDebugEnabled()) {
                    log.warn("Component exception found on " + componentInfo.getComponentClassName(), componentException);
                } else if (log.isWarnEnabled()) {
                    log.warn("Component exception found on {}. ", componentInfo.getComponentClassName(), componentException.toString());
                }
            }
        }
    }
    
}
