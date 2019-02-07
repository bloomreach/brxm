/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.configuration;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.configuration.hosting.PortMount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;

public class GenericVirtualHostWrapper implements VirtualHost {

    private VirtualHost delegatee;

    public GenericVirtualHostWrapper(final VirtualHost delegatee) {

        this.delegatee = delegatee;
    }

    @Override
    public String getHostName() {
        return delegatee.getHostName();
    }

    @Override
    public String getName() {
        return delegatee.getName();
    }

    @Override
    public String getHostGroupName() {
        return delegatee.getHostGroupName();
    }

    @Override
    public String getLocale() {
        return delegatee.getLocale();
    }

    @Override
    public VirtualHost getChildHost(final String name) {
        return delegatee.getChildHost(name);
    }

    @Override
    public List<VirtualHost> getChildHosts() {
        return delegatee.getChildHosts();
    }

    @Override
    public PortMount getPortMount(final int portNumber) {
        return delegatee.getPortMount(portNumber);
    }

    @Override
    public VirtualHosts getVirtualHosts() {
        return delegatee.getVirtualHosts();
    }

    @Override
    public boolean isContextPathInUrl() {
        return delegatee.isContextPathInUrl();
    }

    @Override
    public String getContextPath() {
        return delegatee.getContextPath();
    }

    @Override
    public boolean isPortInUrl() {
        return delegatee.isPortInUrl();
    }

    @Override
    public String getScheme() {
        return delegatee.getScheme();
    }

    @Override
    public boolean isSchemeAgnostic() {
        return delegatee.isSchemeAgnostic();
    }

    @Override
    public int getSchemeNotMatchingResponseCode() {
        return delegatee.getSchemeNotMatchingResponseCode();
    }

    @Override
    public String getHomePage() {
        return delegatee.getHomePage();
    }

    @Override
    public String getBaseURL(final HttpServletRequest request) {
        return delegatee.getBaseURL(request);
    }

    @Override
    public String getPageNotFound() {
        return delegatee.getPageNotFound();
    }

    @Override
    public boolean isVersionInPreviewHeader() {
        return delegatee.isVersionInPreviewHeader();
    }

    @Override
    public boolean isCacheable() {
        return delegatee.isCacheable();
    }

    @Override
    public String[] getDefaultResourceBundleIds() {
        return delegatee.getDefaultResourceBundleIds();
    }

    @Override
    public String getCdnHost() {
        return delegatee.getCdnHost();
    }

    @Override
    public boolean isCustomHttpsSupported() {
        return delegatee.isCustomHttpsSupported();
    }

    @Override
    public Map<String, String> getResponseHeaders() {
        return delegatee.getResponseHeaders();
    }
}
