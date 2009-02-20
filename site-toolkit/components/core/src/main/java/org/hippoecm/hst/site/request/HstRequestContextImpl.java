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

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.HstMatchedSiteMapItem;
import org.hippoecm.hst.core.request.HstRequestContext;

public class HstRequestContextImpl implements HstRequestContext {

    protected Repository repository;
    protected Session session;
    protected Credentials defaultCredentials;
    protected HstMatchedSiteMapItem matchedSiteMapItem;
    protected HstURLFactory urlFactory;
    protected HstContainerURL baseURL;
    protected String contextNamespace;
    protected HstLinkCreator linkCreator;
        

    public HstRequestContextImpl(Repository repository) {
        this(repository, null);
    }
    
    public HstRequestContextImpl(Repository repository, Credentials defaultCredentials) {
        this.repository = repository;
        this.defaultCredentials = defaultCredentials;
    }
    
    public void setContextNamespace(String contextNamespace) {
        this.contextNamespace = contextNamespace;
    }
    
    public String getContextNamespace() {
        return this.contextNamespace;
    }
    
    public Session getSession() throws LoginException, RepositoryException {
        if (this.session == null) {
            this.session = this.repository.login(this.defaultCredentials);
        } else if (!this.session.isLive()) {
            throw new HstComponentException("Invalid session.");
        }
        
        return this.session;
    }

    public Credentials getDefaultCredentials() {
        return this.defaultCredentials;
    }
    
    public void setMatchedSiteMapItem(HstMatchedSiteMapItem matchedSiteMapItem) {
        this.matchedSiteMapItem = matchedSiteMapItem;
    }

    public HstMatchedSiteMapItem getMatchedSiteMapItem() {
        return this.matchedSiteMapItem;
    }
    
    public void setBaseURL(HstContainerURL baseURL) {
        this.baseURL = baseURL;
    }
    
    public HstContainerURL getBaseURL() {
        return this.baseURL;
    }
    
    public void setURLFactory(HstURLFactory urlFactory) {
        this.urlFactory = urlFactory;
    }
    
    public HstURLFactory getURLFactory() {
        return this.urlFactory;
    }

    public void setLinkCreator(HstLinkCreator linkCreator) {
        this.linkCreator = linkCreator;
    }
    
    public HstLinkCreator getHstLinkCreator() {
        return this.linkCreator;
    }
    
}
