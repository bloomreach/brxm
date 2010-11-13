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
package org.hippoecm.hst.demo.wicketexamples;

import java.util.Map;

import org.apache.commons.lang.StringUtils;

public class NodeBean extends ItemBean {
    
    private static final long serialVersionUID = 1L;

    protected String primaryNodeTypeName;
    protected String uuid;
    protected Map<String, Object []> properties;
    
    public NodeBean() {
        super();
    }

    public NodeBean(String name, String path, int depth, boolean ismodified, boolean isnew, boolean isnode, 
            String primaryNodeTypeName, String uuid, Map<String, Object []> properties) {
        
        super(name, path, depth, ismodified, isnew, isnode);
        
        this.primaryNodeTypeName = primaryNodeTypeName;
        this.uuid = uuid;
        this.properties = properties;
        
    }
    
    public String getPrimaryNodeTypeName() {
        return primaryNodeTypeName;
    }
    
    public String getUuid() {
        return uuid;
    }
    
    public Map<String, Object []> getProperties() {
        return properties;
    }
    
    public String getPropertiesAsString() {
        StringBuilder sb = new StringBuilder();
        
        if (properties != null) {
            for (Map.Entry<String, Object []> entry : properties.entrySet()) {
                String name = entry.getKey();
                Object [] props = entry.getValue();
                
                sb.append(name + ": ");
                
                if (props != null && props.length > 0) {
                    sb.append(StringUtils.join(props, ','));
                }
                
                sb.append("\n");
            }
        }
        
        return sb.toString();
    }
    
    public Object [] getProperty(String name) {
        return properties.get(name);
    }
    
}
