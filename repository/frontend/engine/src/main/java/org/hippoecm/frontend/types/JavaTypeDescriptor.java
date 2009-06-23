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

import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.wicket.Session;
import org.hippoecm.frontend.model.event.ListenerList;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaTypeDescriptor implements ITypeDescriptor {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(JavaTypeDescriptor.class);

    private String name;
    private String type;
    private List<String> superTypes;
    private Map<String, IFieldDescriptor> fields;
    private JavaFieldDescriptor primary;
    private boolean node;
    private boolean mixin;
    private List<ITypeListener> listeners;

    public JavaTypeDescriptor(String name, String type) {
        this.name = name;
        this.type = type;
        this.superTypes = new LinkedList<String>();
        this.fields = new HashMap<String, IFieldDescriptor>();
        this.primary = null;
        this.node = true;
        this.mixin = false;
        this.listeners = new ListenerList<ITypeListener>();
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

    public void setSuperTypes(List<String> superTypes) {
        this.superTypes = superTypes;
    }

    public Map<String, IFieldDescriptor> getFields() {
        return fields;
    }

    public IFieldDescriptor getField(String key) {
        return getFields().get(key);
    }

    public void addField(IFieldDescriptor field) {
        String name = field.getName();
        fields.put(name, field);
        for (ITypeListener listener : listeners) {
            listener.fieldAdded(name);
        }
    }

    public void removeField(String name) {
        fields.remove(name);
        for (ITypeListener listener : listeners) {
            listener.fieldRemoved(name);
        }
    }

    public void setPrimary(String name) {
        if (primary != null) {
            if (!primary.getName().equals(name)) {
                primary.setPrimary(false);
                primary = null;
            } else {
                return;
            }
        }

        IFieldDescriptor field = fields.get(name);
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
        this.node = isNode;
    }

    public boolean isMixin() {
        return mixin;
    }

    public void setIsMixin(boolean isMixin) {
        this.mixin = isMixin;
    }

    public Value createValue() {
        try {
            int propertyType = PropertyType.valueFromName(type);
            switch (propertyType) {
            case PropertyType.BOOLEAN:
                return ((UserSession) Session.get()).getJcrSession().getValueFactory().createValue(false);
            case PropertyType.DATE:
                return ((UserSession) Session.get()).getJcrSession().getValueFactory().createValue(Calendar.getInstance());
            case PropertyType.DOUBLE:
                return ((UserSession) Session.get()).getJcrSession().getValueFactory().createValue(0.0);
            case PropertyType.LONG:
                return ((UserSession) Session.get()).getJcrSession().getValueFactory().createValue(0L);
            case PropertyType.NAME:
                return ((UserSession) Session.get()).getJcrSession().getValueFactory().createValue("", PropertyType.NAME);
            case PropertyType.PATH:
                return ((UserSession) Session.get()).getJcrSession().getValueFactory().createValue("/", PropertyType.PATH);
            case PropertyType.REFERENCE:
                return ((UserSession) Session.get()).getJcrSession().getValueFactory().createValue(UUID.randomUUID().toString(), PropertyType.REFERENCE);
            case PropertyType.STRING:
            case PropertyType.UNDEFINED:
                return ((UserSession) Session.get()).getJcrSession().getValueFactory().createValue("", PropertyType.STRING);
            default:
                return null;
           }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
            return null;
        }
    }

    public void addTypeListener(ITypeListener listener) {
        listeners.add(listener);
    }

    public void removeTypeListener(ITypeListener listener) {
        listeners.remove(listener);
    }

}
