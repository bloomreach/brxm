/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.cmsprototype.frontend.plugins.template;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.wicket.IClusterable;

public class TemplateDescriptor implements IClusterable {

    private static final long serialVersionUID = 1L;

    private String name;
    private Map<String,FieldDescriptor> fields;

    public TemplateDescriptor(String name, List<FieldDescriptor> fields) {
        this.name = name;
        this.fields = new HashMap<String,FieldDescriptor>(fields.size());
        for(FieldDescriptor desc : fields) {
            this.fields.put(desc.getName(), desc);
        }
    }

    public String getName() {
        return name;
    }
    
    public Iterator<FieldDescriptor> getFieldIterator() {
        return fields.values().iterator();
    }
    
    public boolean hasField(String name) {
        return fields.containsKey(name);
    }
}
