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
package org.hippoecm.hst.configuration.components;


/**
 * DelegatingHstComponentInfo
 * 
 * @version $Id$
 */
public class DelegatingHstComponentInfo implements HstComponentInfo {
    
    private HstComponentInfo delegatee;
    private String componentName;

    /**
     * HST Component Info constructor with delegatee and component name arguments.
     * Normally, the component name can be full qualified class name which also can be retrieved from HstComponentWindow.
     * @param delegatee
     * @param componentName
     */
    public DelegatingHstComponentInfo(HstComponentInfo delegatee, String componentName) {
        this.delegatee = delegatee;
        this.componentName = componentName;
    }
    
    public String getComponentClassName() {
        return (componentName != null ? componentName : delegatee.getComponentClassName());
    }

    public String getId() {
        return delegatee.getId();
    }

    public String getName() {
        return delegatee.getName();
    }

    @Override
    public boolean isStandalone() {
        return delegatee.isStandalone();
    }

    @Override
    public boolean isAsync() {
        return delegatee.isAsync();
    }

    @Override
    public boolean isCompositeCacheable() {
        return delegatee.isCompositeCacheable();
    }
}
