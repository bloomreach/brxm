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
package org.hippoecm.editor.type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.wicket.model.IDetachable;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.EventCollection;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservationContext;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.model.ocm.JcrObject;
import org.hippoecm.frontend.model.ocm.StoreException;
import org.hippoecm.frontend.types.BuiltinTypeDescriptor;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.frontend.types.ITypeLocator;
import org.hippoecm.frontend.types.TypeDescriptorEvent;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrTypeDescriptor extends JcrObject implements ITypeDescriptor {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(JcrTypeDescriptor.class);

    ITypeLocator locator;

    private String name;
    private Map<String, IFieldDescriptor> declaredFields;
    private Map<String, IFieldDescriptor> fields;
    private Map<String, IObserver> observers = new TreeMap<String, IObserver>();
    private IFieldDescriptor primary;
    private transient boolean attached = false;

    public JcrTypeDescriptor(JcrNodeModel nodeModel, ITypeLocator locator) throws RepositoryException {
        super(nodeModel);

        this.locator = locator;

        Node typeNode = nodeModel.getNode();
        Node templateTypeNode = typeNode;
        while (!templateTypeNode.isNodeType(HippoNodeType.NT_TEMPLATETYPE)) {
            templateTypeNode = templateTypeNode.getParent();
        }

        String prefix = templateTypeNode.getParent().getName();
        if ("system".equals(prefix)) {
            name = templateTypeNode.getName();
        } else {
            name = prefix + ":" + templateTypeNode.getName();
        }

        attached = true;
        loadFields();
    }

    public String getName() {
        return name;
    }

    public String getType() {
        try {
            Node typeNode = getNode();
            if (typeNode.hasProperty(HippoNodeType.HIPPOSYSEDIT_TYPE)) {
                return typeNode.getProperty(HippoNodeType.HIPPOSYSEDIT_TYPE).getString();
            }
        } catch (RepositoryException ex) {
            log.error("error determining JCR type");
        }
        return name;
    }

    public List<String> getSuperTypes() {
        try {
            Node node = getNode();
            List<String> superTypes = new LinkedList<String>();
            if (node.hasProperty(HippoNodeType.HIPPO_SUPERTYPE)) {
                Value[] values = node.getProperty(HippoNodeType.HIPPO_SUPERTYPE).getValues();
                for (Value value : values) {
                    superTypes.add(value.getString());
                }
            }
            return superTypes;
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return Collections.emptyList();
    }

    public List<ITypeDescriptor> getSubTypes() {
        try {
            return locator.getSubTypes(getType());
        } catch (StoreException e) {
            log.error("error retrieving subtypes", e);
        }
        return Collections.emptyList();
    }

    public void setSuperTypes(List<String> superTypes) {
        try {
            String[] types = superTypes.toArray(new String[superTypes.size()]);
            getNode().setProperty(HippoNodeType.HIPPO_SUPERTYPE, types);
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
    }

    public Map<String, IFieldDescriptor> getFields() {
        attach();
        return Collections.unmodifiableMap(fields);
    }

    public Map<String, IFieldDescriptor> getDeclaredFields() {
        attach();
        return Collections.unmodifiableMap(declaredFields);
    }

    public IFieldDescriptor getField(String key) {
        return getFields().get(key);
    }

    public boolean isNode() {
        try {
            if (getNode().hasProperty(HippoNodeType.HIPPO_NODE)) {
                return getNode().getProperty(HippoNodeType.HIPPO_NODE).getBoolean();
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return true;
    }

    public void setIsNode(boolean isNode) {
        setBoolean(HippoNodeType.HIPPO_NODE, isNode);
    }

    public boolean isMixin() {
        return getBoolean(HippoNodeType.HIPPO_MIXIN);
    }

    public void setIsMixin(boolean isMixin) {
        setBoolean(HippoNodeType.HIPPO_MIXIN, isMixin);
    }

    public boolean isValidationCascaded() {
        return getBoolean(HippoNodeType.HIPPO_CASCADEVALIDATION, true);
    }

    public void setIsValidationCascaded(boolean isCascaded) {
        setBoolean(HippoNodeType.HIPPO_CASCADEVALIDATION, isCascaded);
    }

    public void addField(IFieldDescriptor descriptor) {
        try {
            Node typeNode = getNode();
            Node field = typeNode.addNode(descriptor.getName(), HippoNodeType.NT_FIELD);
            JcrFieldDescriptor desc = new JcrFieldDescriptor(new JcrNodeModel(field), this);
            desc.copy(descriptor);

            detach();
        } catch (RepositoryException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    public void removeField(String field) {
        try {
            detach();

            Node fieldNode = getFieldNode(field);
            if (fieldNode != null) {
                IFieldDescriptor descriptor = new JcrFieldDescriptor(new JcrNodeModel(fieldNode), this);

                if (descriptor.isPrimary()) {
                    primary = null;
                }
                fieldNode.remove();
            } else {
                log.warn("field " + field + " was not found in type " + getType());
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
    }

    public void setPrimary(String name) {
        attach();
        if (primary != null) {
            if (!primary.getName().equals(name)) {
                if (!(primary instanceof JcrFieldDescriptor)) {
                    throw new IllegalArgumentException("Field " + name + " was not declared in type " + getName());
                }
                ((JcrFieldDescriptor) primary).setPrimary(false);
                primary = null;
            } else {
                return;
            }
        }

        JcrFieldDescriptor field = (JcrFieldDescriptor) declaredFields.get(name);
        if (field != null) {
            field.setPrimary(true);
            primary = field;
        } else {
            log.warn("field " + name + " was not found");
        }

        detach();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof JcrTypeDescriptor) {
            JcrTypeDescriptor that = (JcrTypeDescriptor) object;
            return new EqualsBuilder().append(this.name, that.name).isEquals();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(53, 7).append(this.name).toHashCode();
    }

    /**
     * (re)load the cached fields "fields", "declaredFields", "primary"
     */
    protected void loadFields() {
        fields = new HashMap<String, IFieldDescriptor>();
        declaredFields = new LinkedHashMap<String, IFieldDescriptor>();
        primary = null;

        for (String superType : getSuperTypes()) {
            try {
                fields.putAll(locator.locate(superType).getFields());
            } catch (StoreException e) {
                throw new IllegalStateException("Could not resolve type " + superType, e);
            }
        }
        try {
            Node node = getNode();
            if (node != null) {
                NodeIterator it = node.getNodes();
                while (it.hasNext()) {
                    Node child = it.nextNode();
                    if (child != null && child.isNodeType(HippoNodeType.NT_FIELD)) {
                        JcrFieldDescriptor field = new JcrFieldDescriptor(new JcrNodeModel(child), this);
                        declaredFields.put(field.getName(), field);
                        fields.put(field.getName(), field);
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
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }

        for (IFieldDescriptor field : fields.values()) {
            if (field.isPrimary()) {
                primary = field;
                break;
            }
        }
    }

    protected void attach() {
        if (!attached) {
            updateFields();
            attached = true;
        }
    }

    @Override
    public void detach() {
        for (IFieldDescriptor field : fields.values()) {
            if (field instanceof IDetachable) {
                ((IDetachable) field).detach();
            }
        }
        attached = false;
        super.detach();
    }

    private boolean getBoolean(String path) {
        return getBoolean(path, false);
    }

    private boolean getBoolean(String path, boolean defaultValue) {
        try {
            if (getNode().hasProperty(path)) {
                return getNode().getProperty(path).getBoolean();
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return defaultValue;
    }

    private void setBoolean(String path, boolean value) {
        try {
            getNode().setProperty(path, value);
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
    }

    private Node getFieldNode(String field) throws RepositoryException {
        Node typeNode = getNode();
        NodeIterator fieldIter = typeNode.getNodes();
        while (fieldIter.hasNext()) {
            Node fieldNode = fieldIter.nextNode();
            if (!fieldNode.isNodeType(HippoNodeType.NT_FIELD)) {
                continue;
            }
            String name = fieldNode.getName();
            if (name.equals(field)) {
                return fieldNode;
            }
        }
        return null;
    }

    @Override
    public void startObservation() {
        super.startObservation();
        for (IFieldDescriptor field : fields.values()) {
            subscribe(field);
        }
    }

    @Override
    public void stopObservation() {
        for (IFieldDescriptor field : fields.values()) {
            unsubscribe(field);
        }
        super.stopObservation();
    }

    private void subscribe(final IFieldDescriptor field) {
        final IObservationContext obContext = getObservationContext();
        IObserver<IFieldDescriptor> observer = new IObserver<IFieldDescriptor>() {
            private static final long serialVersionUID = 1L;

            public IFieldDescriptor getObservable() {
                return field;
            }

            public void onEvent(Iterator<? extends IEvent<IFieldDescriptor>> events) {
                EventCollection<TypeDescriptorEvent> collection = new EventCollection<TypeDescriptorEvent>();
                while (events.hasNext()) {
                    IEvent event = events.next();
                    if (event instanceof TypeDescriptorEvent) {
                        collection.add((TypeDescriptorEvent) event);
                    }
                }
                if (collection.size() > 0) {
                    obContext.notifyObservers(collection);
                }
            }

        };
        observers.put(field.getName(), observer);
        obContext.registerObserver(observer);
    }

    private void unsubscribe(IFieldDescriptor field) {
        final IObservationContext obContext = getObservationContext();
        IObserver observer = observers.remove(field.getName());
        obContext.unregisterObserver(observer);
    }

    @Override
    protected void processEvents(IObservationContext context, Iterator<? extends IEvent> events) {
        // do our own event generation, ignoring the JCR events
        updateFields();
        attached = true;
    }

    protected void processEvents(EventCollection collection) {
        IObservationContext obContext = getObservationContext();
        if (obContext != null) {
            for (IFieldDescriptor field : fields.values()) {
                unsubscribe(field);
            }
        }

        Map<String, IFieldDescriptor> oldFields = fields;
        fields = null;
        loadFields();
        for (String field : fields.keySet()) {
            if (!oldFields.containsKey(field)) {
                collection.add(new TypeDescriptorEvent(this, fields.get(field),
                        TypeDescriptorEvent.EventType.FIELD_ADDED));
            }
        }
        Iterator<Entry<String, IFieldDescriptor>> iter = oldFields.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<String, IFieldDescriptor> entry = iter.next();
            if (!fields.containsKey(entry.getKey())) {
                collection.add(new TypeDescriptorEvent(this, entry.getValue(),
                        TypeDescriptorEvent.EventType.FIELD_REMOVED));
                iter.remove();
            }
        }

        if (obContext != null) {
            for (IFieldDescriptor field : fields.values()) {
                subscribe(field);
            }
        }
    }

    protected void updateFields() {
        EventCollection collection = new EventCollection();
        processEvents(collection);
        if (collection.size() > 0) {
            IObservationContext obContext = getObservationContext();
            if (obContext != null) {
                obContext.notifyObservers(collection);
            }
        }
    }

    public boolean isType(String typeName) {
        if (getType().equals(typeName)) {
            return true;
        }
        List<String> candidates = getSuperTypes();
        Set<String> done = new TreeSet<String>();
        while (candidates.size() > 0) {
            Set<String> todo = new TreeSet<String>();
            for (String candidate : candidates) {
                if (candidate.equals(typeName)) {
                    return true;
                } else {
                    done.add(candidate);
                    try {
                        ITypeDescriptor descriptor = locator.locate(candidate);
                        for (String superType : descriptor.getSuperTypes()) {
                            if (!done.contains(superType)) {
                                todo.add(superType);
                            }
                        }
                    } catch (StoreException ex) {
                        log.error(ex.getMessage());
                        return false;
                    }
                }
            }
            candidates = new ArrayList<String>(todo);
        }
        return false;
    }

    @Override
    public void save() {
        attach();
        // remove ordered attribute if present on a non-multiple field
        for (IFieldDescriptor field : declaredFields.values()) {
            if (!field.isMultiple() && field.isOrdered()) {
                field.setOrdered(false);
            }
        }
        super.save();
    }

    void validate() {
        ITypeDescriptor builtin;
        try {
            builtin = new BuiltinTypeDescriptor(getType(), locator);
        } catch (StoreException e) {
            return;
        }

        List<String> superTypes = getSuperTypes();
        List<String> builtinSuperTypes = builtin.getSuperTypes();
        for (String type : builtinSuperTypes) {
            if (!superTypes.contains(type)) {
                log.warn("Super type " + type + " is defined as super type in CND, but not available in descriptor for " + name);
            }
        }
        for (String type : superTypes) {
            if (!builtinSuperTypes.contains(type)) {
                log.warn("Super type " + type + " is declared in descriptor, but is not defined as super type in CND for " + name);
            }
        }

        if (isMixin() != builtin.isMixin()) {
            log.warn("Node type definition and description disagree on mixin status for " + name);
        }

        if (isNode() != builtin.isNode()) {
            log.warn("Node type definition and description disagree on compound/primitive classification " + name);
        }

        if (isNode()) {
            Map<String, IFieldDescriptor> anyPaths = new HashMap<String, IFieldDescriptor>();
            Map<String, IFieldDescriptor> pathToField = new HashMap<String, IFieldDescriptor>();
            Map<String, IFieldDescriptor> nameToField = getDeclaredFields();
            for (Map.Entry<String, IFieldDescriptor> entry : nameToField.entrySet()) {
                if ("*".equals(entry.getValue().getPath())) {
                    anyPaths.put(entry.getValue().getTypeDescriptor().getType(), entry.getValue());
                } else {
                    pathToField.put(entry.getValue().getPath(), entry.getValue());
                }
            }

            Map<String, IFieldDescriptor> builtinAnyPaths = new HashMap<String, IFieldDescriptor>();
            Map<String, IFieldDescriptor> builtinPathToField = new HashMap<String, IFieldDescriptor>();
            Map<String, IFieldDescriptor> builtinNameToField = builtin.getDeclaredFields();
            for (Map.Entry<String, IFieldDescriptor> entry : builtinNameToField.entrySet()) {
                if ("*".equals(entry.getValue().getPath())) {
                    builtinAnyPaths.put(entry.getValue().getTypeDescriptor().getType(), entry.getValue());
                } else {
                    builtinPathToField.put(entry.getValue().getPath(), entry.getValue());
                }
            }

            for (Map.Entry<String, IFieldDescriptor> entry : pathToField.entrySet()) {
                if (builtinPathToField.containsKey(entry.getKey())) {
                    validateField(entry.getValue(), builtinPathToField.get(entry.getKey()));
                } else if (!builtinAnyPaths.isEmpty()) {
                    log.warn("Path " + entry.getKey() + " is present in description, but not in CND definition for " + name);
                }
            }
            for (Map.Entry<String, IFieldDescriptor> entry : builtinPathToField.entrySet()) {
                if ("*".equals(entry.getKey())) {
                    continue;
                }
                if (!pathToField.containsKey(entry.getKey())) {
                    log.warn("Path " + entry.getKey() + " is present in CND definition, but not in description for " + name);
                }
            }

        }
    }

    void validateField(IFieldDescriptor myField, IFieldDescriptor builtinField) {
        if (!myField.getTypeDescriptor().getType().equals(builtinField.getTypeDescriptor().getType())) {
            log.warn("Node type definition and description disagree on type for field: " + myField.toString());
        }

        if (myField.isMandatory() != builtinField.isMandatory()) {
            log.warn("Node type definition and description disagree on mandatory keyword for " + myField.toString());
        }

        if (myField.isMultiple() != builtinField.isMultiple()) {
            log.warn("Node type definition and description disagree on multiple keyword for " + myField.toString());
        }

        if (myField.isProtected() != builtinField.isProtected()) {
            log.warn("Node type definition and description disagree on protected keyword for " + myField.toString());
        }

        if (myField.isAutoCreated() != builtinField.isAutoCreated()) {
            log.warn("Node type definition and description disagree on autocreated keyword for " + myField.toString());
        }

        if (myField.isPrimary() != builtinField.isPrimary()) {
            log.warn("Node type definition and description disagree on primary keyword for " + myField.toString());
        }
    }

}
