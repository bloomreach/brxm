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
package org.hippoecm.hst.core.container;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.hippoecm.hst.configuration.components.HstComponentInfo;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.util.KeyValue;

/**
 * DefaultPageErrors
 * 
 * @version $Id$
 */
public class DefaultPageErrors implements PageErrors {
    
    protected List<KeyValue<HstComponentInfo, Collection<HstComponentException>>> componentExceptions;
    protected List<HstComponentInfo> componentInfos;
    protected List<HstComponentException> allExceptions;
    
    public DefaultPageErrors(List<KeyValue<HstComponentInfo, Collection<HstComponentException>>> componentExceptions) {
        this.componentExceptions = componentExceptions;
    }
    
    public boolean isEmpty() {
        return (componentExceptions == null || componentExceptions.isEmpty());
    }
    
    public Collection<HstComponentInfo> getComponentInfos() {
        if (componentInfos == null) {
            componentInfos = new ArrayList<HstComponentInfo>();
            
            for (KeyValue<HstComponentInfo, Collection<HstComponentException>> pair : componentExceptions) {
                componentInfos.add(pair.getKey());
            }
        }
        
        return componentInfos;
    }

    public Collection<HstComponentException> getComponentExceptions(HstComponentInfo componentInfo) {
        for (KeyValue<HstComponentInfo, Collection<HstComponentException>> pair : componentExceptions) {
            HstComponentInfo componentInfoKey = pair.getKey();
            
            if (componentInfoKey.getId().equals(componentInfo.getId())) {
                return pair.getValue();
            }
        }
        
        return Collections.emptyList();
    }
    
    public Collection<HstComponentException> getAllComponentExceptions() {
        if (allExceptions == null) {
            allExceptions = new ArrayList<HstComponentException>();
            
            for (KeyValue<HstComponentInfo, Collection<HstComponentException>> pair : componentExceptions) {
                allExceptions.addAll(pair.getValue());
            }
        }
        
        return allExceptions;
    }
    
}
