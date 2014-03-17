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
package org.hippoecm.hst.mock.core.request;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;

/**
 * A dummy {@link org.hippoecm.hst.core.request.ResolvedSiteMapItem} for testing purposes.
 */
public class MockResolvedSiteMapItem implements ResolvedSiteMapItem {

    private String relativeContentPath;
    private String pathInfo;
    private String pageTitle;
    private Map<String, String> parameters = new HashMap<String, String>();
    private Map<String, String> localParameters = new HashMap<String, String>();
    private HstSiteMapItem hstSiteMapItem;
    private int statusCode;
    private int errorCode;
    private Set<String> roles;
    private Set<String> users;
    private boolean authenticated;
    private HstComponentConfiguration hstComponentConfiguration;
    private String namedPipeline;
    private ResolvedMount resolvedMount;

    public String getRelativeContentPath() {
        return relativeContentPath;
    }
    
    public void setRelativeContentPath(String relativeContentPath) {
        this.relativeContentPath = relativeContentPath;
    }

    public String getPathInfo() {
        return pathInfo;
    }

    public void setPathInfo(String pathInfo) {
        this.pathInfo = pathInfo;
    }

    public String getPageTitle() {
        return pageTitle;
    }

    public void setPageTitle(final String pageTitle) {
        this.pageTitle = pageTitle;
    }

    public String getParameter(String name) {
        return parameters.get(name);
    }

    public Properties getParameters() {
        Properties props = new Properties();
        
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            props.setProperty(entry.getKey(), entry.getValue());
        }
        
        return props;
    }

    public void addParameter(String key, String value) {
        parameters.put(key, value);
    }

    public void removeParameter(String key) {
        parameters.remove(key);
    }
    
    public String getLocalParameter(String name) {
        return localParameters.get(name);
    }

    public Properties getLocalParameters() {
        Properties props = new Properties();
        
        for (Map.Entry<String, String> entry : localParameters.entrySet()) {
            props.setProperty(entry.getKey(), entry.getValue());
        }
        
        return props;
    }

    public void addLocalParameter(String key, String value) {
        localParameters.put(key, value);
    }

    public void removeLocalParameter(String key) {
        localParameters.remove(key);
    }
    
    public HstSiteMapItem getHstSiteMapItem() {
        return hstSiteMapItem;
    }

    public void setHstSiteMapItem(HstSiteMapItem hstSiteMapItem) {
        this.hstSiteMapItem = hstSiteMapItem;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public Set<String> getUsers() {
        return users;
    }

    public void setUsers(Set<String> users) {
        this.users = users;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public HstComponentConfiguration getHstComponentConfiguration() {
        return hstComponentConfiguration;
    }

    public void setHstComponentConfiguration(HstComponentConfiguration hstComponentConfiguration) {
        this.hstComponentConfiguration = hstComponentConfiguration;
    }

    public String getNamedPipeline() {
        return namedPipeline;
    }

    public void setNamedPipeline(String namedPipeline) {
        this.namedPipeline = namedPipeline;
    }

    public ResolvedMount getResolvedMount() {
        return resolvedMount;
    }

    public void setResolvedMount(ResolvedMount resolvedMount) {
        this.resolvedMount = resolvedMount;
    }
    
}
