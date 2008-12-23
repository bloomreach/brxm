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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;

public abstract class DialogModel implements IDialogModel {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private Map<String, String> initialValues;
    protected Map<String, String> values;

    private JcrNodeModel initialModel;
    private JcrNodeModel selectedModel;

    public DialogModel(Map<String, String> values, JcrNodeModel parentModel) {
        this.values = values;
        
        initialValues = new HashMap<String, String>();
        for (Entry<String, String> e : values.entrySet()) {
            initialValues.put(e.getKey(), e.getValue());
        }
        initialModel = selectedModel = createInitialModel(parentModel);
    }
    
    protected abstract JcrNodeModel createInitialModel(JcrNodeModel parentModel);

    public boolean isSubmittable() {
        if (selectedModel == null) {
            return false;
        } else if (selectedModel.equals(initialModel)) {
            return valuesChanged();
        }
        return true;
    }
    
    protected boolean valuesChanged() {
        return !values.equals(initialValues);
    }

    public boolean isDetacheable() {
        return initialModel != null;
    }
    
    public boolean isReplacing() {
        if (selectedModel != null && initialModel != null && !selectedModel.equals(initialModel)) {
            return true;
        }
        return false;
    }
    
    public boolean isAttacheable() {
        return initialModel == null || isReplacing();
    }
    
    public JcrNodeModel getNodeModel() {
        return selectedModel;
    }
    
    public void setNodeModel(JcrNodeModel model) {
        this.selectedModel = model;
    }
    
    public JcrNodeModel getInitialModel() {
        return initialModel;
    }

    public Map<String, String> getInitialValues() {
        return initialValues;
    }
    
    public Object getObject() {
        return values;
    }

    public void setObject(Object object) {
        if (object instanceof Map) {
            values = (Map<String, String>) object;
        }
    }

    public IModel getPropertyModel(String key) {
        return new MapModel(key);
    }

    public void reset() {
        for (Entry<String, String> entry : values.entrySet()) {
            entry.setValue("");
        }
        initialModel = selectedModel = null;
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

    private class MapModel implements IModel {
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

    public void detach() {
    }

}
