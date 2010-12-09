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
package org.hippoecm.hst.mock.core.container;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hippoecm.hst.configuration.components.HstComponentInfo;
import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstResponseState;
import org.hippoecm.hst.core.container.HstComponentWindow;
import org.hippoecm.hst.mock.util.IteratorEnumeration;

public class MockHstComponentWindow implements HstComponentWindow {
    
    protected String name;
    protected String referenceName;
    protected String referenceNamespace;
    protected HstComponent component;
    protected String renderPath;
    protected String namedRenderer;
    protected String serveResourcePath;
    protected String namedResourceServer;
    protected String pageErrorHandlerClassName;
    protected Map<String, String> parameters;
    protected Map<String, String> localParameters;
    protected HstComponentWindow parentWindow;
    protected List<HstComponentException> componentExceptions = new LinkedList<HstComponentException>();
    protected LinkedHashMap<String, HstComponentWindow> childWindowMap = new LinkedHashMap<String, HstComponentWindow>();
    protected LinkedHashMap<String, HstComponentWindow> childWindowMapByReferenceName = new LinkedHashMap<String, HstComponentWindow>();
    protected HstResponseState responseState;
    protected HstComponentInfo componentInfo;
    protected Map<String, Object> attributes;

    public void addComponentExcpetion(HstComponentException e) {
        componentExceptions.add(e);
    }

    public void clearComponentExceptions() {
        componentExceptions.clear();
    }

    public HstComponentWindow getChildWindow(String name) {
        return (HstComponentWindow) childWindowMap.get(name);
    }

    public HstComponentWindow getChildWindowByReferenceName(String referenceName) {
        return (HstComponentWindow)childWindowMapByReferenceName.get(referenceName);
    }

    public Map<String, HstComponentWindow> getChildWindowMap() {
        return childWindowMap;
    }
    
    public List<String> getChildWindowNames() {
        if (this.childWindowMap == null) {
            return Collections.emptyList();
        } else {
            List<String> keyList = new ArrayList<String>();
            for (String key : childWindowMap.keySet()) {
                keyList.add(key);
            }
            return keyList;
        }
    }
    
    public HstComponent getComponent() {
        return component;
    }
    
    public void setCompnoent(HstComponent component) {
        this.component = component;
    }

    public List<HstComponentException> getComponentExceptions() {
        return componentExceptions;
    }

    public String getName() {
        return name;
    }

    public HstComponentWindow getParentWindow() {
        return parentWindow;
    }
    
    public void setParentWindow(HstComponentWindow parentWindow) {
        this.parentWindow = parentWindow;
    }

    public String getReferenceName() {
        return referenceName;
    }
    
    public void setReferenceName(String referenceName) {
        this.referenceName = referenceName;
    }

    public String getReferenceNamespace() {
        return referenceNamespace;
    }
    
    public void setReferenceNamespace(String referenceNamespace) {
        this.referenceNamespace = referenceNamespace;
    }

    public String getRenderPath() {
        return renderPath;
    }
    
    public void setRenderPath(String renderPath) {
        this.renderPath = renderPath;
    }
    
    public String getNamedRenderer() {
        return namedRenderer;
    }
    
    public void setNamedRenderer(String namedRenderer) {
        this.namedRenderer = namedRenderer;
    }

    public HstResponseState getResponseState() {
        return responseState;
    }
    
    public void setResponseState(HstResponseState responseState) {
        this.responseState = responseState;
    }

    public String getServeResourcePath() {
        return serveResourcePath;
    }
    
    public void setServeResourcePath(String serveResourcePath) {
        this.serveResourcePath = serveResourcePath;
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
    
    public String getParameter(String name) {
        if (parameters == null)
            return null;
        
        return parameters.get(name);
    }

    public void setParameter(String name, String value) {
        if (parameters == null)
            parameters = new HashMap<String, String>();
        
        parameters.put(name, value);
    }
    
    public String getLocalParameter(String name) {
        if (localParameters == null)
            return null;
        
        return localParameters.get(name);
    }

    public void setLocalParameter(String name, String value) {
        if (localParameters == null)
            localParameters = new HashMap<String, String>();
        
        localParameters.put(name, value);
    }
    
    public boolean hasComponentExceptions() {
        return !componentExceptions.isEmpty();
    }

    public HstComponentInfo getComponentInfo() {
        return componentInfo;
    }

    public void setComponentInfo(HstComponentInfo componentInfo) {
        this.componentInfo = componentInfo;
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
            return new IteratorEnumeration<String>(attributes.keySet().iterator());
        }
        
        Set<String> emptySet = Collections.emptySet();
        return new IteratorEnumeration<String>(emptySet.iterator());
    }
}
