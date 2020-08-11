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

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.message.Exchange;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.annotations.Persistable;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.jcr.LazySession;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.site.HstServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PersistableAnnotationInvokerPreprocessor
 * <P>
 * This <CODE>InvokerPreprocessor</CODE> implementation checks the {@link org.hippoecm.hst.content.annotations.Persistable} annotations of the target operation
 * and sets a persistable JCR session for the current request context if found.
 * </P>
 * 
 * @version $Id$
 */
public class PersistableAnnotationInvokerPreprocessor implements InvokerPreprocessor {

    private static Logger log = LoggerFactory.getLogger(PersistableAnnotationInvokerPreprocessor.class);

    static final String PERSISTABLE_OPERATION = PersistableAnnotationInvokerPreprocessor.class.getName() + ".persistable.operation";
    static final String EXISTING_SESSION = PersistableAnnotationInvokerPreprocessor.class.getName() + ".existing.session";

    public Object preprocoess(Exchange exchange, Object request) {
        if (isPersistableOperation(exchange)) {
            HstRequestContext requestContext = RequestContextProvider.get();
            
            if (requestContext == null) {
                log.warn("No hstRequestContext found in PersistableAnnotationInvokerPreprocessor#preprocoess().");
                return null;
            }
            
            Session persistableSession = null;
            Session existingSession = null;
            
            try {
                existingSession = requestContext.getSession(false);
                
                // if there exists subject based session, do nothing...
                if (existingSession instanceof LazySession) {
                    return null;
                }
                
                if (existingSession != null) {
                    exchange.put(EXISTING_SESSION, existingSession);
                }
                
                Repository repository = HstServices.getComponentManager().getComponent(Repository.class.getName());
                Credentials persistableCredentials = requestContext.getContextCredentialsProvider().getWritableCredentials(requestContext);
                
                persistableSession = repository.login(persistableCredentials);
                
                ((HstMutableRequestContext) requestContext).setSession(persistableSession);

                exchange.put(PERSISTABLE_OPERATION, Boolean.TRUE);
            } catch (RepositoryException e) {
                log.warn("Failed to get a persistableSession", e);
            }
        }
        
        return null;
    }
    
    private boolean isPersistableOperation(Exchange exchange) {
        OperationResourceInfo operationResourceInfo = exchange.get(OperationResourceInfo.class);
        Method method = operationResourceInfo.getMethodToInvoke();
        
        Persistable persistableAnnoOnMethod = method.getAnnotation(Persistable.class);
        
        if (persistableAnnoOnMethod != null) {
            log.debug("The operation is persistable.");
            return true;
        }
        
        return false;
    }

}
