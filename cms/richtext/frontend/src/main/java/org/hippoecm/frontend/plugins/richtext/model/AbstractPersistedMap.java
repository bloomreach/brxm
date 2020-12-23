/*
 *  Copyright 2008-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.richtext.model;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractPersistedMap extends HashMap<String, String> implements IPersisted {

    private Map<String, String> initialValues;

    public AbstractPersistedMap(Map<String, String> values) {
        if (values != null) {
            putAll(values);
        }
        initialValues = values;
    }

    public boolean isValid() {
        return true;
    }

    public boolean hasChanged() {
        return !equals(initialValues);
    }

    public boolean isExisting() {
        return initialValues != null;
    }

    public String toJsString() {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        boolean first = true;
        for (Map.Entry<String, String> e : entrySet()) {
            if (first) {
                first = false;
            } else {
                sb.append(',');
            }
            sb.append(e.getKey()).append(':').append(serializeValue(e.getValue()));
        }
        sb.append('}');
        return sb.toString();
    }

    protected String serializeValue(String value) {
        return JavascriptUtil.serialize2JS(value);
    }

    protected Map<String, String> getInitialValues() {
        return initialValues;
    }

}
