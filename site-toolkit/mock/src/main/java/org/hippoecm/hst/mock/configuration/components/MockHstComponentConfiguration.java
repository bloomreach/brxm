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
package org.hippoecm.hst.mock.configuration.components;

import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.hippoecm.hst.configuration.internal.ConfigurationLockInfo;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;


/**
 * Mock implementation of {@link org.hippoecm.hst.configuration.components.HstComponentConfiguration}.
 *
 */
public class MockHstComponentConfiguration implements HstComponentConfiguration, ConfigurationLockInfo {

    private String id;
    private String name;
    private SortedMap<String, HstComponentConfiguration> componentConfigs =
            new TreeMap<String, HstComponentConfiguration>();
    private Map<String,String> parameters = new HashMap<String,String>();
    private Map<String,String> localParameters = new HashMap<String,String>();
    private String canonicalStoredLocation;
    private HstComponentConfiguration parent;
    private String referenceName;
    private String renderPath;
    private String serveResourcePath;
    private String componentClassName;
    private String canonicalIdentifier;
    private Type componentType;
    private String namedRenderer;
    private String namedResourceServer;
    private String pageErrorHandlerClassName;
    private String xType;
    private String label;
    private String iconPath;
    private boolean inherited;
    private boolean prototype;
    private boolean standalone;
    private boolean async;
    private String asyncMode;
    private boolean compositeCacheable;
    private List<String> variants;
    private List<String> mountVariants;
    private String lockedBy;
    private Calendar lockedOn;
    private Calendar lastModified;

    public MockHstComponentConfiguration(String id) {
        this.id = id;
    }

    public HstComponentConfiguration getChildByName(String name) {
        return componentConfigs.get(name);
    }

    public SortedMap<String, HstComponentConfiguration> getChildren() {
        return componentConfigs;
    }
    
    public HstComponentConfiguration addChild(HstComponentConfiguration config){
        componentConfigs.put(config.getId(), config);
        return config;
    }
    
    public void addChildren(MockHstComponentConfiguration ... config){
        for (MockHstComponentConfiguration mockHstComponentConfiguration : config) {
            addChild(mockHstComponentConfiguration);
        }
    }

    public String getCanonicalStoredLocation() {
        return canonicalStoredLocation;
    }
    
    public void setCanonicalStoredLocation(String canonicalStoredLocation) {
        this.canonicalStoredLocation = canonicalStoredLocation;
    }

    public String getLocalParameter(String name) {
        return localParameters.get(name);
    }
    
    public void setLocalParameter(String name, String value) {
        localParameters.put(name, value);
    }

    public Map<String, String> getLocalParameters() {
        return localParameters;
    }

    public String getParameter(String name) {
        return parameters.get(name);
    }
    
    @Override
    public Set<String> getParameterPrefixes() {
        return Collections.emptySet();
    }

    public void setParameter(String name, String value) {
        parameters.put(name,value);
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public HstComponentConfiguration getParent() {
        return parent;
    }
    
    public void setParent(HstComponentConfiguration parent) {
        this.parent = parent;
    }

    public String getReferenceName() {
        return referenceName;
    }
    
    public void setReferenceName(String referenceName) {
        this.referenceName = referenceName;
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

    public String getComponentClassName() {
        return componentClassName;
    }
    
    public void setComponentClassName(String componentClassName) {
        this.componentClassName = componentClassName;
    }

    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
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

    @Override
    public String getComponentFilterTag() {
        return null;
    }

    public void setComponentType(Type componentType) {
        this.componentType = componentType;
    }

    public String getNamedRenderer() {
        return namedRenderer;
    }
    
    public void setNamedRenderer(String namedRenderer) {
        this.namedRenderer = namedRenderer;
    }

    public String getNamedResourceServer() {
        return namedResourceServer;
    }

    public void setNamedResourceServer(String namedResourceServer) {
        this.namedResourceServer = namedResourceServer;
    }

    public String getPageErrorHandlerClassName() {
        return pageErrorHandlerClassName;
    }

    public void setPageErrorHandlerClassName(String pageErrorHandlerClassName) {
        this.pageErrorHandlerClassName = pageErrorHandlerClassName;
    }

    public String getXType() {
        return xType;
    }
    
    public void setXType(String xType) {
        this.xType = xType;
    }

    @Override
    public boolean isInherited() {
        return inherited;
    }

    @Override
    public boolean isPrototype() {
        return prototype;
    }
    
    public void setInherited(boolean inherited) {
        this.inherited = inherited;
    }

    @Override
    public boolean isStandalone() {
        return standalone;

    }

    public void setStandalone(boolean standalone) {
        this.standalone = standalone;
    }

    @Override
    public boolean isAsync() {
        return async;
    }

    public void setAsync(boolean async) {
        this.async = async;
    }

    @Override
    public String getAsyncMode() {
        return asyncMode;
    }

    public void setAsyncMode(String asyncMode) {
        this.asyncMode = asyncMode;
    }

    @Override
    public boolean isCompositeCacheable() {
        return compositeCacheable;
    }

    public void setCompositeCacheable(boolean compositeCacheable) {
        this.compositeCacheable = compositeCacheable;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getIconPath() {
        return iconPath;
    }
    public void setIconPath(String iconPath) {
        this.iconPath = iconPath;
    }

    public void setVariants(List<String> variants) {
        this.variants = variants;
    }

    @Override
    public List<String> getVariants() {
        return variants;
    }

    public void setMountVariants(List<String> mountVariants) {
        this.mountVariants = mountVariants;
    }

    @Override
    public List<String> getMountVariants() {
        return mountVariants;
    }

    @Override
    public String getLockedBy() {
        return lockedBy;
    }

    public void setLockedBy(final String lockedBy) {
        this.lockedBy = lockedBy;
    }

    @Override
    public Calendar getLockedOn() {
        return lockedOn;
    }

    public void setLockedOn(final Calendar lockedOn) {
        this.lockedOn = lockedOn;
    }

    @Override
    public Calendar getLastModified() {
        return lastModified;
    }

    public void setLastModified(final Calendar lastModified) {
        this.lastModified = lastModified;
    }
}
