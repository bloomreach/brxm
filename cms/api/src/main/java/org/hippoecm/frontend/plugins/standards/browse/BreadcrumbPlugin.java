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
package org.hippoecm.frontend.plugins.standards.browse;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.i18n.model.NodeTranslator;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.ServiceTracker;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.util.MaxLengthStringFormatter;
import org.hippoecm.repository.api.NodeNameCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class BreadcrumbPlugin extends RenderPlugin<Node> {
    private static final long serialVersionUID = 1L;


    static final Logger log = LoggerFactory.getLogger(BreadcrumbPlugin.class);

    private final Set<String> roots;
    private final AjaxButton up;

    private int maxNumberOfCrumbs;
    private MaxLengthStringFormatter format;
    private IModelReference<Node> folderReference;

    private List<NodeItem> nodeitems;

    public BreadcrumbPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        maxNumberOfCrumbs = config.getInt("max.breadcrumbs", 8);

        if (config.getString("model.folder") == null) {
            throw new IllegalArgumentException("Expected model.folder configuration key");
        }

        roots = new HashSet<String>();
        String[] paths = config.getStringArray("root.paths");
        if (paths != null) {
            for (String path : paths) {
                roots.add(path);
            }
        } else {
            roots.add("/");
        }

        context.registerTracker(new ServiceTracker<IModelReference>(IModelReference.class) {
            private static final long serialVersionUID = 1L;

            IObserver folderServiceObserver = null;

            @Override
            protected void onServiceAdded(IModelReference service, String name) {
                if (folderServiceObserver == null) {
                    folderReference = service;
                    context.registerService(folderServiceObserver = new IObserver<IModelReference<Node>>() {
                        private static final long serialVersionUID = 1L;

                        public IModelReference<Node> getObservable() {
                            return folderReference;
                        }

                        public void onEvent(Iterator<? extends IEvent<IModelReference<Node>>> event) {
                            update((JcrNodeModel) folderReference.getModel());
                        }

                    }, IObserver.class.getName());
                    update((JcrNodeModel) service.getModel());
                }
            }

            @Override
            protected void onRemoveService(IModelReference service, String name) {
                if (service == folderReference) {
                    context.unregisterService(folderServiceObserver, IObserver.class.getName());
                    folderServiceObserver = null;
                }
            }

        }, config.getString("model.folder"));

        add(getListView(null));

        up = new AjaxButton("up") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                JcrNodeModel model = (JcrNodeModel) folderReference.getModel();
                model = model.getParentModel();
                if (model != null) {
                    folderReference.setModel(model);
                }
            }
        };
        up.setModel(new StringResourceModel("dialog-breadcrumb-up", this, null));
        up.setEnabled(false);
        add(up);

        format = new MaxLengthStringFormatter(config.getInt("crumb.max.length", 10), config.getString("crumb.splitter",
                ".."), 0);
    }

    @Override
    protected void onDetach() {
        if (nodeitems != null) {
            for (NodeItem nodeItem : nodeitems) {
                nodeItem.detach();
            }
        }
        super.onDetach();
    }

    protected void update(JcrNodeModel model) {
        replace(getListView(model));
        setDefaultModel(model);

        JcrNodeModel parentModel = model.getParentModel();
        if (parentModel == null || roots.contains(model.getItemModel().getPath())) {
            up.setEnabled(false);
        } else {
            up.setEnabled(true);
        }
        AjaxRequestTarget.get().addComponent(this);
    }

    private ListView<NodeItem> getListView(JcrNodeModel model) {
        nodeitems = new LinkedList<NodeItem>();
        if (model != null) {
            //add current folder as disabled
            nodeitems.add(new NodeItem(model, false));
            if (!roots.contains(model.getItemModel().getPath())) {
                model = model.getParentModel();
                while (model != null) {
                    nodeitems.add(new NodeItem(model, true));
                    if (roots.contains(model.getItemModel().getPath())) {
                        model = null;
                    } else {
                        model = model.getParentModel();
                    }
                }
            }
        }
        Collections.reverse(nodeitems);
        ListView<NodeItem> listview = new ListView<NodeItem>("crumbs", nodeitems) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<NodeItem> item) {
                AjaxLink<NodeItem> link = new AjaxLink<NodeItem>("link", item.getModel()) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        folderReference.setModel(getModelObject().model);
                    }

                };

                link.add(new Label("name", new AbstractReadOnlyModel<String>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public String getObject() {
                        NodeItem nodeItem = item.getModelObject();
                        return (nodeItem.name != null ? format.parse(nodeItem.name) : null);
                    }

                }));
                link.add(new AttributeAppender("title", true, new LoadableDetachableModel<String>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected String load() {
                        return item.getModelObject().getName();
                    }
                }, " "));

                link.setEnabled(item.getModelObject().enabled);
                item.add(link);

                IModel<String> css = new LoadableDetachableModel<String>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected String load() {
                        String css = item.getModelObject().enabled ? "enabled" : "disabled";

                        if (nodeitems.size() == 1) {
                            css += " firstlast";
                        } else if (item.getIndex() == 0) {
                            css += " first";
                        } else if (item.getIndex() == (nodeitems.size() - 1)) {
                            css += " last";
                        }
                        return css;
                    }
                };
                item.add(new AttributeAppender("class", css, " "));

                item.setVisible(item.getIndex() < maxNumberOfCrumbs);
            }
        };
        return listview;
    }

    private static class NodeItem implements IDetachable {
        private static final long serialVersionUID = 1L;

        boolean enabled;
        JcrNodeModel model;
        String name;

        public NodeItem(JcrNodeModel model, boolean enabled) {
            this.name = new NodeTranslator(model).getNodeName().getObject();
            this.model = model;
            this.enabled = enabled;
        }

        public String getName() {
            return (name != null ? NodeNameCodec.decode(name) : null);
        }

        public void detach() {
            model.detach();
        }

    }

}
