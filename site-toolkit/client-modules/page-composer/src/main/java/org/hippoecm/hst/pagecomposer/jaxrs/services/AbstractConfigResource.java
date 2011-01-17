/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.hst.pagecomposer.jaxrs.services;

import java.util.List;

import javax.jcr.Node;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.jaxrs.JAXRSService;
import org.hippoecm.hst.jaxrs.util.AnnotatedContentBeanClassesScanner;
import org.hippoecm.hst.pagecomposer.jaxrs.cxf.CXFJaxrsHstConfigService;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ExtResponseRepresentation;
import org.hippoecm.hst.util.ObjectConverterUtils;

/**
 * @version $Id$
 */

public class AbstractConfigResource {
    private ObjectConverter objectConverter;
    private List<Class<? extends HippoBean>> annotatedClasses;
    public static final String BEANS_ANNOTATED_CLASSES_CONF_PARAM = "hst-beans-annotated-classes";


    protected HstRequestContext getRequestContext(HttpServletRequest servletRequest) {
        return (HstRequestContext) servletRequest.getAttribute(ContainerConstants.HST_REQUEST_CONTEXT);
    }

    protected String getRequestConfigIdentifier(HstRequestContext requestContext) {
        return (String) requestContext.getAttribute(CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER);
    }

    protected Node getRequestConfigNode(HstRequestContext requestContext) {
        return (Node) requestContext.getAttribute(JAXRSService.REQUEST_CONTENT_NODE_KEY);
    }

    protected Response ok(String msg) {
        return ok(msg, new String[0]);
    }

    protected Response ok(String msg, Object data) {
        ExtResponseRepresentation entity = new ExtResponseRepresentation(data);
        entity.setMessage(msg);
        entity.setSuccess(true);
        return Response.ok().entity(entity).build();
    }

    protected Response error(String msg) {
        return error(msg, new String[0]);
    }

    protected Response error(String msg, Object data) {
        ExtResponseRepresentation entity = new ExtResponseRepresentation(data);
        entity.setMessage(msg);
        entity.setSuccess(false);
        return Response.serverError().entity(entity).build();
    }

    protected List<Class<? extends HippoBean>> getAnnotatedClasses(HstRequestContext requestContext) {
        if (annotatedClasses == null) {
            String annoClassPathResourcePath = "";

            if (StringUtils.isBlank(annoClassPathResourcePath)) {
                annoClassPathResourcePath = requestContext.getServletContext().getInitParameter(BEANS_ANNOTATED_CLASSES_CONF_PARAM);
            }

            annotatedClasses = AnnotatedContentBeanClassesScanner.scanAnnotatedContentBeanClasses(requestContext, annoClassPathResourcePath);
        }
        return annotatedClasses;
    }


    protected ObjectConverter getObjectConverter(HstRequestContext requestContext) {
        if (objectConverter == null) {
            List<Class<? extends HippoBean>> annotatedClasses = getAnnotatedClasses(requestContext);
            objectConverter = ObjectConverterUtils.createObjectConverter(annotatedClasses);
        }
        return objectConverter;
    }

}
