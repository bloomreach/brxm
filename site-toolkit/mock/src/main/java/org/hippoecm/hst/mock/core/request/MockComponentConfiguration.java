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

import org.hippoecm.hst.configuration.components.HstComponentConfiguration.Type;
import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mock implementation of {@link org.hippoecm.hst.core.request.ComponentConfiguration for testing purposes.
 */
public class MockComponentConfiguration implements ComponentConfiguration {

    private final Map<String, String> rawParameters = new LinkedHashMap<String, String>();

    public String getParameter(String name, ResolvedSiteMapItem hstResolvedSiteMapItem) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public Map<String, String> getParameters(ResolvedSiteMapItem hstResolvedSiteMapItem) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public String getLocalParameter(String name, ResolvedSiteMapItem hstResolvedSiteMapItem) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public Map<String, String> getLocalParameters(ResolvedSiteMapItem hstResolvedSiteMapItem) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public Map<String, String> getRawParameters() {
        return Collections.unmodifiableMap(rawParameters);
    }

    public Map<String, String> getRawLocalParameters() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public String getRenderPath() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public String getServeResourcePath() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public String getCanonicalPath() {
        throw new UnsupportedOperationException("Not implemented");
    }

    // Mock specific methods

    /**
     * Add a raw parameter.
     *
     * @param parameterName  the parameter name
     * @param parameterValue the parameter value
     */
    public void addRawParameter(String parameterName, String parameterValue) {
        this.rawParameters.put(parameterName, parameterValue);
    }

    public String getCanonicalIdentifier() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public Type getComponentType() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public String getXType() {
        throw new UnsupportedOperationException("Not implemented");
    }
}
