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
package org.hippoecm.frontend.template;

import java.util.Map;

import org.hippoecm.frontend.plugin.PluginDescriptor;

public class TemplateDescriptor extends ItemDescriptor {
    private static final long serialVersionUID = 1L;

    private static ThreadLocal<TemplateDescriptor> serializer = new ThreadLocal<TemplateDescriptor>();

    private TypeDescriptor type;

    public TemplateDescriptor(TypeDescriptor type, PluginDescriptor plugin, String mode) {
        super(0, plugin, mode);

        this.type = type;
    }

    public TemplateDescriptor(Map<String, Object> map) {
        super(map);
        this.type = new TypeDescriptor((Map<String, Object>) map.get("typeDescriptor"));
        for (ItemDescriptor item : getItems()) {
            item.setTemplate(this);
        }
    }

    @Override
    public Map<String, Object> getMapRepresentation() {
        TemplateDescriptor current = serializer.get();
        if (!equals(current)) {
            serializer.set(this);
            Map<String, Object> map = super.getMapRepresentation();
            map.put("typeDescriptor", this.type.getMapRepresentation());
            serializer.set(current);
            return map;
        }
        serializer.set(current);
        return null;
    }

    public TypeDescriptor getTypeDescriptor() {
        return type;
    }

    public String getType() {
        return type.getType();
    }
}
