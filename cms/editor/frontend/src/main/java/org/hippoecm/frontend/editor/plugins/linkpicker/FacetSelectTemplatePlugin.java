/*
 *  Copyright 2008-2014 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IChainingModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.ClearableDialogLink;
import org.hippoecm.frontend.dialog.IDialogFactory;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FacetSelectTemplatePlugin extends RenderPlugin<Node> {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(FacetSelectTemplatePlugin.class);

    private static final String EMPTY_LINK_TEXT = "[...]";

    private final String mode;

    public FacetSelectTemplatePlugin(final IPluginContext context, IPluginConfig config) {
        super(context, config);

        Node node = getModelObject();
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

        final IModel<String> displayModel = new LoadableDetachableModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                Node node = FacetSelectTemplatePlugin.this.getModelObject();
                try {
                    if (node != null && node.hasProperty("hippo:docbase")) {
                        String docbaseUUID = node.getProperty("hippo:docbase").getString();
                        if (docbaseUUID == null || docbaseUUID.equals("") || docbaseUUID.startsWith("cafebabe-")) {
                            return EMPTY_LINK_TEXT;
                        }
                        return node.getSession().getNodeByUUID(docbaseUUID).getPath();
                    }
                } catch (ValueFormatException e) {
                    log.warn("Invalid value format for docbase " + e.getMessage());
                    log.debug("Invalid value format for docbase ", e);
                } catch (PathNotFoundException e) {
                    log.warn("Docbase not found " + e.getMessage());
                    log.debug("Docbase not found ", e);
                } catch (RepositoryException e) {
                    log.error("Invalid docbase" + e.getMessage(), e);
                }
                return EMPTY_LINK_TEXT;
            }
        };

        mode = config.getString("mode", "view");
        try {
            IDataProvider<Integer> provider = new IDataProvider<Integer>() {
                private static final long serialVersionUID = 1L;

                @Override
                public Iterator<Integer> iterator(long first, long count) {
                    return new Iterator<Integer>() {
                        int current = 0;

                        public boolean hasNext() {
                            try {
                                Node node = ((JcrNodeModel) FacetSelectTemplatePlugin.this.getDefaultModel()).getNode();
                                return current < node.getProperty("hippo:facets").getValues().length;
                            } catch (RepositoryException ex) {
                                return false;
                            }
                        }

                        public Integer next() {
                            if (hasNext()) {
                                return Integer.valueOf(current++);
                            } else {
                                throw new NoSuchElementException();
                            }
                        }

                        public void remove() {
                            throw new UnsupportedOperationException();
                        }
                    };
                }

                @Override
                public long size() {
                    try {
                        Node node = ((JcrNodeModel) FacetSelectTemplatePlugin.this.getDefaultModel()).getNode();
                        return node.getProperty("hippo:facets").getValues().length;
                    } catch (RepositoryException ex) {
                        return 0;
                    }
                }

                @Override
                public IModel<Integer> model(Integer object) {
                    return new Model<Integer>(object);
                }

                @Override
                public void detach() {
                }
            };
            if ("edit".equals(mode)) {
                final JcrPropertyValueModel<String> docbaseModel = new JcrPropertyValueModel<String>(new JcrPropertyModel<String>(node
                        .getProperty("hippo:docbase")));

                IDialogFactory dialogFactory = new IDialogFactory() {
                    private static final long serialVersionUID = 1L;

                    public AbstractDialog<String> createDialog() {
                        final IPluginConfig dialogConfig = LinkPickerDialogConfig.fromPluginConfig(getPluginConfig(), docbaseModel);
                        return new LinkPickerDialog(context, dialogConfig, new IChainingModel<String>() {
                            private static final long serialVersionUID = 1L;

                            public String getObject() {
                                return docbaseModel.getObject();
                            }

                            public void setObject(String object) {
                                docbaseModel.setObject(object);
                                redraw();
                            }

                            public IModel<String> getChainedModel() {
                                return docbaseModel;
                            }

                            public void setChainedModel(IModel<?> model) {
                                throw new UnsupportedOperationException("Value model cannot be changed");
                            }

                            public void detach() {
                                docbaseModel.detach();
                            }
                        });
                    }
                };

                add(new ClearableDialogLink("docbase", displayModel, dialogFactory, getDialogService()) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClear() {
                        Node node = ((JcrNodeModel) FacetSelectTemplatePlugin.this.getDefaultModel()).getNode();
                        try {
                            node.setProperty("hippo:docbase", node.getSession().getRootNode().getUUID());
                        } catch (RepositoryException e) {
                            log.error("Unable to reset docbase to rootnode uuid", e);
                        }
                        redraw();
                    }

                    @Override
                    public boolean isClearVisible() {
                        // Checking for string literals ain't pretty. It's probably better to create a better display model.
                        return !EMPTY_LINK_TEXT.equals((String) displayModel.getObject());
                    }
                });

                add(new DataView<Integer>("arguments", provider) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void populateItem(final Item<Integer> item) {
                        Node node = FacetSelectTemplatePlugin.this.getModelObject();
                        final int index = item.getModelObject().intValue();
                        try {
                            item.add(new TextFieldWidget("facet", new JcrPropertyValueModel<String>(index,
                                    new JcrPropertyModel<String>(node.getProperty("hippo:facets")))));
                            item.add(new TextFieldWidget("mode", new JcrPropertyValueModel<String>(index,
                                    new JcrPropertyModel<String>(node.getProperty("hippo:modes")))));
                            item.add(new TextFieldWidget("value", new JcrPropertyValueModel<String>(index,
                                    new JcrPropertyModel<String>(node.getProperty("hippo:values")))));
                            AjaxLink<Void> removeButton;
                            item.add(removeButton = new AjaxLink<Void>("remove") {
                                private static final long serialVersionUID = 1L;

                                @Override
                                public void onClick(AjaxRequestTarget target) {
                                    Node node = ((JcrNodeModel) FacetSelectTemplatePlugin.this.getDefaultModel())
                                            .getNode();
                                    for (String property : new String[] { "hippo:facets", "hippo:modes", "hippo:values" }) {
                                        try {
                                            Value[] oldValues = node.getProperty(property).getValues();
                                            Value[] newValues = new Value[oldValues.length - 1];
                                            System.arraycopy(oldValues, 0, newValues, 0, index);
                                            System.arraycopy(oldValues, index + 1, newValues, 0, oldValues.length
                                                    - index - 1);
                                            node.setProperty(property, newValues);
                                        } catch (RepositoryException ex) {
                                            log.error("cannot add new facet select line", ex);
                                        }
                                    }
                                    FacetSelectTemplatePlugin.this.redraw();
                                }
                            });
                            removeButton.setOutputMarkupId(true);
                        } catch (RepositoryException ex) {
                            log.error("cannot read facet select line", ex);
                        }
                    }
                });
                AjaxLink<Void> addButton;
                add(addButton = new AjaxLink<Void>("add") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        Node node = ((JcrNodeModel) FacetSelectTemplatePlugin.this.getDefaultModel()).getNode();
                        for (String property : new String[] { "hippo:facets", "hippo:modes", "hippo:values" }) {
                            try {
                                Value[] oldValues = node.getProperty(property).getValues();
                                Value[] newValues = new Value[oldValues.length + 1];
                                System.arraycopy(oldValues, 0, newValues, 0, oldValues.length);
                                newValues[newValues.length - 1] = node.getSession().getValueFactory().createValue("");
                                node.setProperty(property, newValues);
                            } catch (RepositoryException ex) {
                                log.error("cannot add new facet select line", ex);
                            }
                        }
                        FacetSelectTemplatePlugin.this.redraw();
                    }
                });
                addButton.setOutputMarkupId(true);
            } else {
                add(new Label("docbase", displayModel));
                add(new DataView<Integer>("arguments", provider) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void populateItem(final Item<Integer> item) {
                        try {
                            Node node = FacetSelectTemplatePlugin.this.getModelObject();
                            int index = item.getModelObject().intValue();
                            item
                                    .add(new Label("facet", node.getProperty("hippo:facets").getValues()[index]
                                            .getString()));
                            item.add(new Label("mode", node.getProperty("hippo:modes").getValues()[index].getString()));
                            item
                                    .add(new Label("value", node.getProperty("hippo:values").getValues()[index]
                                            .getString()));
                            Label removeButton;
                            item.add(removeButton = new Label("remove"));
                            removeButton.setVisible(false);
                        } catch (RepositoryException ex) {
                            log.error("cannot read facet select line", ex);
                        }
                    }
                });
                Label addButton;
                add(addButton = new Label("add"));
                addButton.setVisible(false);
            }
        } catch (PathNotFoundException ex) {
            log.error("failed to read existing facet select", ex);
        } catch (ValueFormatException ex) {
            log.error("failed to read existing facet select", ex);
        } catch (RepositoryException ex) {
            log.error("failed to read existing facet select", ex);
        }

        setOutputMarkupId(true);
    }
}
