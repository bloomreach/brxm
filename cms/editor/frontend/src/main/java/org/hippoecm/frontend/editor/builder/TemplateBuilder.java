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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.ItemDefinition;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.jackrabbit.JcrConstants;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.collections.MiniMap;
import org.hippoecm.editor.prototype.JcrPrototypeStore;
import org.hippoecm.editor.template.BuiltinTemplateStore;
import org.hippoecm.editor.template.JcrTemplateStore;
import org.hippoecm.editor.template.TemplateLocator;
import org.hippoecm.editor.type.JcrDraftStore;
import org.hippoecm.editor.type.JcrTypeStore;
import org.hippoecm.frontend.editor.plugins.field.NodeFieldPlugin;
import org.hippoecm.frontend.editor.plugins.field.PropertyFieldPlugin;
import org.hippoecm.frontend.editor.plugins.mixin.MixinLoaderPlugin;
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
import org.hippoecm.frontend.plugin.config.impl.AbstractPluginDecorator;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.types.BuiltinTypeStore;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.frontend.types.ITypeLocator;
import org.hippoecm.frontend.types.JavaFieldDescriptor;
import org.hippoecm.frontend.types.TypeException;
import org.hippoecm.frontend.types.TypeHelper;
import org.hippoecm.frontend.types.TypeLocator;
import org.hippoecm.repository.api.HippoNodeType;
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
            try {
                updateItem(getPrototype().getObject(), delegate.getPath(), delegate);
            } catch (RepositoryException ex) {
                log.error("Failed to update prototype", ex);
            } catch (BuilderException ex) {
                log.error("Failed to find prototype when updating the path", ex);
            }
        }

        public void setOrdered(boolean isOrdered) {
            delegate.setOrdered(isOrdered);
        }

        public void setPath(String path) throws TypeException {
            if (!"*".equals(path)) {
                for (IFieldDescriptor sibling : typeDescriptor.getFields().values()) {
                    if (!sibling.getName().equals(delegate.getName())) {
                        if (sibling.getPath().equals(path)) {
                            throw new TypeException("A field with path " + path + " already exists");
                        }
                    }
                }
                if (path.contains("-")) {
                    throw new TypeException("Hyphens are not allowed in field path");
                }
            }

            String oldPath = delegate.getPath();
            delegate.setPath(path);
            try {
                IModel<Node> prototypeModel = getPrototype();
                Node prototype = prototypeModel.getObject();
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
            boolean updateFieldName = false;
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
                updateFieldName = true;
            } else {
                paths.put(name, path);
            }
            boolean containsNewName = false;
            int position = -1;
            List<IPluginConfig> newPlugins = new LinkedList<IPluginConfig>();
            List<IPluginConfig> plugins = clusterConfig.getPlugins();
            for (int i = 0; i < plugins.size(); i++) {
                IPluginConfig plugin = plugins.get(i);
                if (plugin.containsKey("field") && name.equals(plugin.getString("field"))) {
                    if (updateFieldName) {
                        plugin.put("field", newName);
                    }
                    position = i;
                    JavaPluginConfig newPlugin = new JavaPluginConfig(newName);
                    newPlugin.putAll(plugins.get(position));
                    newPlugins.add(newPlugin);
                } else {
                    newPlugins.add(plugin);
                }
                if (newName.equals(plugin.getName())) {
                    containsNewName = true;
                }
            }
            if (!containsNewName) {
                clusterConfig.setPlugins(newPlugins);
                pluginModel.setObject(newName);
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

        public void addField(IFieldDescriptor descriptor) throws TypeException {
            for (IFieldDescriptor field : getFields().values()) {
                if (!"*".equals(field.getPath())) {
                    if (field.getPath().equals(descriptor.getPath())) {
                        throw new TypeException("Path " + descriptor.getPath() + " already exists, not adding field");
                    }
                } else if ("*".equals(descriptor.getPath())) {
                    if (field.getTypeDescriptor().getType().equals(descriptor.getTypeDescriptor().getType())) {
                        throw new TypeException("Path " + descriptor.getPath() + " already exists, not adding field");
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

        public void removeField(String name) throws TypeException {
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
            List<String> currentTypes = typeDescriptor.getSuperTypes();
            typeDescriptor.setSuperTypes(superTypes);
            for (String superType : superTypes) {
                if (!currentTypes.contains(superType)) {
                    processMixinAdded(superType);
                }
            }
            updatePrototype();
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

    class BuilderPluginList extends AbstractList<IPluginConfig> implements IDetachable {
        private static final long serialVersionUID = 1L;

        @Override
        public IPluginConfig get(int index) {
            return clusterConfig.getPlugins().get(index);
        }

        @Override
        public void add(int index, IPluginConfig config) {
            List<IPluginConfig> plugins = new LinkedList<IPluginConfig>(clusterConfig.getPlugins());
            plugins.add(index, config);
            clusterConfig.setPlugins(plugins);
        }

        @Override
        public int size() {
            return clusterConfig.getPlugins().size();
        }

        void replaceAll(List<IPluginConfig> configs) {
            Set<String> names = new TreeSet<String>();
            for (IPluginConfig plugin : configs) {
                names.add(plugin.getName());
            }

            boolean doUpdate = false;
            List<IPluginConfig> plugins = new LinkedList<IPluginConfig>(clusterConfig.getPlugins());
            for (IPluginConfig plugin : plugins) {
                if (!names.contains(plugin.getName())) {
                    String field = plugin.getString("field");
                    if (field != null) {
                        try {
                            typeDescriptor.removeField(field);
                        } catch (TypeException e) {
                            log.error("Failed to remove field from type", e);
                        }
                        doUpdate = true;
                    } else {
                        String mixin = plugin.getString("mixin");
                        if (mixin != null) {
                            try {
                                NodeType nt = getJcrNodeType(typeDescriptor.getType());
                                if (!nt.isNodeType(mixin)) {
                                    List<String> superTypes = typeDescriptor.getSuperTypes();
                                    superTypes.remove(mixin);
                                    typeDescriptor.setSuperTypes(superTypes);
                                }
                            } catch (RepositoryException e) {
                                log.error("Unable to check whether removed mixin " + mixin + " is part of the JCR primary node type", e);
                            }
                            doUpdate = true;
                        }
                    }
                    
                }
            }
            if (doUpdate) {
                updatePrototype();
            }

            clusterConfig.setPlugins(configs);
        }

        public void detach() {
            for (IPluginConfig config : clusterConfig.getPlugins()) {
                if (config instanceof IDetachable) {
                    ((IDetachable) config).detach();
                }
            }
        }

    }

    private class BuilderClusterDecorator extends AbstractPluginDecorator implements IClusterConfig {
        private static final long serialVersionUID = 1L;

        private IObserver<IClusterConfig> observer;

        private BuilderClusterDecorator(IClusterConfig upstream) {
            super(upstream);
        }

        public List<IPluginConfig> getPlugins() {
            return Collections.unmodifiableList(TemplateBuilder.this.getPlugins());
        }

        public void setPlugins(List<IPluginConfig> plugins) {
            TemplateBuilder.this.getPlugins().replaceAll(plugins);
        }

        @Override
        protected Object decorate(Object object) {
            return object;
        }

        public List<String> getProperties() {
            return ((IClusterConfig) upstream).getProperties();
        }

        public List<String> getReferences() {
            return ((IClusterConfig) upstream).getReferences();
        }

        public List<String> getServices() {
            return ((IClusterConfig) upstream).getServices();
        }

        @SuppressWarnings("unchecked")
        @Override
        protected IObservationContext<IClusterConfig> getObservationContext() {
            return (IObservationContext<IClusterConfig>) super.getObservationContext();
        }

        @Override
        public void startObservation() {
            super.startObservation();
            final IObservationContext<IClusterConfig> obContext = getObservationContext();
            obContext.registerObserver(observer = new IObserver<IClusterConfig>() {
                private static final long serialVersionUID = 1L;

                public IClusterConfig getObservable() {
                    return (IClusterConfig) upstream;
                }

                public void onEvent(Iterator<? extends IEvent<IClusterConfig>> events) {
                    EventCollection<IEvent<IClusterConfig>> collection = new EventCollection<IEvent<IClusterConfig>>();
                    while (events.hasNext()) {
                        IEvent<IClusterConfig> event = events.next();
                        if (event instanceof ClusterConfigEvent) {
                            ClusterConfigEvent cce = (ClusterConfigEvent) event;
                            collection.add(new ClusterConfigEvent(BuilderClusterDecorator.this, wrapConfig(cce.getPlugin()), cce.getType()));
                        }
                    }
                    if (collection.size() > 0) {
                        obContext.notifyObservers(collection);
                    }
                }

            });
        }

        @Override
        public void stopObservation() {
            IObservationContext<IClusterConfig> obContext = getObservationContext();
            obContext.unregisterObserver(observer);
            super.stopObservation();
        }
    }

    private String type;
    private boolean readonly;
    private IPluginContext context;
    private IModel selectedExtPtModel;
    private IModel<String> pluginModel;

    private IStore<IClusterConfig> jcrTemplateStore;
    private BuiltinTemplateStore builtinTemplateStore;

    private JcrTypeStore jcrTypeStore;
    private ITypeLocator typeLocator;

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

    public TemplateBuilder(final String type, boolean readonly, IPluginContext context, IModel extPtModel, IModel<String> pluginModel)
            throws BuilderException {
        this.type = type;
        this.readonly = readonly;
        this.context = context;
        this.selectedExtPtModel = extPtModel;
        this.pluginModel = pluginModel;

        jcrTypeStore = new JcrTypeStore();
        String prefix;
        if (type.indexOf(':') > 0) {
            prefix = type.substring(0, type.indexOf(':'));
        } else {
            prefix = "system";
        }
        IStore draftStore = new JcrDraftStore(jcrTypeStore, prefix);
        BuiltinTypeStore builtinTypeStore = new BuiltinTypeStore();
        typeLocator = new TypeLocator(new IStore[]{draftStore, jcrTypeStore, builtinTypeStore});
        builtinTypeStore.setTypeLocator(typeLocator);
        jcrTypeStore.setTypeLocator(typeLocator);

        this.jcrTemplateStore = new JcrTemplateStore(typeLocator);
        this.builtinTemplateStore = new BuiltinTemplateStore(typeLocator);
        builtinTemplateStore.setTemplateLocator(new TemplateLocator(new IStore[]{jcrTemplateStore}));

        this.prototypeStore = new JcrPrototypeStore();

        try {
            typeDescriptor = jcrTypeStore.getDraftType(type);
            if (typeDescriptor == null) {
                if (readonly) {
                    typeDescriptor = jcrTypeStore.load(type);
                } else {
                    throw new BuilderException("No draft found for type " + type);
                }
            }

            try {
                currentTypeDescriptor = jcrTypeStore.load(type);
            } catch (StoreException ex) {
                try {
                    currentTypeDescriptor = builtinTypeStore.load(type);
                } catch (StoreException ex2) {
                    // ignore
                }
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

    public ITypeDescriptor getTypeDescriptor() {
        return new BuilderTypeDescriptor();
    }

    protected final BuilderPluginList getPlugins() {
        if (plugins == null) {
            plugins = new BuilderPluginList();
        }
        return plugins;
    }

    public IClusterConfig getTemplate() throws BuilderException {
        if (clusterConfig != null) {
            return new BuilderClusterDecorator(clusterConfig);
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
                Set<String> currentTypes = new HashSet<String>();
                if (prototype.hasProperty(JcrConstants.JCR_MIXINTYPES)) {
                    final Value[] currentNodeTypes = prototype.getProperty(JcrConstants.JCR_MIXINTYPES).getValues();
                    for (Value nt : currentNodeTypes) {
                        currentTypes.add(nt.getString());
                    }
                }
                final List<String> superTypes = typeDescriptor.getSuperTypes();
                for (String superType : superTypes) {
                    final NodeType nodeType = getJcrNodeType(superType);
                    if (!nodeType.isMixin()) {
                        continue;
                    }
                    if (!currentTypes.contains(superType)) {
                        prototype.addMixin(superType);
                    }
                }
                for (String currentType : currentTypes) {
                    if (HippoNodeType.NT_HARDDOCUMENT.equals(currentType)) {
                        continue;
                    }
                    if (!superTypes.contains(currentType)) {
                        prototype.removeMixin(currentType);
                    }
                }

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

    private NodeType getJcrNodeType(final String superType) throws RepositoryException {
        return UserSession.get().getJcrSession().getWorkspace().getNodeTypeManager().getNodeType(superType);
    }

    private void updateItem(Node prototype, String oldPath, IFieldDescriptor newField) throws RepositoryException {
        if (newField != null) {
            ITypeDescriptor fieldType = newField.getTypeDescriptor();
            ItemDefinition itemDefinition = null;
            boolean multiple = false;
            if (fieldType.isNode() && prototype.hasNode(oldPath)) {
                itemDefinition = prototype.getNode(oldPath).getDefinition();
                multiple = ((NodeDefinition) itemDefinition).allowsSameNameSiblings();
            } else if (!fieldType.isNode() && prototype.hasProperty(oldPath)) {
                itemDefinition = prototype.getProperty(oldPath).getDefinition();
                multiple = ((PropertyDefinition) itemDefinition).isMultiple();
            }
            if (itemDefinition != null &&
                    ((!fieldType.isNode() && multiple != newField.isMultiple()) ||
                    (!newField.getPath().equals(oldPath) && !newField.getPath().equals("*") && !oldPath.equals("*")))) {
                if (fieldType.isNode()) {
                    Node child = prototype.getNode(oldPath);
                    child.getSession().move(child.getPath(), prototype.getPath() + "/" + newField.getPath());
                } else {
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
                            prototype.setProperty(newField.getPath(), new Value[]{value});
                        } else {
                            prototype.setProperty(newField.getPath(), value);
                        }
                    }
                }
            } else if ((oldPath.equals("*") || newField.getPath().equals("*"))
                    && !(oldPath.equals(newField.getPath()))) {
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
            pluginConfig.put("field", fieldDescriptor.getName());
            pluginConfig.put("caption", fieldType.getName());

            getPlugins().add(pluginConfig);

            pluginModel.setObject(pluginName);
        }
    }

    protected void processMixinAdded(String mixinTypeName) {
        if (clusterConfig != null) {
            String mixinName = mixinTypeName;
            mixinName = mixinName.substring(mixinName.indexOf(':') + 1);
            String pluginName = TypeHelper.getFieldName("mix:" + mixinName, null);

            JavaPluginConfig pluginConfig = new JavaPluginConfig(pluginName);
            pluginConfig.put("plugin.class", MixinLoaderPlugin.class.getName());
            pluginConfig.put("wicket.id", getSelectedExtensionPoint());
            pluginConfig.put("mixin", mixinTypeName);

            getPlugins().add(pluginConfig);

            pluginModel.setObject(pluginName);
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
        typeLocator.detach();
        detach(plugins);
        detach(prototype);
        detach(clusterConfig);
        detach(typeDescriptor);
        detach(currentTypeDescriptor);
    }

    private void detach(Object object) {
        if (object instanceof IDetachable) {
            ((IDetachable) object).detach();
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
