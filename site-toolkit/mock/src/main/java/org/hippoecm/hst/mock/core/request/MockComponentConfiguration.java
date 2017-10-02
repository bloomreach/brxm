/*
 *  Copyright 2008-2017 Hippo B.V. (http://www.onehippo.com)
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration.Type;
import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;

/**
 * Mock implementation of {@link org.hippoecm.hst.core.request.ComponentConfiguration for testing purposes.
 */
public class MockComponentConfiguration implements ComponentConfiguration {

    private Map<String, String> parameters = new LinkedHashMap<String, String>();
    private Map<String, String> localParameters = new LinkedHashMap<String, String>();
    private Map<String, String> rawParameters = new LinkedHashMap<String, String>();
    private Map<String, String> rawLocalParameters = new LinkedHashMap<String, String>();
    private String renderPath;
    private String serveResourcePath;
    private String canonicalPath;
    private String canonicalIdentifier;
    private Type componentType;
    private String xType;
    private String parametersInfoClassName;

    public String getParameter(String name, ResolvedSiteMapItem hstResolvedSiteMapItem) {
        return parameters.get(name);
    }

    public Map<String, String> getParameters(ResolvedSiteMapItem hstResolvedSiteMapItem) {
        return Collections.unmodifiableMap(parameters);
    }

    public void addParameter(String name, String value) {
        parameters.put(name, value);
    }
    
    public void removeParameter(String name) {
        parameters.remove(name);
    }
    
    public String getLocalParameter(String name, ResolvedSiteMapItem hstResolvedSiteMapItem) {
        return localParameters.get(name);
    }

    public Map<String, String> getLocalParameters(ResolvedSiteMapItem hstResolvedSiteMapItem) {
        return Collections.unmodifiableMap(localParameters);
    }

    public void addLocalParameter(String name, String value) {
        localParameters.put(name, value);
    }
    
    public void removeLocalParameter(String name) {
        localParameters.remove(name);
    }
    
    public Map<String, String> getRawParameters() {
        return Collections.unmodifiableMap(rawParameters);
    }
    
    public void addRawParameter(String name, String value) {
        rawParameters.put(name, value);
    }
    
    public void removeRawParameter(String name) {
        rawParameters.remove(name);
    }
    
    public Map<String, String> getRawLocalParameters() {
        return Collections.unmodifiableMap(rawLocalParameters);
    }
    
    public void addRawLocalParameter(String name, String value) {
        rawLocalParameters.put(name, value);
    }
    
    public void removeRawLocalParameter(String name) {
        rawLocalParameters.remove(name);
    }

    public String getRenderPath() {
        return renderPath;
    }

    public void setRenderPath(String renderPath) {
        this.renderPath = renderPath;
    }

    public String getServeResourcePath() {
        return serveResourcePath;
    }

    public void setServeResourcePath(String serveResourcePath) {
        this.serveResourcePath = serveResourcePath;
    }

    public String getCanonicalPath() {
        return canonicalPath;
    }

    public void setCanonicalPath(String canonicalPath) {
        this.canonicalPath = canonicalPath;
    }

    public String getCanonicalIdentifier() {
        return canonicalIdentifier;
    }

    public void setCanonicalIdentifier(String canonicalIdentifier) {
        this.canonicalIdentifier = canonicalIdentifier;
    }

    public Type getComponentType() {
        return componentType;
    }

    public void setComponentType(Type componentType) {
        this.componentType = componentType;
    }

    public String getXType() {
        return xType;
    }

    public void setXType(String xType) {
        this.xType = xType;
    }

    @Override
    public List<String> getParameterNames() {
        return new ArrayList<String>(parameters.values());
    }

    @Override
    public String getParametersInfoClassName() {
        return parametersInfoClassName;
    }

    public void setParametersInfoClassName(String parametersInfoClassName) {
        this.parametersInfoClassName = parametersInfoClassName;
    }
}
