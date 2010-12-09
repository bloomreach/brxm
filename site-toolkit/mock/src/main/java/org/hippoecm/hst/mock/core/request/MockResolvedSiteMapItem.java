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

    private final Map<String, String> parameters = new HashMap<String, String>();

    public String getRelativeContentPath() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public String getPathInfo() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public String getParameter(String name) {
        return parameters.get(name);
    }

    public Properties getParameters() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public HstSiteMapItem getHstSiteMapItem() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public int getStatusCode() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public int getErrorCode() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public Set<String> getRoles() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public boolean isSecured() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public HstComponentConfiguration getHstComponentConfiguration() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public HstComponentConfiguration getPortletHstComponentConfiguration() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public String getLocalParameter(String arg0) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public Properties getLocalParameters() {
        throw new UnsupportedOperationException("Not implemented");
    }

    // Mock methods

    public void addParameter(String key, String value) {
        parameters.put(key, value);
    }

    public String getNamedPipeline() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public ResolvedMount getResolvedMount() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public Set<String> getUsers() {
        throw new UnsupportedOperationException("Not implemented");
    }
}
