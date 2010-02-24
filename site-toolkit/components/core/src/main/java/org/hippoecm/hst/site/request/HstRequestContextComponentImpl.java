/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.hst.site.request;

import javax.jcr.Repository;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.container.ContainerConfiguration;
import org.hippoecm.hst.core.request.ContextCredentialsProvider;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.HstRequestContextComponent;

/**
 * HstRequestContextComponentImpl
 * 
 * @version $Id$
 */
public class HstRequestContextComponentImpl implements HstRequestContextComponent {

    protected Repository repository;
    protected ContextCredentialsProvider contextCredentialsProvider;

    public HstRequestContextComponentImpl(Repository repository, ContextCredentialsProvider contextCredentialsProvider) {
        this.repository = repository;
        this.contextCredentialsProvider = contextCredentialsProvider;
    }

    public HstRequestContext create(HttpServletRequest req, HttpServletResponse resp, ContainerConfiguration config) {
        HstRequestContextImpl rc = null;
        
        if (req.getAttribute("javax.portlet.request") != null) {
            // portlet invoked request context
            HstPortletRequestContextImpl prc = new HstPortletRequestContextImpl(repository, contextCredentialsProvider);
            prc.initPortletContext(req, resp);
            rc = prc;
        } else {
            // servlet invoked request context
            rc = new HstRequestContextImpl(repository, contextCredentialsProvider);
        }
            
        rc.setContainerConfiguration(config);
        
        return rc;
    }
    
    public void release(HstRequestContext context) {
    }
}
