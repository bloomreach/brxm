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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.collections.MiniMap;
import org.hippoecm.editor.tools.JcrPrototypeStore;
import org.hippoecm.editor.tools.JcrTypeStore;
import org.hippoecm.frontend.editor.impl.JcrTemplateStore;
import org.hippoecm.frontend.editor.plugins.field.NodeFieldPlugin;
import org.hippoecm.frontend.editor.plugins.field.PropertyFieldPlugin;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObservationContext;
import org.hippoecm.frontend.model.ocm.IStore;
import org.hippoecm.frontend.model.ocm.StoreException;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IClusterConfigListener;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.frontend.types.JavaFieldDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemplateTypeEditorHelper implements IDetachable, IObservable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(TemplateTypeEditorHelper.class);

    private String type;
    private IStore<IClusterConfig> templateStore;
    private IStore<ITypeDescriptor> typeStore;
    private JcrPrototypeStore prototypeStore;

    private ITypeDescriptor typeDescriptor;
    private IClusterConfig clusterConfig;
    private JcrNodeModel prototype;
    private Map<String, String> paths;
    private transient Map<String, IFieldDescriptor> removedFields;

    private IObservationContext observationContext;

    public TemplateTypeEditorHelper(String type, IPluginContext context) {
        this.type = type;
        this.typeStore = new JcrTypeStore(context);
        this.templateStore = new JcrTemplateStore(typeStore, context);
        this.prototypeStore = new JcrPrototypeStore();
    }

    public String getName() {
        return type;
    }

    public ITypeDescriptor getTypeDescriptor() {
        if (typeDescriptor == null) {
            typeDescriptor = typeStore.load(type);
            typeDescriptor.addTypeListener(new ITypeDescriptor.ITypeListener() {
                private static final long serialVersionUID = 1L;

                public void fieldAdded(String field) {
                    IClusterConfig template = getTemplate();

                    IFieldDescriptor fieldDescriptor = typeDescriptor.getField(field);
                    ITypeDescriptor fieldType = typeStore.load(fieldDescriptor.getType());

                    String pluginName = UUID.randomUUID().toString();
                    JavaPluginConfig pluginConfig = new JavaPluginConfig(pluginName);
                    if (fieldType.isNode()) {
                        pluginConfig.put("plugin.class", NodeFieldPlugin.class.getName());
                    } else {
                        pluginConfig.put("plugin.class", PropertyFieldPlugin.class.getName());
                    }
                    pluginConfig.put("wicket.id", "${cluster.id}.field");
                    pluginConfig.put("wicket.model", "${wicket.model}");
                    pluginConfig.put("mode", "${mode}");
                    pluginConfig.put("engine", "${engine}");
                    pluginConfig.put("field", field);
                    pluginConfig.put("caption", new String[] { fieldType.getName() });

                    template.getPlugins().add(pluginConfig);

                    updatePrototype();
                }

                public void fieldRemoved(String field) {
                    updatePrototype();
                }

            });
        }
        return typeDescriptor;
    }

    public IClusterConfig getTemplate() {
        if (clusterConfig == null) {
            final ITypeDescriptor type = getTypeDescriptor();

            @SuppressWarnings("unchecked")
            Map<String, Object> criteria = new MiniMap(1);
            criteria.put("type", type);
            Iterator<IClusterConfig> iter = templateStore.find(criteria);
            if (iter.hasNext()) {
                clusterConfig = iter.next();
                clusterConfig.addClusterConfigListener(new IClusterConfigListener() {
                    private static final long serialVersionUID = 1L;

                    public void onPluginAdded(IPluginConfig config) {
                        if (removedFields != null && config.getString("field") != null) {
                            String field = config.getString("field");
                            IFieldDescriptor descriptor = removedFields.remove(field);
                            if (descriptor != null) {
                                type.addField(descriptor);
                            }
                        }
                        notifyObservers();
                    }

                    public void onPluginRemoved(IPluginConfig config) {
                        // assume that the plugin is a fieldplugin
                        // TODO: check plugin class to verify this
                        if (config.getString("field") != null) {
                            String field = config.getString("field");
                            if (removedFields == null) {
                                removedFields = new TreeMap<String, IFieldDescriptor>();
                            }
                            removedFields.put(field, new JavaFieldDescriptor(type.getField(field)));
                            type.removeField(config.getString("field"));
                        }
                        notifyObservers();
                    }

                    public void onPluginChanged(IPluginConfig config) {
                        notifyObservers();
                    }

                });
            } else {
                log.error("No template found to display type");
            }
        }
        return clusterConfig;
    }

    public JcrNodeModel getPrototype() {
        if (prototype == null) {
            prototype = prototypeStore.getPrototype(type, true);
            paths = new HashMap<String, String>();
            for (Map.Entry<String, IFieldDescriptor> entry : getTypeDescriptor().getFields().entrySet()) {
                paths.put(entry.getKey(), entry.getValue().getPath());
            }
        }
        return prototype;
    }

    public void save() {
        try {
            ITypeDescriptor type = getTypeDescriptor();
            if (type != null) {
                typeStore.save(type);
            }
            IClusterConfig template = getTemplate();
            if (template != null) {
                templateStore.save(template);
            }
            JcrNodeModel nodeModel = getPrototype();
            if (nodeModel != null) {
                try {
                    nodeModel.getNode().save();
                } catch (RepositoryException ex) {
                    log.error(ex.getMessage());
                }
            }
        } catch (StoreException ex) {
            log.error(ex.getMessage());
        }
    }

    private void notifyObservers() {
        if (observationContext != null) {
            observationContext.notifyObservers(new IEvent() {

                public IObservable getSource() {
                    return TemplateTypeEditorHelper.this;
                }

            });
        }
    }

    private void updatePrototype() {
        ITypeDescriptor typeModel = getTypeDescriptor();
        IModel prototypeModel = getPrototype();
        if (prototypeModel == null) {
            return;
        }

        try {
            Node prototype = (Node) prototypeModel.getObject();
            if (prototype != null && typeModel != null) {
                Map<String, String> oldFields = paths;
                paths = new HashMap<String, String>();
                for (Map.Entry<String, IFieldDescriptor> entry : typeModel.getFields().entrySet()) {
                    paths.put(entry.getKey(), entry.getValue().getPath());
                }

                for (Map.Entry<String, String> entry : oldFields.entrySet()) {
                    String oldPath = entry.getValue();
                    IFieldDescriptor newField = typeModel.getField(entry.getKey());
                    if (newField != null) {
                        ITypeDescriptor fieldType = typeStore.load(newField.getType());
                        if (!newField.getPath().equals(oldPath) && !newField.getPath().equals("*")
                                && !oldPath.equals("*")) {
                            if (fieldType.isNode()) {
                                if (prototype.hasNode(oldPath)) {
                                    Node child = prototype.getNode(oldPath);
                                    child.getSession().move(child.getPath(),
                                            prototype.getPath() + "/" + newField.getPath());
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
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
    }

    public void setObservationContext(IObservationContext context) {
        this.observationContext = context;
    }

    public void startObservation() {
    }

    public void stopObservation() {
    }

    public void detach() {
        prototypeStore.detach();
    }

}
