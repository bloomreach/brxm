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
package org.hippoecm.hst.service.jcr;

import java.io.Serializable;
import java.util.Map;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.service.Service;
import org.hippoecm.hst.service.UnderlyingServiceAware;


@Node(jcrType="hippostd:document")
public class HippoStdImpl implements UnderlyingServiceAware, Serializable {
    
    private static final long serialVersionUID = 1L;

    private Service service;
    private String state;
    private Map<String, Object> properties;

    public Service getUnderlyingService() {
        return service;
    }

    public void setUnderlyingService(Service service) {
        this.service = service;
    }

    public String getState() {
        return state;
    }
    
    public Map<String, Object> getProperties() {
        if (properties == null) {
            properties = service.getValueProvider().getProperties();
        }
        
        return properties;
    }
    
    public <T> T getProperty(String name) {
        return (T) getProperties().get(name);
    }
    
}
