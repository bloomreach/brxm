/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.container;

import org.hippoecm.hst.jaxrs.JAXRSService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JaxrsRestServiceValve
 * 
 * @version $Id$
 */
public class JaxrsRestServiceValve extends AbstractValve {
    
    private static final Logger log = LoggerFactory.getLogger(JaxrsRestServiceValve.class);
    
    private JAXRSService service;
    
    public JaxrsRestServiceValve(JAXRSService service) {
    	this.service = service;
    }
    
    @Override
    public void invoke(ValveContext context) throws ContainerException {
        try {
            service.invoke(context.getRequestContext(), context.getServletRequest(), context.getServletResponse()); 
        }
        catch (ContainerException ce) {
        	throw ce;
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.error("Failed to invoke jaxrs service.", e);
            } else {
                log.error("Failed to invoke jaxrs service. {}", e.toString());
            }
            throw new ContainerException(e);
        }
    }

    @Override
    public void destroy() {
        service.destroy();
    }
}
