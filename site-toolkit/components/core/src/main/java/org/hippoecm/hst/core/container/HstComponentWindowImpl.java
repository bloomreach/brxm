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
package org.hippoecm.hst.core.container;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.collections.map.LinkedMap;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.components.HstComponentInfo;
import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstResponseState;
import org.hippoecm.hst.core.request.ComponentConfiguration;
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
    protected ComponentConfiguration componentConfiguration;
    protected String name;
    protected String referenceName;
    protected String referenceNamespace;
    protected HstComponent component;
    protected String renderPath;
    protected String serveResourcePath;
    protected HstComponentWindow parentWindow;
    
    protected List<HstComponentException> componentExceptions;
    protected LinkedMap childWindowMap;
    protected LinkedMap childWindowMapByReferenceName;
    
    protected Map<String, Object> attributes;
    
    protected HstResponseState responseState;
    
    public HstComponentWindowImpl(HstComponentConfiguration hstComponentConfiguration, HstComponent component, HstComponentWindow parentWindow, String referenceNamespace) {
        this.hstComponentConfiguration = hstComponentConfiguration;
        this.component = component;
        this.parentWindow = parentWindow;
        this.referenceNamespace = referenceNamespace;
    }
    
    public HstComponent getComponent() {
        return this.component;
    }
    
    public boolean hasComponentExceptions() {
        return (this.componentExceptions != null && !this.componentExceptions.isEmpty());
    }
    
    public List<HstComponentException> getComponentExceptions() {
        return this.componentExceptions;
    }
    
    public void addComponentExcpetion(HstComponentException e) {
        if (this.componentExceptions == null) {
            this.componentExceptions = new LinkedList<HstComponentException>();
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
            String temp = hstComponentConfiguration.getRenderPath();
            
            if (temp != null && !temp.startsWith("/")) {
                temp = new StringBuilder(temp.length() + 1).append('/').append(temp).toString();
            }
            
            renderPath = temp;
        }
        
        return renderPath;
    }
    
    public String getServeResourcePath() {
        if (serveResourcePath == null) {
            String temp = hstComponentConfiguration.getServeResourcePath();
            
            if (temp != null && !temp.startsWith("/")) {
                temp = new StringBuilder(temp.length() + 1).append('/').append(temp).toString();
            }
            
            serveResourcePath = temp;
        }
        
        return serveResourcePath;
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
        return this.childWindowMap;
    }
    
    public List<String> getChildWindowNames() {
        if (this.childWindowMap == null) {
            return Collections.emptyList();
        } else {
            return this.childWindowMap.asList();
        }
    }
    
    public HstComponentWindow getChildWindow(String name) {
        HstComponentWindow childWindow = null;
        
        if (this.childWindowMap != null) {
            childWindow = (HstComponentWindow) this.childWindowMap.get(name);
        }
        
        return childWindow;
    }
    
    public HstComponentWindow getChildWindowByReferenceName(String referenceName) {
        HstComponentWindow childWindow = null;
        
        if (this.childWindowMapByReferenceName != null) {
            childWindow = (HstComponentWindow) this.childWindowMapByReferenceName.get(referenceName);
        }
        
        return childWindow;
    }
    
    public String getName() {
        return hstComponentConfiguration.getName();
    }

    public String getReferenceName() {
        return hstComponentConfiguration.getReferenceName();
    }

    public String getReferenceNamespace() {
        return this.referenceNamespace;
    }
    
    public HstResponseState getResponseState() {
        return this.responseState;
    }
    
    public void addChildWindow(HstComponentWindow child) {
        if (this.childWindowMap == null) {
            this.childWindowMap = new LinkedMap();
        }
        
        HstComponentWindow old = (HstComponentWindow) this.childWindowMap.put(child.getName(), child);
        
        if (old != null) {
            log.warn("Ambiguous components configuration because component sibblings found with same name. " +
            		"The first one is replaced. Fix your configuration as this leads to unexpected behavior. Double name: '{}'", child.getName() );
        }
        
        if (this.childWindowMapByReferenceName == null) {
            this.childWindowMapByReferenceName = new LinkedMap();
        }
        
        this.childWindowMapByReferenceName.put(child.getReferenceName(), child);
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
            attributes = new HashMap<String, Object>();
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
            return (Enumeration<String>) IteratorUtils.asEnumeration(attributes.keySet().iterator());
        }
        
        return (Enumeration<String>) IteratorUtils.asEnumeration(Collections.emptySet().iterator());
    }
    
    protected void setResponseState(HstResponseState responseState) {
        this.responseState = responseState;
    }
    
}
