/*
 *  Copyright 2012 Hippo.
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

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.cxf.message.Exchange;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PersistableAnnotationInvokerPostprocessor
 * <P>
 * This <CODE>InvokerPreprocessor</CODE> implementation removes the persistable session from the requestContext if found.
 * </P>
 * 
 * @version $Id$
 */
public class PersistableAnnotationInvokerPostprocessor implements InvokerPostprocessor {

    private static Logger log = LoggerFactory.getLogger(PersistableAnnotationInvokerPostprocessor.class);

    public Object postprocoess(Exchange exchange, Object request, Object result) {
        Boolean persistableOperation = (Boolean) exchange.remove(PersistableAnnotationInvokerPreprocessor.PERSISTABLE_OPERATION);
        
        if (persistableOperation == null || !persistableOperation.booleanValue()) {
            return result;
        }
        
        Session persistableSession = null;
        Session existingSession = null;
        
        try {
            HstRequestContext requestContext = RequestContextProvider.get();
            existingSession = (Session) exchange.remove(PersistableAnnotationInvokerPreprocessor.EXISTING_SESSION);
            persistableSession = requestContext.getSession(false);
            ((HstMutableRequestContext) requestContext).setSession(existingSession);
        } catch (RepositoryException e) {
            log.warn("Failed to get a persistableSession", e);
        } finally {
            if (persistableSession != null) {
                try {
                    persistableSession.logout();
                } catch (Exception ex) {
                    log.warn("Failed to logout persistableSession", ex);
                }
            }
        }
        
        return result;
    }

}
