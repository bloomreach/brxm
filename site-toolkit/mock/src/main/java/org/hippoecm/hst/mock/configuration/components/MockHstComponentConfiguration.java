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
package org.hippoecm.hst.mock.configuration.components;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;


/**
 * Mock implementation of {@link org.hippoecm.hst.configuration.components.HstComponentConfiguration}.
 *
 */
public class MockHstComponentConfiguration implements HstComponentConfiguration {

    private String id;
    private SortedMap<String, HstComponentConfiguration> componentConfigs =
            new TreeMap<String, HstComponentConfiguration>();
    private Map<String,String> parameters = new HashMap<String,String>();

    public MockHstComponentConfiguration(String id) {
        this.id = id;
    }

    public HstComponentConfiguration getChildByName(String arg0) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public String getCanonicalStoredLocation() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public SortedMap<String, HstComponentConfiguration> getChildren() {
        return componentConfigs;
    }
    
    public MockHstComponentConfiguration addChild(MockHstComponentConfiguration config){
        componentConfigs.put(config.getId(), config);
        return config;
    }
    
    public void addChildren(MockHstComponentConfiguration... config){
        for (MockHstComponentConfiguration mockHstComponentConfiguration : config) {
            addChild(mockHstComponentConfiguration);
        }
    }

    public String getLocalParameter(String arg0) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public Map<String, String> getLocalParameters() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public String getParameter(String name) {
        return parameters.get(name);
    }

    public void setParameter(String name, String value) {
        parameters.put(name,value);
    }

    public Map<String, String> getParameters() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public HstComponentConfiguration getParent() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public String getReferenceName() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public String getRenderPath() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public String getServeResourcePath() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public String getComponentClassName() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public String getId() {
        return id;
    }
    
    public void setId(String id){
        this.id=id;
    }

    public String getName() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public String getCanonicalIdentifier() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public Type getComponentType() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public String getNamedRenderer() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public String getNamedResourceServer() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public String getPageErrorHandlerClassName() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public String getXType() {
        throw new UnsupportedOperationException("Not implemented");
    }
    
}
