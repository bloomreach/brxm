/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.jaxrs.cxf;

import java.lang.reflect.Method;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.impl.SecurityContextImpl;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.MessageContentsList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SecurityAnnotationInvokerPreprocessor
 * <P>
 * This <CODE>InvokerPreprocessor</CODE> implementation checks the security annotations of the target operation
 * and return a 403 Forbidden error message when not authorized for the operation by the annotation.
 * </P>
 * 
 * @version $Id$
 */
public class SecurityAnnotationInvokerPreprocessor implements InvokerPreprocessor {
    
    private static Logger log = LoggerFactory.getLogger(SecurityAnnotationInvokerPreprocessor.class);

    public Object preprocoess(Exchange exchange, Object request) {
        if (isForbiddenOperation(exchange)) {
            return new MessageContentsList(Response.status(Response.Status.FORBIDDEN).build());
        }
        
        return null;
    }
    
    protected boolean isForbiddenOperation(Exchange exchange) {
        SecurityContext securityContext = new SecurityContextImpl(exchange.getInMessage());
        OperationResourceInfo operationResourceInfo = exchange.get(OperationResourceInfo.class);
        Method method = operationResourceInfo.getMethodToInvoke();
        
        DenyAll denyAllAnnoOnMethod = method.getAnnotation(DenyAll.class);
        
        if (denyAllAnnoOnMethod != null) {
            log.debug("The operation is denied to all.");
            return true;
        }
        
        RolesAllowed rolesAllowedAnnoOnMethod = method.getAnnotation(RolesAllowed.class);
        
        if (rolesAllowedAnnoOnMethod != null) {
            String [] roles = rolesAllowedAnnoOnMethod.value();
            
            if (roles != null) {
                for (String role : roles) {
                    if (securityContext.isUserInRole(role)) {
                        log.debug("The user is in role: " + role);
                        return false;
                    }
                }
            }
            
            log.debug("The user is not in any role: " + StringUtils.join(roles, ", "));
            return true;
        }
        
        PermitAll permitAllAnnoOnMethod = method.getAnnotation(PermitAll.class);
        
        if (permitAllAnnoOnMethod != null) {
            log.debug("The operation is permitted to all.");
            return false;
        }
        
        RolesAllowed rolesAllowedAnnoOnType = method.getDeclaringClass().getAnnotation(RolesAllowed.class);
        
        if (rolesAllowedAnnoOnType != null) {
            String [] roles = rolesAllowedAnnoOnType.value();
            
            if (roles != null) {
                for (String role : roles) {
                    if (securityContext.isUserInRole(role)) {
                        log.debug("The user is in role: " + role);
                        return false;
                    }
                }
            }
            
            log.debug("The user is not in any role defined in the type: " + StringUtils.join(roles, ", "));
            return true;
        }
        
        PermitAll permitAllAnnoOnType = method.getDeclaringClass().getAnnotation(PermitAll.class);
        
        if (permitAllAnnoOnType != null) {
            log.debug("The type is permitted to all.");
            return false;
        }
        
        return false;
    }

}
