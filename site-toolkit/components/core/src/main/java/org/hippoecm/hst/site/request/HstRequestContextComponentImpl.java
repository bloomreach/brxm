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
package org.hippoecm.hst.site.request;

import javax.jcr.Repository;

import org.hippoecm.hst.core.container.ContainerConfiguration;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.internal.HstRequestContextComponent;
import org.hippoecm.hst.core.request.ContextCredentialsProvider;
import org.hippoecm.hst.core.request.HstRequestContext;

/**
 * HstRequestContextComponentImpl
 * 
 * @version $Id$
 */
public class HstRequestContextComponentImpl implements HstRequestContextComponent {

    protected Repository repository;
    protected ContextCredentialsProvider contextCredentialsProvider;
    protected ContainerConfiguration config;

    public HstRequestContextComponentImpl(Repository repository, ContextCredentialsProvider contextCredentialsProvider, ContainerConfiguration config) {
        this.repository = repository;
        this.contextCredentialsProvider = contextCredentialsProvider;
        this.config = config;
    }

    public HstMutableRequestContext create(boolean portletContext) {
    	HstMutableRequestContext rc = null;
    	if (portletContext) {
        	rc = new HstPortletRequestContextImpl(repository, contextCredentialsProvider);
    	}
    	else {
        	rc = new HstRequestContextImpl(repository, contextCredentialsProvider);
    	}
    	rc.setContainerConfiguration(config);
    	return rc;
    }
    
    public void release(HstRequestContext context) {
    }
}
