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

import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstURL;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.request.HstRequestContext;

public class HstRequestContextImpl implements HstRequestContext {

    private Repository repository;
    protected Session session;
    protected Credentials defaultCredentials;
    protected HstSiteMapItem siteMapItem;
    protected HstURLFactory urlFactory;
    protected HstContainerURL baseURL;

    public HstRequestContextImpl(Repository repository) {
        this(repository, null);
    }
    
    public HstRequestContextImpl(Repository repository, Credentials defaultCredentials) {
        this.repository = repository;
        this.defaultCredentials = defaultCredentials;
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
    
    public void setSiteMapItem(HstSiteMapItem siteMapItem) {
        this.siteMapItem = siteMapItem;
    }

    public HstSiteMapItem getSiteMapItem() {
        return this.siteMapItem;
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

    public HstURL createURL(String type, String parameterNamespace) {
        return this.urlFactory.createURL(type, parameterNamespace, this.baseURL);
    }
    
}
