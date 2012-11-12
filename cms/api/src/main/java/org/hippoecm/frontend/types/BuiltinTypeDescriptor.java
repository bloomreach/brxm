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

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.ItemDefinition;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.wicket.model.IDetachable;
import org.hippoecm.frontend.model.ocm.StoreException;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BuiltinTypeDescriptor extends JavaTypeDescriptor implements IDetachable {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(BuiltinTypeDescriptor.class);

    private String type;
    private transient boolean loaded = false;
    private transient Map<String, IFieldDescriptor> fields;
    private transient Map<String, IFieldDescriptor> declaredFields;
    private transient NodeType nt = null;

    public BuiltinTypeDescriptor(String type, ITypeLocator locator) throws StoreException {
        super(type, type, locator);

        this.type = type;

        // set properties of super class
        if (type.indexOf(':') < 0) {
            setIsNode(false);
        } else {
            try {
                Session session = ((UserSession) org.apache.wicket.Session.get()).getJcrSession();
                NodeTypeManager ntMgr = session.getWorkspace().getNodeTypeManager();
                ntMgr.getNodeType(type);
            } catch (NoSuchNodeTypeException ex) {
                throw new StoreException("Type does not exist");
            } catch (RepositoryException ex) {
                // ignore; will be triggered by load
            }
        }

        load();
        if (nt != null) {
            NodeType[] supers = nt.getDeclaredSupertypes();
            List<String> superTypes = new LinkedList<String>();
            for (NodeType superType : supers) {
                if (!"nt:base".equals(superType.getName())) {
                    superTypes.add(superType.getName());
                }
            }
            setSuperTypes(superTypes);
            setIsMixin(nt.isMixin());
        }
        setMutable(false);
    }

    boolean isValid() {
        load();
        return (!isNode() || nt != null);
    }

    void load() {
        if (!loaded && isNode()) {
            fields = new LinkedHashMap<String, IFieldDescriptor>();
            declaredFields = new LinkedHashMap<String, IFieldDescriptor>();
            try {
                Session session = ((UserSession) org.apache.wicket.Session.get()).getJcrSession();
                NodeTypeManager ntMgr = session.getWorkspace().getNodeTypeManager();
                nt = ntMgr.getNodeType(type);

                if (nt != null) {
                    String prefix = nt.getName().substring(0, nt.getName().indexOf(':'));
                    NodeDefinition[] childNodes = nt.getChildNodeDefinitions();
                    for (NodeDefinition definition : childNodes) {
                        addDefinition(prefix, definition);
                    }
                    PropertyDefinition[] properties = nt.getPropertyDefinitions();
                    for (PropertyDefinition definition : properties) {
                        addDefinition(prefix, definition);
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
            } catch (RepositoryException ex) {
                log.error(ex.getMessage(), ex);
            } catch (StoreException e) {
                log.error("Failed to load type descriptor", e);
            }
            loaded = true;
        }
    }
    
    protected void addDefinition(String prefix, ItemDefinition definition) throws StoreException {
        BuiltinFieldDescriptor field = new BuiltinFieldDescriptor(prefix, definition, locator, this);
        if (definition.getDeclaringNodeType().equals(nt)) {
            declaredFields.put(field.getName(), field);
        }
        if ("nt:base".equals(definition.getDeclaringNodeType().getName())) {
            return;
        }
        fields.put(field.getName(), field);
        String primaryItemName = definition.getDeclaringNodeType().getPrimaryItemName();
        if (primaryItemName != null && primaryItemName.equals(definition.getName())) {
            field.setPrimary(true);
        }
    }

    @Override
    public boolean isType(String typeName) {
        load();
        if (nt != null) {
            return nt.isNodeType(typeName);
        } else if (type.equals(typeName)) {
            return true;
        }
        return false;
    }
    
    @Override
    public Map<String, IFieldDescriptor> getDeclaredFields() {
        load();
        if (declaredFields != null) {
            return Collections.unmodifiableMap(declaredFields);
        } else {
            return Collections.emptyMap();
        }
    }
    
    @Override
    public Map<String, IFieldDescriptor> getFields() {
        load();
        if (fields != null) {
            return Collections.unmodifiableMap(fields);
        } else {
            return Collections.emptyMap();
        }
    }

    @Override
    public void detach() {
        nt = null;
        fields = null;
        declaredFields = null;
        super.detach();
    }

}
