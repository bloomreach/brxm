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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstResponseState;

public class HstComponentWindowImpl implements HstComponentWindow {
    
    protected String name;
    protected String referenceName;
    protected String referenceNamespace;
    protected HstComponent component;
    protected String renderPath;
    protected String serveResourcePath;
    protected HstComponentWindow parentWindow;
    protected List<HstComponentException> componentExceptions;
    protected Map<String, HstComponentWindow> childWindowMap;
    protected Map<String, HstComponentWindow> childWindowMapByReferenceName;
    
    protected HstResponseState responseState;
    
    public HstComponentWindowImpl(String name, String referenceName, String referenceNamespace, HstComponent component, String renderPath, String serveResourcePath, HstComponentWindow parentWindow) {
        this.name = name;
        this.referenceName = referenceName;
        this.referenceNamespace = referenceNamespace;
        this.component = component;
        this.renderPath = renderPath;
        this.serveResourcePath = serveResourcePath;
        this.parentWindow = parentWindow;
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
        return this.renderPath;
    }
    
    public String getServeResourcePath() {
        return this.serveResourcePath;
    }
    
    public HstComponentWindow getParentWindow() {
        return this.parentWindow;
    }

    public Map<String, HstComponentWindow> getChildWindowMap() {
        return this.childWindowMap;
    }
    
    public HstComponentWindow getChildWindow(String name) {
        HstComponentWindow childWindow = null;
        
        if (this.childWindowMap != null) {
            childWindow = this.childWindowMap.get(name);
        }
        
        return childWindow;
    }
    
    public HstComponentWindow getChildWindowByReferenceName(String referenceName) {
        HstComponentWindow childWindow = null;
        
        if (this.childWindowMapByReferenceName != null) {
            childWindow = this.childWindowMapByReferenceName.get(referenceName);
        }
        
        return childWindow;
    }
    
    public String getName() {
        return this.name;
    }

    public String getReferenceName() {
        return this.referenceName;
    }

    public String getReferenceNamespace() {
        return this.referenceNamespace;
    }
    
    public HstResponseState getResponseState() {
        return this.responseState;
    }
    
    public void addChildWindow(HstComponentWindow child) {
        if (this.childWindowMap == null) {
            this.childWindowMap = new HashMap<String, HstComponentWindow>();
        }
        
        this.childWindowMap.put(child.getName(), child);
        
        if (this.childWindowMapByReferenceName == null) {
            this.childWindowMapByReferenceName = new HashMap<String, HstComponentWindow>();
        }
        
        this.childWindowMapByReferenceName.put(child.getReferenceName(), child);
    }

    protected void setResponseState(HstResponseState responseState) {
        this.responseState = responseState;
    }
    
}
