/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.configuration.components.HstComponentInfo;
import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstComponentMetadata;
import org.hippoecm.hst.core.component.HstResponseState;
import org.hippoecm.hst.core.component.HstServletResponseState;
import org.hippoecm.hst.core.container.HstComponentWindow;
import org.hippoecm.hst.mock.util.IteratorEnumeration;

public class MockHstComponentWindow implements HstComponentWindow {
    
    private String name;
    private String referenceName;
    private String referenceNamespace;
    private HstComponent component;
    private String parametersInfoClassName;
    private HstComponentMetadata componentMetadata;
    private String renderPath;
    private String namedRenderer;
    private String serveResourcePath;
    private String namedResourceServer;
    private String pageErrorHandlerClassName;
    private Map<String, String> parameters;
    private Map<String, String> localParameters;
    private HstComponentWindow parentWindow;
    private List<HstComponentException> componentExceptions = new LinkedList<HstComponentException>();
    private LinkedHashMap<String, HstComponentWindow> childWindowMap = new LinkedHashMap<String, HstComponentWindow>();
    private LinkedHashMap<String, HstComponentWindow> childWindowMapByReferenceName = new LinkedHashMap<String, HstComponentWindow>();
    private HstResponseState responseState;
    private HstComponentInfo componentInfo;
    private Map<String, Object> attributes;
    boolean visible = true;
    private boolean containerWindow;
    private boolean containerItemWindow;

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
    
    public void setComponent(HstComponent component) {
        this.component = component;
    }

    public HstComponentMetadata getComponentMetadata() {
        return componentMetadata;
    }

    public void setComponentMetadata(HstComponentMetadata componentMetadata) {
        this.componentMetadata = componentMetadata;
    }

    public String getComponentName() {
        return component.getClass().getName();
    }

    public String getParametersInfoClassName() {
        return parametersInfoClassName;
    }

    public void setParametersInfoClassName(String parametersInfoClassName) {
        this.parametersInfoClassName = parametersInfoClassName;
    }

    public List<HstComponentException> getComponentExceptions() {
        return componentExceptions;
    }

    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
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

    public void setResponseState(final HstResponseState responseState) {
        this.responseState = responseState;
    }

    @Override
    public void bindResponseState(final HttpServletRequest request, final HttpServletResponse parentResponse) {
        if (responseState != null) {
            return;
        }
        responseState = new HstServletResponseState(request, parentResponse, this);
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
        throw new UnsupportedOperationException("Not supported");
    }

}
