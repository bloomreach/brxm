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
package org.hippoecm.hst.hippo.ocm;

import java.util.Map;

import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node;
import org.hippoecm.hst.jackrabbit.ocm.HippoStdDocument;
import org.hippoecm.hst.service.Service;
import org.hippoecm.hst.service.UnderlyingServiceAware;

@Node(jcrType="testproject:textpage", discriminator=false)
public class SimpleTextPage1 extends HippoStdDocument implements UnderlyingServiceAware {

    // the following two properties might be defined in the base class for convenience.
    protected Service underlyingService;
    private Map<String, Object> properties;

    protected String title;

    public String getTitle() {
        return getProperty("testproject:title");
    }
    
    public void setTitle(String title) {
        
    }

    public Service getUnderlyingService() {
        return underlyingService;
    }

    public void setUnderlyingService(Service underlyingService) {
        this.underlyingService = underlyingService;        
    }
    
    public Map<String, Object> getProperties() {
        if (properties == null) {
            properties = underlyingService.getValueProvider().getProperties();
        }
        
        return properties;
    }
    
    public <T> T getProperty(String name) {
        return (T) getProperties().get(name);
    }
    
}
