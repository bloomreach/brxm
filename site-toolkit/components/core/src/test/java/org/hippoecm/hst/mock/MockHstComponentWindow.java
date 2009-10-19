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
package org.hippoecm.hst.mock;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.LinkedMap;
import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstResponseState;
import org.hippoecm.hst.core.container.HstComponentWindow;

public class MockHstComponentWindow implements HstComponentWindow {
    
    protected String name;
    protected String referenceName;
    protected String referenceNamespace;
    protected HstComponent component;
    protected String renderPath;
    protected String serveResourcePath;
    protected HstComponentWindow parentWindow;
    protected List<HstComponentException> componentExceptions = new LinkedList<HstComponentException>();
    protected LinkedMap childWindowMap = new LinkedMap();
    protected LinkedMap childWindowMapByReferenceName = new LinkedMap();
    protected HstResponseState responseState;

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
            return this.childWindowMap.asList();
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

    public boolean hasComponentExceptions() {
        return !componentExceptions.isEmpty();
    }

}
