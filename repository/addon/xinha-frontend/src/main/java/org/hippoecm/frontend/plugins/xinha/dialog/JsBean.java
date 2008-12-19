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

package org.hippoecm.frontend.plugins.xinha.dialog;

import java.util.Map;
import java.util.Map.Entry;

import org.apache.wicket.IClusterable;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;

public class JsBean implements IClusterable {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    protected Map<String, String> values;
    private JcrNodeModel nodeModel;

    public JsBean(Map<String, String> values) {
        this.values = values;
    }

    public JcrNodeModel getNodeModel() {
        return nodeModel;
    }

    public void setNodeModel(JcrNodeModel nodeModel) {
        this.nodeModel = nodeModel;
    }
    
    public void reset() {
        for(Entry<String, String> entry : values.entrySet()) {
            entry.setValue("");
        }
        nodeModel = null;
    }

    public String toJsString() {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        boolean first = true;
        for (Entry<String, String> e : values.entrySet()) {
            if (first) {
                first = false;
            } else {
                sb.append(',');
            }
            String value = e.getValue();
            sb.append(e.getKey()).append(':').append(JavascriptUtil.serialize2JS(value));
        }
        sb.append('}');
        return sb.toString();
    }

    public IModel getPropertyModel(String key) {
        return new MapModel(key);
    }

    class MapModel implements IModel {
        private static final long serialVersionUID = 1L;

        private String key;

        public MapModel(String key) {
            this.key = key;
        }

        public Object getObject() {
            if (values != null) {
                return values.get(key);
            }
            return null;
        }

        public void setObject(Object object) {
            if (values != null) {
                values.put(key, (String) object);
            }
        }

        public void detach() {
        }

    }

}
