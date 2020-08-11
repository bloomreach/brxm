/*
 *  Copyright 2008-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.container;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections4.IteratorUtils;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.components.HstComponentInfo;
import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstComponentMetadata;
import org.hippoecm.hst.core.component.HstResponseState;
import org.hippoecm.hst.core.component.HstServletResponseState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HstComponentWindowImpl
 * 
 * @version $Id$
 */
public class HstComponentWindowImpl implements HstComponentWindow {

    protected final static Logger log = LoggerFactory.getLogger(HstComponentWindowImpl.class);
    
    protected HstComponentConfiguration hstComponentConfiguration;
    protected String referenceNamespace;
    protected HstComponent component;
    protected HstComponentMetadata componentMetadata;
    protected String componentName;
    protected String parametersInfoClassName;
    protected String renderPath;
    protected String namedRenderer;
    protected String serveResourcePath;
    protected String namedResourceServer;
    protected HstComponentWindow parentWindow;
    
    protected List<HstComponentException> componentExceptions;
    protected LinkedHashMap<String, HstComponentWindow> childWindowMap;
    protected LinkedHashMap<String, HstComponentWindow> childWindowMapByReferenceName;
    
    protected Map<String, Object> attributes;
    protected boolean visible = true;
    
    protected HstResponseState responseState;
    
    public HstComponentWindowImpl(final HstComponentConfiguration hstComponentConfiguration,
                                  final String componentName,
                                  final HstComponent component,
                                  final HstComponentMetadata componentMetadata,
                                  final HstComponentWindow parentWindow,
                                  final String referenceNamespace,
                                  final String parametersInfoClassName) {
        this.hstComponentConfiguration = hstComponentConfiguration;
        this.componentName = componentName;
        this.component = component;
        this.componentMetadata = componentMetadata;
        this.parentWindow = parentWindow;
        this.referenceNamespace = referenceNamespace;
        this.parametersInfoClassName = parametersInfoClassName;
    }
    
    public HstComponent getComponent() {
        return this.component;
    }
    
    public HstComponentMetadata getComponentMetadata() {
        return componentMetadata;
    }
    
    public String getComponentName() {
        return componentName;
    }

    public String getParametersInfoClassName() {
        return parametersInfoClassName;
    }

    public boolean hasComponentExceptions() {
        return (this.componentExceptions != null && !this.componentExceptions.isEmpty());
    }
    
    public List<HstComponentException> getComponentExceptions() {
        return this.componentExceptions;
    }
    
    public void addComponentExcpetion(HstComponentException e) {
        if (this.componentExceptions == null) {
            this.componentExceptions = new LinkedList<>();
        }
        
        this.componentExceptions.add(e);
    }
    
    public void clearComponentExceptions() {
        if (this.componentExceptions != null) {
            this.componentExceptions.clear();
        }
    }
    
    public String getRenderPath() {
        if (renderPath == null) {
            renderPath = hstComponentConfiguration.getRenderPath();
        }
        
        return renderPath;
    }
    
    public String getNamedRenderer() {
        if (namedRenderer == null) {
            namedRenderer = hstComponentConfiguration.getNamedRenderer();
        }
        
        return namedRenderer;
    }
    
    public String getServeResourcePath() {
        if (serveResourcePath == null) {
            serveResourcePath = hstComponentConfiguration.getServeResourcePath();
        }
        return serveResourcePath;
    }
    
    public String getNamedResourceServer() {
        if (namedResourceServer == null) {
            namedResourceServer = hstComponentConfiguration.getNamedResourceServer();
        }
        return namedResourceServer;
    }

    public String getParameter(String paramName) {
        return hstComponentConfiguration.getParameter(paramName);
    }
    
    public String getLocalParameter(String paramName) {
        return hstComponentConfiguration.getLocalParameter(paramName);
    }
    
    public HstComponentWindow getParentWindow() {
        return this.parentWindow;
    }

