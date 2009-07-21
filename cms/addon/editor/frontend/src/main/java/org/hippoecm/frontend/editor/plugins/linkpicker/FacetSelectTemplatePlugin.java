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
package org.hippoecm.frontend.editor.plugins.linkpicker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IChainingModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogLink;
import org.hippoecm.frontend.dialog.IDialogFactory;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.widgets.TextFieldWidget;

public class FacetSelectTemplatePlugin extends RenderPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(FacetSelectTemplatePlugin.class);

    private final String mode;

    public FacetSelectTemplatePlugin(final IPluginContext context, IPluginConfig config) {
        super(context, config);

        JcrNodeModel jcrNodeModel = (JcrNodeModel) getModel();
        Node node = jcrNodeModel.getNode();
        try {
            if (!node.hasProperty("hippo:docbase")) {
                node.setProperty("hippo:docbase", node.getSession().getRootNode().getUUID());
            }
            if (!node.hasProperty("hippo:facets")) {
                node.setProperty("hippo:facets", new String[0]);
            }
            if (!node.hasProperty("hippo:values")) {
                node.setProperty("hippo:values", new String[0]);
            }
            if (!node.hasProperty("hippo:modes")) {
                node.setProperty("hippo:modes", new String[0]);
            }
        } catch (ValueFormatException e) {
            log.error(e.getMessage());
        } catch (PathNotFoundException e) {
            log.error(e.getMessage());
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
        
        IModel displayModel = new Model() {
            private static final long serialVersionUID = 1L;

            @Override
            public Object getObject() {
                Node node = ((JcrNodeModel) FacetSelectTemplatePlugin.this.getModel()).getNode();
                try {
                    String docbaseUUID = node.getProperty("hippo:docbase").getString();
                    if (docbaseUUID == null || docbaseUUID.equals("") || docbaseUUID.startsWith("cafebabe-")) {
                        return "[...]";
                    }
                    return node.getSession().getNodeByUUID(docbaseUUID).getPath();
                } catch (ValueFormatException e) {
                    log.warn("Invalid value format for docbase " + e.getMessage());
                    log.debug("Invalid value format for docbase ", e);
                } catch (PathNotFoundException e) {
                    log.warn("Docbase not found " + e.getMessage());
                    log.debug("Docbase not found ", e);
                } catch (RepositoryException e) {
                    log.error("Invalid docbase" + e.getMessage(), e);
                }
                return "[...]";
            }
        };

        mode = config.getString("mode", "view");
        try {
            IDataProvider provider = new IDataProvider() {
                public Iterator iterator(int first, int count) {
                    return new Iterator() {
                        int current = 0;
                        public boolean hasNext() {
                            try {
                                Node node = ((JcrNodeModel) FacetSelectTemplatePlugin.this.getModel()).getNode();
                                return current < node.getProperty("hippo:facets").getValues().length;
                            } catch(RepositoryException ex) {
                                return false;
                            }
                        }
                        public Object next() {
                            if(hasNext()) {
                                return new Integer(current++);
                            } else {
                                throw new NoSuchElementException();
                            }
                        }
                        public void remove() {
                            throw new UnsupportedOperationException();
                        }
                    };
                }
                public int size() {
                    try {
                        Node node = ((JcrNodeModel) FacetSelectTemplatePlugin.this.getModel()).getNode();
                        return node.getProperty("hippo:facets").getValues().length;
                    } catch(RepositoryException ex) {
                        return 0;
                    }
                }
                public IModel model(Object object) {
                    return new Model(((Integer)object).intValue());
                }
                public void detach() {
                }
            };
            if("edit".equals(mode)) {
                final List<String> nodetypes = new ArrayList<String>();
                if (config.getStringArray("nodetypes") != null) {
                    String[] nodeTypes = config.getStringArray("nodetypes");
                    nodetypes.addAll(Arrays.asList(nodeTypes));
                }
                if (nodetypes.size() == 0) {
                    log.debug("No configuration specified for filtering on nodetypes. No filtering will take place.");
                }
                final JcrPropertyValueModel docbaseModel = new JcrPropertyValueModel(new JcrPropertyModel(node.getProperty("hippo:docbase")));
                //add(new TextFieldWidget("docbase", docbaseModel));
                IDialogFactory dialogFactory = new IDialogFactory() {
                    private static final long serialVersionUID = 1L;

                    public AbstractDialog createDialog() {
                        return new LinkPickerDialog(context, getPluginConfig(), new IChainingModel() {
                            private static final long serialVersionUID = 1L;

                            public Object getObject() {
                                return docbaseModel.getObject();
                            }

                            public void setObject(Object object) {
                                docbaseModel.setObject(object);
                                redraw();
                            }

                            public IModel getChainedModel() {
                                return docbaseModel;
                            }

                            public void setChainedModel(IModel model) {
                                throw new UnsupportedOperationException("Value model cannot be changed");
                            }

                            public void detach() {
                                docbaseModel.detach();
                            }
                        }, nodetypes);
                    }
                };
                add(new DialogLink("docbase", displayModel, dialogFactory, getDialogService()));

                add(new DataView("arguments", provider) {
                    public void populateItem(final Item item) {
                            Node node = ((JcrNodeModel) FacetSelectTemplatePlugin.this.getModel()).getNode();
                            final int index = ((Integer) item.getModelObject()).intValue();
                            try {
                                item.add(new TextFieldWidget("facet", new JcrPropertyValueModel(index, null, new JcrPropertyModel(node.getProperty("hippo:facets")))));
                                item.add(new TextFieldWidget("mode", new JcrPropertyValueModel(index, null, new JcrPropertyModel(node.getProperty("hippo:modes")))));
                                item.add(new TextFieldWidget("value", new JcrPropertyValueModel(index, null, new JcrPropertyModel(node.getProperty("hippo:values")))));
                                AjaxLink removeButton;
                                item.add(removeButton = new AjaxLink("remove") {
                                    public void onClick(AjaxRequestTarget target) {
                                        Node node = ((JcrNodeModel)FacetSelectTemplatePlugin.this.getModel()).getNode();
                                        for (String property : new String[] {"hippo:facets", "hippo:modes", "hippo:values"}) {
                                            try {
                                                Value[] oldValues = node.getProperty(property).getValues();
                                                Value[] newValues = new Value[oldValues.length - 1];
                                                System.arraycopy(oldValues, 0, newValues, 0, index);
                                                System.arraycopy(oldValues, index+1, newValues, 0, oldValues.length-index-1);
                                                node.setProperty(property, newValues);
                                            } catch (RepositoryException ex) {
                                                log.error("cannot add new facet select line", ex);
                                            }
                                        }
                                        FacetSelectTemplatePlugin.this.redraw();
                                    }
                                });
                                removeButton.setOutputMarkupId(true);
                            } catch(RepositoryException ex) {
                                log.error("cannot read facet select line", ex);
                            }
                        }
                    });
                AjaxLink addButton;
                add(addButton = new AjaxLink("add") {
                    public void onClick(AjaxRequestTarget target) {
                        Node node = ((JcrNodeModel) FacetSelectTemplatePlugin.this.getModel()).getNode();
                        for(String property : new String[] { "hippo:facets", "hippo:modes", "hippo:values"}) {
                            try {
                                Value[] oldValues = node.getProperty(property).getValues();
                                Value[] newValues = new Value[oldValues.length + 1];
                                System.arraycopy(oldValues, 0, newValues, 0, oldValues.length);
                                newValues[newValues.length - 1] = node.getSession().getValueFactory().createValue("");
                                node.setProperty(property, newValues);
                            } catch(RepositoryException ex) {
                                log.error("cannot add new facet select line", ex);
                            }
                        }
                        FacetSelectTemplatePlugin.this.redraw();
                    }
                });
                addButton.setOutputMarkupId(true);
            } else {
                add(new Label("docbase", displayModel));
                add(new DataView("arguments", provider) {
                    public void populateItem(final Item item) {
                            try {
                                Node node = ((JcrNodeModel)FacetSelectTemplatePlugin.this.getModel()).getNode();
                                int index = ((Integer)item.getModelObject()).intValue();
                                item.add(new Label("facet", node.getProperty("hippo:facets").getValues()[index].getString()));
                                item.add(new Label("mode", node.getProperty("hippo:modes").getValues()[index].getString()));
                                item.add(new Label("value", node.getProperty("hippo:values").getValues()[index].getString()));
                                Label removeButton;
                                item.add(removeButton = new Label("remove"));
                                removeButton.setVisible(false);
                            } catch(RepositoryException ex) {
                                log.error("cannot read facet select line", ex);
                            }
                        }
                    });
                Label addButton;
                add(addButton = new Label("add"));
                addButton.setVisible(false);
            }
        } catch(PathNotFoundException ex) {
            log.error("failed to read existing facet select", ex);
        } catch(ValueFormatException ex) {
            log.error("failed to read existing facet select", ex);
        } catch(RepositoryException ex) {
            log.error("failed to read existing facet select", ex);
        }

        setOutputMarkupId(true);
    }
}
