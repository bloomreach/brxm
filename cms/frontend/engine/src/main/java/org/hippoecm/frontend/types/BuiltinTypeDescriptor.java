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
package org.hippoecm.frontend.types;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;

import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class BuiltinTypeDescriptor extends JavaTypeDescriptor {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(BuiltinTypeDescriptor.class);

    private String type;
    private transient NodeType nt = null;

    BuiltinTypeDescriptor(String type) {
        super(type, type);

        this.type = type;
        if (type.indexOf(':') < 0) {
            setIsNode(false);
        }

        load();
        if (nt != null) {
            NodeType[] supers = nt.getSupertypes();
            List<String> superTypes = new LinkedList<String>();
            for (NodeType superType : supers) {
                superTypes.add(superType.getName());
            }
            setSuperTypes(superTypes);
        }
    }

    boolean isValid() {
        load();
        return (!isNode() || nt != null);
    }

    void load() {
        if (nt == null && isNode()) {
            try {
                Session session = ((UserSession) org.apache.wicket.Session.get()).getJcrSession();
                NodeTypeManager ntMgr = session.getWorkspace().getNodeTypeManager();
                nt = ntMgr.getNodeType(type);
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        }
    }

    @Override
    public Map<String, IFieldDescriptor> getFields() {
        load();
        Map<String, IFieldDescriptor> fields = new LinkedHashMap<String, IFieldDescriptor>();
        if (nt != null) {
            String prefix = nt.getName().substring(0, nt.getName().indexOf(':'));
            NodeDefinition[] childNodes = nt.getChildNodeDefinitions();
            for (NodeDefinition definition : childNodes) {
                fields.put(definition.getName(), new BuiltinFieldDescriptor(prefix, definition));
            }
            PropertyDefinition[] properties = nt.getPropertyDefinitions();
            for (PropertyDefinition definition : properties) {
                if (!definition.getDeclaringNodeType().getName().equals("nt:base")) {
                    fields.put(definition.getName(), new BuiltinFieldDescriptor(prefix, definition));
                }
            }
        }
        Set<String> explicit = new HashSet<String>();
        for (IFieldDescriptor field : fields.values()) {
            if (!field.getPath().equals("*")) {
                explicit.add(field.getPath());
            }
        }
        for (IFieldDescriptor field : fields.values()) {
            if (field.getPath().equals("*")) {
                field.setExcluded(explicit);
            }
        }

        return fields;
    }
}