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
package org.hippoecm.frontend.model.properties;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.observation.Event;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.ItemModelWrapper;
import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.EventCollection;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObservationContext;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.model.event.JcrEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrPropertyModel<T> extends ItemModelWrapper<Property> implements IDataProvider<T>, IObservable {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(ItemModelWrapper.class);

    private static class IndexedValue {
        protected Value value;
        protected int index;

        IndexedValue(Value value, int index) {
            this.index = index;
            this.value = value;
        }
    }

    private IObservationContext obContext;
    private IObserver observer;
    private JcrNodeModel parentModel;

    //  Constructor
    public JcrPropertyModel(JcrItemModel<Property> model) {
        super(model);
    }

    public JcrPropertyModel(Property prop) {
        super(prop);
    }

    public JcrPropertyModel(String path) {
        super(path);
    }

    // The wrapped jcr property

    public Property getProperty() {
        return (Property) getItemModel().getObject();
    }

    public PropertyDefinition getDefinition(int type, boolean multiValued) {
        Node node = (Node) getItemModel().getParentModel().getObject();
        String path = getItemModel().getPath();
        String name = path.substring(path.lastIndexOf('/') + 1);

        try {
            NodeType priType = node.getPrimaryNodeType();
            PropertyDefinition def = findDefinition(priType, name, type, multiValued);
            if (def != null) {
                return def;
            }
            for (NodeType mixin : node.getMixinNodeTypes()) {
                def = findDefinition(mixin, name, type, multiValued);
                if (def != null) {
                    return def;
                }
            }

            // not found; try to match ANY ("*") definitions
            priType = node.getPrimaryNodeType();
            def = findDefinition(priType, "*", type, multiValued);
            if (def != null) {
                return def;
            }
            for (NodeType mixin : node.getMixinNodeTypes()) {
                def = findDefinition(mixin, "*", type, multiValued);
                if (def != null) {
                    return def;
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    protected PropertyDefinition findDefinition(NodeType nodeType, String name, int type, boolean multiValued) {
        PropertyDefinition best = null;
        for (PropertyDefinition pdef : nodeType.getPropertyDefinitions()) {
            if (pdef.getName().equals(name) && pdef.isMultiple() == multiValued) {
                if ((!"*".equals(name) && type == JcrPropertyValueModel.NO_TYPE) || pdef.getRequiredType() == type) {
                    return pdef;
                } else if (pdef.getRequiredType() == PropertyType.UNDEFINED) {
                    best = pdef;
                }
            }
        }
        return best;
    }

    // IDataProvider implementation for use in DataViews
    // (lists and tables)

    // FIXME: iterator should return domain objects (Value holders)
    public Iterator iterator(int first, int count) {
        List<IndexedValue> list = new ArrayList<IndexedValue>();
        try {
            Property prop = getProperty();
            if (prop == null) {
                return list.iterator();
            }
            if (prop.getDefinition().isMultiple()) {
                Value[] values = prop.getValues();
                for (int i = 0; i < values.length; i++) {
                    list.add(new IndexedValue(values[i], i));
                }
            } else {
                list.add(new IndexedValue(prop.getValue(), JcrPropertyValueModel.NO_INDEX));
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
        return list.iterator();
    }

    public IModel<T> model(Object object) {
        IndexedValue indexedValue = (IndexedValue) object;
        if (indexedValue.index == JcrPropertyValueModel.NO_INDEX) {
            return new JcrPropertyValueModel(this);
        } else {
            return new JcrPropertyValueModel(indexedValue.index, indexedValue.value, this);
        }
    }

    public int size() {
        try {
            Property prop = getProperty();
            if (prop == null) {
                return 0;
            }
            if (prop.getDefinition().isMultiple()) {
                return prop.getValues().length;
            } else {
                return 1;
            }
        } catch (RepositoryException e) {
            log.info("Failed to determine number of values", e);
            detach();
        }
        return 0;
    }

    public void setObservationContext(IObservationContext context) {
        this.obContext = context;
    }

    public void startObservation() {
        parentModel = new JcrNodeModel(getItemModel().getParentModel());
        observer = new IObserver<JcrNodeModel>() {
            private static final long serialVersionUID = 1L;

            public JcrNodeModel getObservable() {
                return parentModel;
            }

            public void onEvent(Iterator<? extends IEvent<JcrNodeModel>> events) {
                EventCollection<JcrEvent> filtered = new EventCollection<JcrEvent>();
                while (events.hasNext()) {
                    JcrEvent jcrEvent = (JcrEvent) events.next();
                    Event event = jcrEvent.getEvent();
                    try {
                        switch (event.getType()) {
                        case 0:
                            filtered.add(jcrEvent);
                            break;
                        case Event.PROPERTY_ADDED:
                        case Event.PROPERTY_REMOVED:
                        case Event.PROPERTY_CHANGED:
                            String path = event.getPath();
                            JcrItemModel eventModel = new JcrItemModel(path);
                            if (eventModel.equals(getItemModel())) {
                                filtered.add(jcrEvent);
                            }
                        }
                    } catch (RepositoryException ex) {
                        log.error("Error filtering event", ex);
                    }
                }
                if (filtered.size() > 0) {
                    obContext.notifyObservers(filtered);
                }
            }

        };
        obContext.registerObserver(observer);
    }

    public void stopObservation() {
        obContext.unregisterObserver(observer);
        observer = null;
        parentModel = null;
    }

    @Override
    public void detach() {
        if (parentModel != null) {
            parentModel.detach();
        }
        super.detach();
    }

    // override Object

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).append("itemModel", getItemModel().toString())
                .toString();
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof JcrPropertyModel)) {
            return false;
        }
        if (this == object) {
            return true;
        }
        JcrPropertyModel propertyModel = (JcrPropertyModel) object;
        return new EqualsBuilder().append(getItemModel(), propertyModel.getItemModel()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(473, 17).append(getItemModel()).toHashCode();
    }

}