    public Map<String, HstComponentWindow> getChildWindowMap() {
        if (childWindowMap == null) {
            return Collections.emptyMap();
        }
        return childWindowMap;
    }
    
    public List<String> getChildWindowNames() {
        if (childWindowMap == null) {
            return Collections.emptyList();
        } else {
           return Collections.unmodifiableList(new ArrayList<>(childWindowMap.keySet()));
        }
    }
    
    public HstComponentWindow getChildWindow(String name) {
        HstComponentWindow childWindow = null;
        
        if (childWindowMap != null) {
            childWindow = childWindowMap.get(name);
        }
        
        return childWindow;
    }
    
    public HstComponentWindow getChildWindowByReferenceName(String referenceName) {
        HstComponentWindow childWindow = null;
        
        if (this.childWindowMapByReferenceName != null) {
            childWindow = childWindowMapByReferenceName.get(referenceName);
        }
        
        return childWindow;
    }
    
    public String getName() {
        return hstComponentConfiguration.getName();
    }

    public String getReferenceName() {
        return hstComponentConfiguration.getReferenceName();
    }
    

    public String getPageErrorHandlerClassName() {
        return hstComponentConfiguration.getPageErrorHandlerClassName();
    }
    

    public String getReferenceNamespace() {
        return referenceNamespace;
    }

    @Override
    public void bindResponseState(final HttpServletRequest request, final HttpServletResponse parentResponse) {
        if (responseState != null) {
            log.warn("Window '{}' already has a responseState. Return", getName());
            return;
        }
        responseState = new HstServletResponseState(request, parentResponse, this);
    }

    public HstResponseState getResponseState() {
        return responseState;
    }
    
    public void addChildWindow(HstComponentWindow child) {
        if (childWindowMap == null) {
            childWindowMap = new LinkedHashMap<>();
        }
        
        HstComponentWindow old = childWindowMap.put(child.getName(), child);
        
        if (old != null) {
            log.error("Ambiguous components configuration because component sibblings found with same name: '{}'. " +
            		"The first one is replaced. This should not be possible. You can report the HST2 team so they can fix this.", child.getName() );
        }
        
        if (childWindowMapByReferenceName == null) {
            childWindowMapByReferenceName = new LinkedHashMap<>();
        }
        
        childWindowMapByReferenceName.put(child.getReferenceName(), child);
    }
    
    public HstComponentInfo getComponentInfo() {
        return hstComponentConfiguration;
    }
    
    public Object getAttribute(String name) {
        if (attributes != null) {
            return attributes.get(name);
        }
        
        return null;
    }
    
    public void setAttribute(String name, Object value) {
        if (attributes == null) {
            attributes = new HashMap<>();
        }
        
        attributes.put(name, value);
    }
    
    public Object removeAttribute(String name) {
        if (attributes != null) {
            return attributes.remove(name);
        }
        
        return null;
    }
    
    public Enumeration<String> getAttributeNames() {
        if (attributes != null) {
            return IteratorUtils.asEnumeration(attributes.keySet().iterator());
        }
        
        return IteratorUtils.asEnumeration(Collections.emptyIterator());
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public void setVisible(boolean visible) {
       this.visible = visible;
    }

    @Override
    public void removeChildWindow(final HstComponentWindow window) {
        if (window == null) {
            throw new IllegalArgumentException("'window' argument not allowed to be null");
        }
        if (childWindowMap == null) {
            log.debug("Cannot remove '{}' since no child windows present for '{}'", window.getName(), getName());
            return;
        }

        HstComponentWindow removed = childWindowMap.remove(window.getName());
        if (removed == null) {
            log.debug("could not remove '{}' from '{}' because not a child.", window.getName(), getName());
        } else {
            log.debug("Removed '{}' from '{}'.", window.getName(), getName());
            if (childWindowMapByReferenceName != null) {
                childWindowMapByReferenceName.remove(window.getReferenceName());
            }
        }
    }

}
