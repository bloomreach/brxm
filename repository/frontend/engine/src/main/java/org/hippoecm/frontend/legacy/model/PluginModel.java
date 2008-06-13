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
package org.hippoecm.frontend.legacy.model;

import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.model.IModel;

public class PluginModel extends HashMap<String, Object> implements IModel, IPluginModel {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    public PluginModel() {
    }

    // implement IPluginModel

    public Map<String, Object> getMapRepresentation() {
        return this;
    }

    // implement IModel

    public Object getObject() {
        return this;
    }

    public void setObject(Object object) {
        if (object instanceof Map) {
            putAll((Map<String, Object>) object);
        } else {
            throw new IllegalArgumentException("PluginModel's object should be of type Map<String, Object>");
        }
    }

    // implement IDetachable

    public void detach() {
    }
}
