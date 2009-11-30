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
package org.hippoecm.frontend.editor.builder;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.wicket.IClusterable;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.collections.MiniMap;
import org.hippoecm.editor.tools.JcrPrototypeStore;
import org.hippoecm.editor.tools.JcrTypeStore;
import org.hippoecm.frontend.editor.impl.BuiltinTemplateStore;
import org.hippoecm.frontend.editor.impl.JcrTemplateStore;
import org.hippoecm.frontend.editor.plugins.field.NodeFieldPlugin;
import org.hippoecm.frontend.editor.plugins.field.PropertyFieldPlugin;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.EventCollection;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObservationContext;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.model.ocm.IStore;
import org.hippoecm.frontend.model.ocm.StoreException;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.ClusterConfigEvent;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.AbstractClusterDecorator;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.types.BuiltinTypeStore;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.frontend.types.ITypeLocator;
import org.hippoecm.frontend.types.JavaFieldDescriptor;
import org.hippoecm.frontend.types.TypeHelper;
import org.hippoecm.frontend.types.TypeLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemplateBuilder implements IDetachable, IObservable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(TemplateBuilder.class);

    class BuilderFieldDescriptor implements IFieldDescriptor, IDetachable {
        private static final long serialVersionUID = 6935814333088957137L;

        private IFieldDescriptor delegate;

        BuilderFieldDescriptor(IFieldDescriptor descriptor) {
            delegate = descriptor;
        }

        @Override
        public boolean equals(Object obj) {
            return ((obj instanceof BuilderFieldDescriptor) && delegate.equals(((BuilderFieldDescriptor) obj).delegate));
        }

        @Override
        public int hashCode() {
            return 991 ^ delegate.hashCode();
        }

        public Set<String> getExcluded() {
            return delegate.getExcluded();
        }

        public String getName() {
            return delegate.getName();
        }

        public String getPath() {
            return delegate.getPath();
        }

        public ITypeDescriptor getTypeDescriptor() {
            return delegate.getTypeDescriptor();
        }

        public boolean isAutoCreated() {
            return delegate.isAutoCreated();
        }

        public void setAutoCreated(boolean autocreated) {
            delegate.setAutoCreated(autocreated);
        }

        public boolean isMandatory() {
            return delegate.isMandatory();
        }

        public boolean isMultiple() {
            return delegate.isMultiple();
        }

        public boolean isOrdered() {
            return delegate.isOrdered();
        }

        public boolean isPrimary() {
            return delegate.isPrimary();
        }

        public boolean isProtected() {
            return delegate.isProtected();
        }

        public void setExcluded(Set<String> set) {
            delegate.setExcluded(set);
        }

        public void setMandatory(boolean mandatory) {
            delegate.setMandatory(mandatory);
        }

        public void setMultiple(boolean multiple) {
            delegate.setMultiple(multiple);
        }

        public void setOrdered(boolean isOrdered) {
            delegate.setOrdered(isOrdered);
        }

        public void setPath(String path) {
            String oldPath = delegate.getPath();
            delegate.setPath(path);
            try {
                IModel prototypeModel = getPrototype();
                Node prototype = (Node) prototypeModel.getObject();
                updateItem(prototype, oldPath, delegate);
            } catch (RepositoryException ex) {
                log.error("Failed to update prototype", ex);
            } catch (BuilderException ex) {
                log.error("Failed to find prototype when updating the path", ex);
            }

            Set<String> fieldNames = new HashSet<String>();
            for (ITypeDescriptor subType : typeDescriptor.getSubTypes()) {
                fieldNames.addAll(subType.getFields().keySet());
            }

            String name = delegate.getName();
            String newName = TypeHelper.getFieldName(path, getTypeDescriptor().getName());
            if (!typeDescriptor.getFields().containsKey(newName)
                    && !fieldNames.contains(newName)
                    && (currentTypeDescriptor == null || (!currentTypeDescriptor.getFields().containsKey(name) && !currentTypeDescriptor
                            .getFields().containsKey(newName)))) {
                JavaFieldDescriptor javaFieldDescriptor = new JavaFieldDescriptor(delegate);
                typeDescriptor.removeField(name);
                javaFieldDescriptor.setName(newName);
                typeDescriptor.addField(javaFieldDescriptor);
                delegate = typeDescriptor.getField(newName);
                paths.remove(name);
                paths.put(newName, path);
            } else {
                paths.put(name, path);
            }
            boolean containsNewName = false;
            int position = -1;
            List<IPluginConfig> plugins = clusterConfig.getPlugins();
            for (int i = 0; i < plugins.size(); i++) {
                IPluginConfig plugin = plugins.get(i);
                if (plugin.containsKey("field") && name.equals(plugin.getString("field"))) {
                    plugin.put("field", newName);
                    position = i;
                }
                if (newName.equals(plugin.getName())) {
                    containsNewName = true;
                }
            }
            if (!containsNewName) {
                JavaPluginConfig newPlugin = new JavaPluginConfig(newName);
                newPlugin.putAll(plugins.get(position));
                plugins.set(position, newPlugin);
            }
        }

        public void addValidator(String validator) {
            delegate.addValidator(validator);
        }

        public Set<String> getValidators() {
            return delegate.getValidators();
        }

        public void removeValidator(String validator) {
            delegate.removeValidator(validator);
        }

        private IObservationContext obContext;
        private IObserver observer;

        public void setObservationContext(IObservationContext context) {
            this.obContext = context;
        }

        public void startObservation() {
            obContext.registerObserver(observer = new IObserver<IFieldDescriptor>() {
                private static final long serialVersionUID = -510095692858775942L;

                public IFieldDescriptor getObservable() {
                    return delegate;
                }

                public void onEvent(Iterator<? extends IEvent<IFieldDescriptor>> events) {
                    obContext.notifyObservers(new EventCollection(events));
                }

            });
        }

        public void stopObservation() {
            obContext.unregisterObserver(observer);
            observer = null;
        }

        public void detach() {
            if (delegate instanceof IDetachable) {
                ((IDetachable) delegate).detach();
            }
        }
    }

    class FieldMap extends AbstractMap<String, IFieldDescriptor> implements Serializable {
        private static final long serialVersionUID = -7856611025545479323L;

        Map<String, IFieldDescriptor> delegate;

        FieldMap(Map<String, IFieldDescriptor> delegate) {
            this.delegate = delegate;
        }

        @Override
        public Set<Map.Entry<String, IFieldDescriptor>> entrySet() {
            return new AbstractSet<Map.Entry<String, IFieldDescriptor>>() {

                @Override
                public Iterator<Map.Entry<String, IFieldDescriptor>> iterator() {
                    final Iterator<Map.Entry<String, IFieldDescriptor>> upstream = delegate.entrySet().iterator();
                    return new Iterator<Map.Entry<String, IFieldDescriptor>>() {

                        public boolean hasNext() {
                            return upstream.hasNext();
                        }

                        public Map.Entry<String, IFieldDescriptor> next() {
                            final Map.Entry<String, IFieldDescriptor> upstreamEntry = upstream.next();
                            return new Map.Entry<String, IFieldDescriptor>() {

                                public String getKey() {
                                    return upstreamEntry.getKey();
                                }

                                public IFieldDescriptor getValue() {
                                    return wrap(upstreamEntry.getValue());
                                }

                                public IFieldDescriptor setValue(IFieldDescriptor value) {
                                    throw new UnsupportedOperationException();
                                }
                            };
                        }

                        public void remove() {
                            upstream.remove();
                        }

                    };
                }

                @Override
                public int size() {
                    return delegate.size();
                }

            };
        }

    }

    class BuilderTypeDescriptor implements ITypeDescriptor {

        private static final long serialVersionUID = 1L;

        private IObservationContext obContext;
        private IObserver observer;

        public void addField(IFieldDescriptor descriptor) {
            for (IFieldDescriptor field : getFields().values()) {
                if (!"*".equals(field.getPath())) {
                    if (field.getPath().equals(descriptor.getPath())) {
                        log.warn("Path " + descriptor.getPath() + " already exists, not adding field");
                        return;
                    }
                } else if ("*".equals(descriptor.getPath())) {
                    if (field.getTypeDescriptor().getType().equals(descriptor.getTypeDescriptor().getType())) {
                        log.warn("Path " + descriptor.getPath() + " already exists, not adding field");
                        return;
                    }
                }
            }
            typeDescriptor.addField(descriptor);
            processFieldAdded(descriptor);
            updatePrototype();
        }

        public Map<String, IFieldDescriptor> getDeclaredFields() {
            return new FieldMap(typeDescriptor.getDeclaredFields());
        }

        public IFieldDescriptor getField(String key) {
            return wrap(typeDescriptor.getField(key));
        }

        public Map<String, IFieldDescriptor> getFields() {
            return new FieldMap(typeDescriptor.getFields());
        }

        public String getName() {
            return typeDescriptor.getName();
        }

        public List<String> getSuperTypes() {
            return typeDescriptor.getSuperTypes();
        }

        public List<ITypeDescriptor> getSubTypes() {
            return typeDescriptor.getSubTypes();
        }

        public String getType() {
            return typeDescriptor.getType();
        }

        public boolean isMixin() {
            return typeDescriptor.isMixin();
        }

        public boolean isNode() {
            return typeDescriptor.isNode();
        }

        public boolean isType(String typeName) {
            return typeDescriptor.isType(typeName);
        }

        public void removeField(String name) {
            typeDescriptor.removeField(name);
            updatePrototype();
        }

        public void setIsMixin(boolean isMixin) {
            typeDescriptor.setIsMixin(isMixin);
        }

        public void setIsNode(boolean isNode) {
            typeDescriptor.setIsNode(isNode);
        }

        public void setObservationContext(final IObservationContext context) {
            obContext = context;
        }

        public void setPrimary(String name) {
            typeDescriptor.setPrimary(name);
        }

        public void setSuperTypes(List<String> superTypes) {
            typeDescriptor.setSuperTypes(superTypes);
        }

        public boolean isValidationCascaded() {
            return typeDescriptor.isValidationCascaded();
        }

        public void setIsValidationCascaded(boolean isCascaded) {
            typeDescriptor.setIsValidationCascaded(isCascaded);
        }

        public void startObservation() {
            obContext.registerObserver(observer = new IObserver<ITypeDescriptor>() {
                private static final long serialVersionUID = 1L;

                public ITypeDescriptor getObservable() {
                    return typeDescriptor;
                }

                public void onEvent(Iterator<? extends IEvent<ITypeDescriptor>> events) {
                    obContext.notifyObservers(new EventCollection(events));
                }

            });
        }

        public void stopObservation() {
            obContext.unregisterObserver(observer);
            observer = null;
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj;
        }

        @Override
        public int hashCode() {
            return typeDescriptor.hashCode();
        }

    }

    private IFieldDescriptor wrap(IFieldDescriptor descriptor) {
        return new BuilderFieldDescriptor(descriptor);
    }

    class BuilderPluginList extends AbstractList<IPluginConfig> implements IClusterable {
        private static final long serialVersionUID = 1L;

        private List<IPluginConfig> plugins;
        private transient Map<String, IFieldDescriptor> removedFields;

        BuilderPluginList(List<IPluginConfig> plugins) {
            this.plugins = plugins;
        }

        @Override
        public void add(int index, IPluginConfig config) {
            plugins.add(index, config);

            if (config.getString("field") != null) {
                String field = config.getString("field");
                if (removedFields != null) {
                    IFieldDescriptor descriptor = removedFields.remove(field);
                    if (descriptor != null) {
                        typeDescriptor.addField(descriptor);
                    }
                }
            }
        }

        @Override
        public IPluginConfig remove(int index) {
            IPluginConfig config = plugins.remove(index);
            String field = config.getString("field");
            if (field != null) {
                if (removedFields == null) {
                    removedFields = new TreeMap<String, IFieldDescriptor>();
                }
                removedFields.put(field, new JavaFieldDescriptor(typeDescriptor.getField(field)));
                typeDescriptor.removeField(field);
                updatePrototype();
            }
            return config;
        }

        @Override
        public IPluginConfig get(int index) {
            return plugins.get(index);
        }

        @Override
        public int size() {
            return plugins.size();
        }

    }

    private static class BuilderClusterDecorator extends AbstractClusterDecorator {
        private static final long serialVersionUID = 1L;

        List<IPluginConfig> plugins;

        private BuilderClusterDecorator(IClusterConfig upstream, List<IPluginConfig> plugins) {
            super(upstream);
            this.plugins = plugins;
        }

        @Override
        public List<IPluginConfig> getPlugins() {
            return plugins;
        }

        @Override
        protected Object decorate(Object object) {
            return object;
        }

    }

    private String type;
    private boolean readonly;
    private IPluginContext context;
    private IModel selectedExtPtModel;

    private IStore<IClusterConfig> jcrTemplateStore;
    private IStore<IClusterConfig> builtinTemplateStore;

    private JcrTypeStore jcrTypeStore;
    private BuiltinTypeStore builtinTypeStore;

    private JcrPrototypeStore prototypeStore;

    private ITypeDescriptor currentTypeDescriptor;
    private ITypeDescriptor typeDescriptor;
    private IClusterConfig clusterConfig;
    private JcrNodeModel prototype;
    private Map<String, String> paths;
    private Map<String, IPluginConfig> pluginCache;

    private IObservationContext obContext;
    private IObserver clusterObserver;

    private BuilderPluginList plugins;

    public TemplateBuilder(String type, boolean readonly, IPluginContext context, IModel extPtModel)
            throws BuilderException {
        this.type = type;
        this.readonly = readonly;
        this.context = context;
        this.selectedExtPtModel = extPtModel;

        this.jcrTypeStore = new JcrTypeStore();
        this.builtinTypeStore = new BuiltinTypeStore();
        ITypeLocator fieldTypeLocator = new TypeLocator(new IStore[] { jcrTypeStore, builtinTypeStore });
        builtinTypeStore.setTypeLocator(fieldTypeLocator);

        this.jcrTemplateStore = new JcrTemplateStore(fieldTypeLocator);
        this.builtinTemplateStore = new BuiltinTemplateStore(fieldTypeLocator);

        this.prototypeStore = new JcrPrototypeStore();

        try {
            typeDescriptor = jcrTypeStore.load(type);

            try {
                currentTypeDescriptor = jcrTypeStore.getCurrentType(type);
            } catch (StoreException ex) {
                // ignore
            }

            // load template
            @SuppressWarnings("unchecked")
            Map<String, Object> criteria = new MiniMap(1);
            criteria.put("type", typeDescriptor);
            Iterator<IClusterConfig> iter = jcrTemplateStore.find(criteria);
            if (iter.hasNext()) {
                clusterConfig = iter.next();
                initPluginCache();
            } else {
                if (!readonly) {
                    iter = builtinTemplateStore.find(criteria);
                    if (iter.hasNext()) {
                        try {
                            String id = jcrTemplateStore.save(iter.next());
                            clusterConfig = jcrTemplateStore.load(id);
                            initPluginCache();
                        } catch (StoreException ex) {
                            throw new BuilderException("Failed to save generated template", ex);
                        }
                    }
                } else {
                    throw new BuilderException("No template found to display type");
                }
            }
        } catch (StoreException ex) {
            if (!readonly) {
                try {
                    ITypeDescriptor builtin = builtinTypeStore.load(type);
                    String id = jcrTypeStore.save(builtin);
                    if (!id.equals(type)) {
                        throw new BuilderException("Created type descriptor has invalid id " + id);
                    }
                    typeDescriptor = jcrTypeStore.load(id);
                } catch (StoreException ex2) {
                    throw new BuilderException("Could not convert builtin type descriptor to editable copy", ex2);
                }
            } else {
                throw new BuilderException("Could not load type descriptor", ex);
            }
        }

        registerObservers();
    }

    public void dispose() {
        unregisterObservers();
    }

    public String getName() {
        return type;
    }

    public ITypeDescriptor getTypeDescriptor() throws BuilderException {
        return new BuilderTypeDescriptor();
    }

    protected final List<IPluginConfig> getPlugins() {
        if (plugins == null) {
            plugins = new BuilderPluginList(clusterConfig.getPlugins());
        }
        return plugins;
    }

    public IClusterConfig getTemplate() throws BuilderException {
        if (clusterConfig != null) {
            return new BuilderClusterDecorator(clusterConfig, getPlugins());
        }
        throw new BuilderException("No template exists");
    }

    public JcrNodeModel getPrototype() throws BuilderException {
        if (prototype == null || prototype.getObject() == null) {
            prototype = prototypeStore.getPrototype(type, true);
            if (prototype == null) {
                if (readonly) {
                    prototype = prototypeStore.getPrototype(type, false);
                    if (prototype == null) {
                        throw new BuilderException("No prototype found");
                    }
                } else {
                    prototype = prototypeStore.createPrototype(type, true);
                    if (prototype == null) {
                        throw new BuilderException("No prototype could be created for " + type);
                    }
                }
            }
            paths = new HashMap<String, String>();
            for (Map.Entry<String, IFieldDescriptor> entry : typeDescriptor.getFields().entrySet()) {
                paths.put(entry.getKey(), entry.getValue().getPath());
            }
        }
        return prototype;
    }

    public void save() throws BuilderException {
        if (readonly) {
            throw new UnsupportedOperationException("readonly builder cannot save artifacts");
        }
        try {
            jcrTypeStore.save(typeDescriptor);

            jcrTemplateStore.save(clusterConfig);

            JcrNodeModel nodeModel = getPrototype();
            try {
                nodeModel.getNode().save();
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        } catch (StoreException ex) {
            log.error(ex.getMessage());
        }
    }

    private void updatePrototype() {
        try {
            IModel prototypeModel = getPrototype();

            Node prototype = (Node) prototypeModel.getObject();
            if (prototype != null && typeDescriptor != null) {
                Map<String, String> oldFields = paths;
                paths = new HashMap<String, String>();
                for (Map.Entry<String, IFieldDescriptor> entry : typeDescriptor.getFields().entrySet()) {
                    paths.put(entry.getKey(), entry.getValue().getPath());
                }

                for (Map.Entry<String, String> entry : oldFields.entrySet()) {
                    String oldPath = entry.getValue();
                    IFieldDescriptor newField = typeDescriptor.getField(entry.getKey());
                    updateItem(prototype, oldPath, newField);
                }
            }
        } catch (RepositoryException ex) {
            log.error("Failed to update prototype", ex.getMessage());
        } catch (BuilderException ex) {
            log.error("Incomplete model", ex);
        }
    }

    private void updateItem(Node prototype, String oldPath, IFieldDescriptor newField) throws RepositoryException {
        if (newField != null) {
            ITypeDescriptor fieldType = newField.getTypeDescriptor();
            if (!newField.getPath().equals(oldPath) && !newField.getPath().equals("*") && !oldPath.equals("*")) {
                if (fieldType.isNode()) {
                    if (prototype.hasNode(oldPath)) {
                        Node child = prototype.getNode(oldPath);
                        child.getSession().move(child.getPath(), prototype.getPath() + "/" + newField.getPath());
                    }
                } else {
                    if (prototype.hasProperty(oldPath)) {
                        Property property = prototype.getProperty(oldPath);
                        if (property.getDefinition().isMultiple()) {
                            Value[] values = property.getValues();
                            property.remove();
                            if (newField.isMultiple()) {
                                prototype.setProperty(newField.getPath(), values);
                            } else if (values.length > 0) {
                                prototype.setProperty(newField.getPath(), values[0]);
                            }
                        } else {
                            Value value = property.getValue();
                            property.remove();
                            if (newField.isMultiple()) {
                                prototype.setProperty(newField.getPath(), new Value[] { value });
                            } else {
                                prototype.setProperty(newField.getPath(), value);
                            }
                        }
                    }
                }
            } else if (oldPath.equals("*") || newField.getPath().equals("*")) {
                log.warn("Wildcard fields are not supported");
            }
        } else {
            if (oldPath.equals("*")) {
                log
                        .warn("Removing wildcard fields is unsupported.  Items that fall under the definition will not be removed.");
            } else {
                if (prototype.hasNode(oldPath)) {
                    prototype.getNode(oldPath).remove();
                }
                if (prototype.hasProperty(oldPath)) {
                    prototype.getProperty(oldPath).remove();
                }
            }
        }
    }

    protected void processFieldAdded(IFieldDescriptor fieldDescriptor) {
        if (clusterConfig != null) {
            ITypeDescriptor fieldType = fieldDescriptor.getTypeDescriptor();

            String pluginName = TypeHelper.getFieldName(fieldDescriptor.getPath(), fieldDescriptor.getTypeDescriptor()
                    .getName());
            JavaPluginConfig pluginConfig = new JavaPluginConfig(pluginName);
            if (fieldType.isNode()) {
                pluginConfig.put("plugin.class", NodeFieldPlugin.class.getName());
            } else {
                pluginConfig.put("plugin.class", PropertyFieldPlugin.class.getName());
            }
            pluginConfig.put("wicket.id", getSelectedExtensionPoint());
            pluginConfig.put("wicket.model", "${wicket.model}");
            pluginConfig.put("validator.id", "${validator.id}");
            pluginConfig.put("mode", "${mode}");
            pluginConfig.put("engine", "${engine}");
            pluginConfig.put("field", fieldDescriptor.getName());
            pluginConfig.put("caption", fieldType.getName());

            getPlugins().add(pluginConfig);
        }
    }

    private void registerObservers() {
        if (clusterConfig != null) {
            context.registerService(clusterObserver = new IObserver<IClusterConfig>() {
                private static final long serialVersionUID = 1L;

                public IClusterConfig getObservable() {
                    return clusterConfig;
                }

                public void onEvent(Iterator<? extends IEvent<IClusterConfig>> events) {
                    while (events.hasNext()) {
                        IEvent event = events.next();
                        if (event instanceof ClusterConfigEvent) {
                            ClusterConfigEvent cce = (ClusterConfigEvent) event;
                            switch (cce.getType()) {
                            case PLUGIN_CHANGED:
                                // notify observers when the wicket hierarchy has changed
                                if (updatePluginCache()) {
                                    notifyObservers();
                                }
                                break;
                            case PLUGIN_ADDED:
                            case PLUGIN_REMOVED:
                                notifyObservers();
                                break;
                            }
                        }
                    }
                }

            }, IObserver.class.getName());
        }

    }

    private void unregisterObservers() {
        if (clusterObserver != null) {
            context.unregisterService(clusterObserver, IObserver.class.getName());
            clusterObserver = null;
        }
    }

    private void initPluginCache() {
        this.pluginCache = new TreeMap<String, IPluginConfig>();
        List<IPluginConfig> plugins = clusterConfig.getPlugins();
        for (IPluginConfig plugin : plugins) {
            JavaPluginConfig cache = new JavaPluginConfig();
            cache.put("wicket.id", plugin.get("wicket.id"));
            pluginCache.put(plugin.getName(), cache);
        }
    }

    private boolean updatePluginCache() {
        boolean changed = false;
        List<IPluginConfig> plugins = clusterConfig.getPlugins();
        for (IPluginConfig plugin : plugins) {
            IPluginConfig cache = pluginCache.get(plugin.getName());
            if (cache == null) {
                changed = true;
                break;
            }
            if (cache.getString("wicket.id") != null
                    && !cache.getString("wicket.id").equals(plugin.getString("wicket.id"))) {
                changed = true;
                break;
            }
        }
        if (changed) {
            initPluginCache();
            return true;
        }
        return false;
    }

    // IDetachable

    public void detach() {
        prototypeStore.detach();
        jcrTypeStore.detach();
        if (prototype != null) {
            prototype.detach();
        }
        if (clusterConfig != null && (clusterConfig instanceof IDetachable)) {
            ((IDetachable) clusterConfig).detach();
        }
    }

    // IObservable

    public void setObservationContext(IObservationContext context) {
        this.obContext = context;
    }

    public void startObservation() {
    }

    public void stopObservation() {
    }

    private void notifyObservers() {
        if (obContext != null) {
            EventCollection<IEvent<IObservable>> collection = new EventCollection<IEvent<IObservable>>();
            collection.add(new IEvent() {

                public IObservable getSource() {
                    return TemplateBuilder.this;
                }

            });
            obContext.notifyObservers(collection);
        }
    }

    private String getSelectedExtensionPoint() {
        return (String) selectedExtPtModel.getObject();
    }

}
