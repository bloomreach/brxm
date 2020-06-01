/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.wicket.model.IDetachable;
import org.hippoecm.frontend.model.event.EventCollection;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservationContext;
import org.hippoecm.frontend.model.ocm.StoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaTypeDescriptor implements ITypeDescriptor, IDetachable {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(JavaTypeDescriptor.class);

    ITypeLocator locator;

    private String name;
    private String type;
    private List<String> superTypes;
    private transient Map<String, IFieldDescriptor> fields = null;
    private Map<String, IFieldDescriptor> declaredFields;
    private JavaFieldDescriptor primary;
    private boolean node;
    private boolean mixin;
    private boolean cascadeValidation;

    private IObservationContext obContext;
    private boolean mutable = true;

    public JavaTypeDescriptor(String name, String type, ITypeLocator locator) {
        this.name = name;
        this.type = type;
        this.superTypes = new LinkedList<String>();
        this.declaredFields = new LinkedHashMap<String, IFieldDescriptor>();
        this.primary = null;
        this.node = true;
        this.mixin = false;
        this.cascadeValidation = true;
        this.locator = locator;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public List<String> getSuperTypes() {
        return superTypes;
    }

    public List<ITypeDescriptor> getSubTypes() {
        try {
            return locator.getSubTypes(type);
        } catch (StoreException e) {
            log.error(e.getMessage());
        }
        return Collections.emptyList();
    }

    public void setSuperTypes(List<String> superTypes) {
        checkMutable();
        this.superTypes = superTypes;
        fields = null;
    }

    public Map<String, IFieldDescriptor> getDeclaredFields() {
        return Collections.unmodifiableMap(declaredFields);
    }

    public Map<String, IFieldDescriptor> getFields() {
        if (fields == null) {
            fields = new HashMap<String, IFieldDescriptor>();
            for (String superType : superTypes) {
                try {
                    fields.putAll(locator.locate(superType).getFields());
                } catch (StoreException e) {
                    throw new RuntimeException("Supertype cannot be found", e);
                }
            }
            fields.putAll(declaredFields);
        }
        return Collections.unmodifiableMap(fields);
    }

    public IFieldDescriptor getField(String key) {
        return getFields().get(key);
    }

    public void addField(IFieldDescriptor field) {
        checkMutable();
        String name = field.getName();
        declaredFields.put(name, field);
        fields = null;
        if (obContext != null) {
            EventCollection<IEvent<ITypeDescriptor>> collection = new EventCollection<IEvent<ITypeDescriptor>>();
            collection.add(new TypeDescriptorEvent(this, field, TypeDescriptorEvent.EventType.FIELD_ADDED));
            obContext.notifyObservers(collection);
        }
    }

    public void removeField(String name) {
        checkMutable();
        IFieldDescriptor field = declaredFields.remove(name);
        fields = null;
        if (obContext != null) {
            EventCollection<TypeDescriptorEvent> collection = new EventCollection<TypeDescriptorEvent>();
            collection.add(new TypeDescriptorEvent(this, field, TypeDescriptorEvent.EventType.FIELD_REMOVED));
            obContext.notifyObservers(collection);
        }
    }

    public void setPrimary(String name) {
        checkMutable();
        if (primary != null) {
            if (!primary.getName().equals(name)) {
                primary.setPrimary(false);
                primary = null;
            } else {
                return;
            }
        }

        IFieldDescriptor field = declaredFields.get(name);
        if (field != null) {
            if (field instanceof JavaFieldDescriptor) {
                ((JavaFieldDescriptor) field).setPrimary(true);
                primary = (JavaFieldDescriptor) field;
            } else {
                log.warn("unknown type " + field.getClass().getName());
            }
        } else {
            log.warn("field " + name + " was not found");
        }
    }

    public boolean isNode() {
        return node;
    }

    public void setIsNode(boolean isNode) {
        checkMutable();
        this.node = isNode;
    }

    public boolean isMixin() {
        return mixin;
    }

    public void setIsMixin(boolean isMixin) {
        checkMutable();
        this.mixin = isMixin;
    }

    public boolean isValidationCascaded() {
        return cascadeValidation;
    }

    public void setIsValidationCascaded(boolean isCascaded) {
        cascadeValidation = isCascaded;
    }
    
    public void setObservationContext(IObservationContext context) {
        this.obContext = context;
    }

    public void startObservation() {
    }

    public void stopObservation() {
    }

    public boolean isType(String typeName) {
        return getType().equals(typeName);
    }

    protected void checkMutable() {
        if (!mutable) {
            throw new UnsupportedOperationException("type is immutable");
        }
    }

    public boolean isMutable() {
        return mutable;
    }

    public void setMutable(boolean mutable) {
        this.mutable = mutable;
    }

    public void detach() {
        fields = null;
        for (IFieldDescriptor field : declaredFields.values()) {
            if (field instanceof IDetachable) {
                ((IDetachable) field).detach();
            }
        }
    }

}
