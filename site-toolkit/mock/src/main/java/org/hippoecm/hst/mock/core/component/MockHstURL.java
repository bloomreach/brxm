/**
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.mock.core.component;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.hippoecm.hst.core.component.HstURL;

/**
 * MockHstURL
 */
public class MockHstURL implements HstURL {

    private static final String [] EMPTY_STRING_ARRAY = new String[0];

    private String type = RENDER_TYPE;
    private String referenceNamespace;
    private Map<String, String []> parameters;
    private String resourceID;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        if (!ACTION_TYPE.equals(type) && !RENDER_TYPE.equals(type) && !COMPONENT_RENDERING_TYPE.equals(type) && !RESOURCE_TYPE.equals(type)) {
            throw new IllegalArgumentException("Unknown url type: " + type);
        }

        this.type = type;
    }

    public String getReferenceNamespace() {
        return referenceNamespace;
    }

    public void setReferenceNamespace(String referenceNamespace) {
        this.referenceNamespace = referenceNamespace;
    }

    public void setParameter(String key, String value) {
        if (value == null) {
            setParameter(key, EMPTY_STRING_ARRAY);
        } else {
            setParameter(key, new String [] { value });
        }
    }

    public void setParameter(String key, String[] values) {
        if (this.parameters == null) {
            this.parameters = new HashMap<String, String []>();
        }
        
        if (values == null) {
            this.parameters.remove(key);
        } else {
            this.parameters.put(key, values);
        }
    }

    public void setParameters(Map<String, String[]> parameters) {
        for (Map.Entry<String, String []> entry : parameters.entrySet()) {
            setParameter(entry.getKey(), entry.getValue());
        }
    }

    public void setParameter(Map<String, String[]> parameters) {
        if (parameters == null) {
            this.parameters = null;
        } else {
            if (this.parameters == null) {
                this.parameters = new HashMap<String, String []>();
            } else {
                this.parameters.clear();
            }
        
            for (Map.Entry<String, String []> entry : parameters.entrySet()) {
                setParameter(entry.getKey(), entry.getValue());
            }
        }
    }

    public Map<String, String[]> getParameterMap() {
        return parameters;
    }

    public void write(Writer out) throws IOException {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    public void write(Writer out, boolean escapeXML) throws IOException {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    public void setResourceID(String resourceID) {
        this.resourceID = resourceID;
    }

    public String getResourceID() {
        return resourceID;
    }

}
