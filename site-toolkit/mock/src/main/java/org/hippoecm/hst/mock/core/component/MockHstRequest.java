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
package org.hippoecm.hst.mock.core.component;

import java.util.HashMap;
import java.util.Map;

import org.hippoecm.hst.core.request.HstRequestContext;

public class MockHstRequest extends MockHstRequestBase {

    private String referencePath;
    private Map<String, Map<String, Object>> attributeMap = new HashMap<String, Map<String, Object>>();
    private Map<String, Map<String, String []>> parameterMap = new HashMap<String, Map<String, String []>>();
    private HstRequestContext requestContext;
    private String resourceId;
    private String referenceNamespace;
    private String lifecyclePhase;

    public void setReferencePath(String referencePath) {
        this.referencePath = referencePath;
    }

    @Override
    public Map<String, Object> getAttributeMap() {
        return getAttributeMap(this.referencePath);
    }
    
    public void setAttributeMap(String referencePath, Map<String, Object> attrMap) {
        this.attributeMap.put(referencePath, attrMap);
    }
    
    public Map<String, Object> getAttributeMap(String referencePath) {
        return this.attributeMap.get(referencePath);
    }

    public void setParameterMap(String referencePath, Map<String, String []> paramMap) {
        this.parameterMap.put(referencePath, paramMap);
    }
    
    public Map<String, String []> getParameterMap(String referencePath) {
        return this.parameterMap.get(referencePath);
    }

    public void setRequestContext(HstRequestContext requestContext) {
        this.requestContext = requestContext;
    }
    
    public HstRequestContext getRequestContext() {
        return this.requestContext;
    }
    
    public void setResourceID(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getResourceID() {
        return this.resourceId;
    }

    public String getReferenceNamespace() {
        return this.referenceNamespace;
    }
    
    public void setReferenceNamespace(String referenceNamespace) {
        this.referenceNamespace =referenceNamespace;
    }

    public String getLifecyclePhase() {
        return this.lifecyclePhase;
    }
    
    public void setLifecyclePhase(String lifecyclePhase) {
        this.lifecyclePhase = lifecyclePhase;
    }

}