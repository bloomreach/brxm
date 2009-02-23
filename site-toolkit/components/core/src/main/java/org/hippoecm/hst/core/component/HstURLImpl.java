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
package org.hippoecm.hst.core.component;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public class HstURLImpl implements HstURL {
    
    protected String type = TYPE_RENDER;
    protected Map<String, String[]> parameterMap = new HashMap<String, String[]>();
    protected String resourceID;
    protected String basePath;
    protected String baseContext;
    protected HstURLProvider urlProvider;
    
    public HstURLImpl(HstURLProvider urlProvider) {
        this.urlProvider = urlProvider;
    }

    public Map<String, String[]> getParameterMap() {
        return this.parameterMap;
    }

    public String getType() {
        return this.type;
    }

    public void setParameter(String name, String value) {
        setParameter(name, value != null ? new String [] { value } : (String []) null);
    }

    public void setParameter(String name, String[] values) {
        this.parameterMap.put(name, values);
    }

    public void setParameters(Map<String, String[]> parameters) {
        for (Map.Entry<String, String []> entry : parameters.entrySet()) {
            setParameter(entry.getKey(), entry.getValue());
        }
    }

    public void setResourceID(String resourceID) {
        this.resourceID = resourceID;
    }

    public void setType(String type) {
        this.type = type;
    }
    
    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }
    
    public void setBaseContext(String baseContext) {
        this.baseContext = baseContext;
    }

    public void write(Writer out) throws IOException {
        out.write(toString());
    }

    public void write(Writer out, boolean escapeXML) throws IOException {
        write(out);
    }
    
    public String toString() {
        this.urlProvider.setType(this.type);
        this.urlProvider.setParameters(this.parameterMap);
        this.urlProvider.setBasePath(this.basePath);
        this.urlProvider.setBaseContext(this.baseContext);
        return this.urlProvider.toString();
    }

}
