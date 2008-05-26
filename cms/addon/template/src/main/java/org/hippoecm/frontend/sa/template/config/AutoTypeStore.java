/*
 * Copyright 2008 Hippo
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
package org.hippoecm.frontend.sa.template.config;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.ItemDefinition;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;

import org.hippoecm.frontend.sa.template.FieldDescriptor;
import org.hippoecm.frontend.sa.template.ITypeStore;
import org.hippoecm.frontend.sa.template.TypeDescriptor;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoTypeStore implements ITypeStore {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(AutoTypeStore.class);

    public TypeDescriptor getTypeDescriptor(String type) {
        JcrTypeDescriptor result = new JcrTypeDescriptor(type);
        if (result.isValid()) {
            return result;
        }
        return null;
    }

    public List<TypeDescriptor> getTypes(String namespace) {
        // TODO Auto-generated method stub
        return null;
    }

    class JcrTypeDescriptor extends TypeDescriptor {
        private static final long serialVersionUID = 1L;

        private String type;
        private transient NodeType nt = null;

        JcrTypeDescriptor(String type) {
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
        public Map<String, FieldDescriptor> getFields() {
            load();
            Map<String, FieldDescriptor> fields = new LinkedHashMap<String, FieldDescriptor>();
            if (nt != null) {
                NodeDefinition[] childNodes = nt.getChildNodeDefinitions();
                for (NodeDefinition definition : childNodes) {
                    fields.put(definition.getName(), new JcrFieldDescriptor(definition.getName(), definition));
                }
                PropertyDefinition[] properties = nt.getPropertyDefinitions();
                for (PropertyDefinition definition : properties) {
                    fields.put(definition.getName(), new JcrFieldDescriptor(definition.getName(), definition));
                }
            }
            Set<String> explicit = new HashSet<String>();
            for (FieldDescriptor field : fields.values()) {
                if (!field.getPath().equals("*")) {
                    explicit.add(field.getPath());
                }
            }
            for (FieldDescriptor field : fields.values()) {
                if (field.getPath().equals("*")) {
                    field.setExcluded(explicit);
                }
            }

            return fields;
        }
    }

    class JcrFieldDescriptor extends FieldDescriptor {
        private static final long serialVersionUID = 1L;

        JcrFieldDescriptor(String path, ItemDefinition definition) {
            super(path);

            if (definition instanceof NodeDefinition) {
                NodeDefinition ntDef = (NodeDefinition) definition;
                setIsMultiple(ntDef.allowsSameNameSiblings());
                setMandatory(ntDef.isMandatory());

                // set referenced type
                NodeType[] types = ntDef.getRequiredPrimaryTypes();
                if (types.length > 1) {
                    log.warn("multiple primary types specified; this is not supported.  Only the first one is used.");
                }
                setType(types[0].getName());
            } else {
                PropertyDefinition propDef = (PropertyDefinition) definition;
                setIsMultiple(propDef.isMultiple());
                setMandatory(propDef.isMandatory());

                setType(PropertyType.nameFromValue(propDef.getRequiredType()));
            }

            setMandatory(definition.isMandatory());

            if (isMultiple()) {
                if (definition.getDeclaringNodeType().hasOrderableChildNodes()) {
                    setIsOrdered(true);
                }
            }
        }
    }

}
