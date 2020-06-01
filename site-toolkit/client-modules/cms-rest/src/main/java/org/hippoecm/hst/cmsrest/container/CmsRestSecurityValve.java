/*
 *  Copyright 2012-2016 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.cmsrest.container;

import java.io.IOException;

import javax.jcr.Repository;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.container.valves.AbstractOrderableValve;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.ValveContext;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedVirtualHost;
import org.onehippo.cms7.services.cmscontext.CmsSessionContext;
import org.onehippo.cms7.services.cmscontext.CmsContextService;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CmsRestSecurityValve responsible for authenticating CMS REST calls and sets the correct session on the request context
 */
public class CmsRestSecurityValve extends AbstractOrderableValve {

    private final static Logger log = LoggerFactory.getLogger(CmsRestSecurityValve.class);

    private static final String HEADER_CMS_CONTEXT_SERVICE_ID = "X-CMS-CS-ID";
    private static final String HEADER_CMS_SESSION_CONTEXT_ID = "X-CMS-SC-ID";
    private static final String CMSREST_CMSHOST_HEADER = "X-CMSREST-CMSHOST";

    public static final String HOST_GROUP_NAME_FOR_CMS_HOST = "HOST_GROUP_NAME_FOR_CMS_HOST";

    private Repository repository;

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    @Override
    public void invoke(ValveContext context) throws ContainerException {
        HttpServletRequest servletRequest = context.getServletRequest();
        HttpServletResponse servletResponse = context.getServletResponse();
        HstRequestContext requestContext = context.getRequestContext();

        if(!requestContext.isCmsRequest()) {
            setResponseError(HttpServletResponse.SC_BAD_REQUEST, servletResponse, "Bad CMS REST call");
            return;
        }

        log.debug("Request '{}' is invoked from CMS context. Check for credentials and apply security rules or raise proper error!", servletRequest.getRequestURL());

        String cmsContextServiceId = servletRequest.getHeader(HEADER_CMS_CONTEXT_SERVICE_ID);
        if (StringUtils.isBlank(cmsContextServiceId)) {
            log.warn("Cannot proceed _cmsrest request: header '"+ HEADER_CMS_CONTEXT_SERVICE_ID +"' missing");
            setResponseError(HttpServletResponse.SC_BAD_REQUEST, servletResponse, "Request header '"+ HEADER_CMS_CONTEXT_SERVICE_ID +"' missing");
            return;
        }

        String cmsSessionContextId = servletRequest.getHeader(HEADER_CMS_SESSION_CONTEXT_ID);
        if (StringUtils.isBlank(cmsSessionContextId)) {
            log.warn("Cannot proceed _cmsrest request: header '"+ HEADER_CMS_SESSION_CONTEXT_ID +"' missing");
            setResponseError(HttpServletResponse.SC_BAD_REQUEST, servletResponse, "Request header '"+ HEADER_CMS_SESSION_CONTEXT_ID +"' missing");
            return;
        }

        final String cmsHost = servletRequest.getHeader(CMSREST_CMSHOST_HEADER);
        if (StringUtils.isEmpty(cmsHost)) {
            log.warn("Cannot proceed _cmsrest request: header'"+CMSREST_CMSHOST_HEADER+"' missing");
            setResponseError(HttpServletResponse.SC_BAD_REQUEST, servletResponse, "Request header '"+CMSREST_CMSHOST_HEADER+"' missing");
            return;
        }

        CmsContextService cmsContextService = HippoServiceRegistry.getService(CmsContextService.class);
        if (cmsContextService == null) {
            log.error("No CmsContextService available");
            setResponseError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, servletResponse);
            return;
        }

        if (!cmsContextServiceId.equals(cmsContextService.getId())) {
            log.warn("Cannot proceed _cmsrest request: not coming from this CMS HOST");
            setResponseError(HttpServletResponse.SC_BAD_REQUEST, servletResponse, "Cannot proceed _cmsrest request: not coming from this CMS HOST");
            return;
        }

        CmsSessionContext cmsSessionContext = cmsContextService.getSessionContext(cmsSessionContextId);
        if (cmsSessionContext == null) {
            log.warn("Cannot proceed _cmsrest request: CmsSessionContext not found");
            setResponseError(HttpServletResponse.SC_BAD_REQUEST, servletResponse, "Cannot proceed _cmsrest request: CmsSessionContext not found");
            return;
        }

        Session cmsSession = null;
        try {
            cmsSession = repository.login(cmsSessionContext.getRepositoryCredentials());
            ((HstMutableRequestContext) requestContext).setSession(cmsSession);

            final ResolvedVirtualHost resolvedVirtualHost = requestContext.getVirtualHost().getVirtualHosts().matchVirtualHost(cmsHost);
            if (resolvedVirtualHost == null) {
                log.error("Cannot match cmsHost '{}' to a host. Make sure '{}' is configured on a hst:virtualhostgroup node " +
                        "that belong to the correct environment for the cmsHost", cmsHost, cmsHost);
                setResponseError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, servletResponse);
                return;
            }
            final String hostGroupNameForCmsHost = resolvedVirtualHost.getVirtualHost().getHostGroupName();
            requestContext.setAttribute(HOST_GROUP_NAME_FOR_CMS_HOST, hostGroupNameForCmsHost);
            context.invokeNext();
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("{} while processing CMS REST credentials :", e.getClass().getSimpleName(), e);
            } else {
                log.warn("{} while processing CMS REST credentials : {}", e.getClass().getSimpleName(), e.toString());
            }
            setResponseError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, servletResponse);
            return;
        } finally {
            if (cmsSession != null) {
                cmsSession.logout();
            }
        }
    }

    protected void setResponseError(int scError, HttpServletResponse response) {
        setResponseError(scError, response, null);
    }

    protected void setResponseError(int scError, HttpServletResponse response, String message) {
        try {
            if (StringUtils.isBlank(message)) {
                response.sendError(scError);
            } else {
                response.sendError(scError, message);
            }
        } catch (IOException ioe) {
            log.warn("Exception while sending HTTP error response", ioe);
        }
    }

}
