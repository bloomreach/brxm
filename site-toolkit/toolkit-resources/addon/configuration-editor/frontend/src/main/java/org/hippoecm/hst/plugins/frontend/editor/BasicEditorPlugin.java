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

package org.hippoecm.hst.plugins.frontend.editor;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.hst.plugins.frontend.editor.domain.BeanProvider;
import org.hippoecm.hst.plugins.frontend.editor.domain.EditorBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BasicEditorPlugin<K extends EditorBean> extends EditorPlugin<K> implements BeanProvider<K> {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(BasicEditorPlugin.class);

    private final FeedbackPanel feedback;
    protected final Form form;
    protected K bean;

    public BasicEditorPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        bean = dao.load((JcrNodeModel) getModel());

        add(form = new Form("editor", new CompoundPropertyModel(new LoadableDetachableModel(bean) {
            private static final long serialVersionUID = 1L;

            @Override
            protected Object load() {
                if (getModel() instanceof JcrNodeModel) {
                    JcrNodeModel newModel = (JcrNodeModel) getModel();
                    JcrNodeModel oldModel = bean.getModel();
                    if (!newModel.equals(oldModel)) {
                        bean = dao.load(newModel);
                    }
                }
                return bean;
            }
        })));

        add(new AjaxSubmitLink("save", form) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                doSave();
            }
        });

        add(new AjaxLink("remove") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                doRemove();
            }
        });

        form.add(feedback = new FeedbackPanel("editor-feedback"));
        feedback.setOutputMarkupId(true);

        modelChanged();
    }

    @Override
    public void render(PluginRequestTarget target) {
        if (target != null && feedback.anyMessage()) {
            target.addComponent(feedback);
        }
        super.render(target);
    }

    protected void doSave() {
        if (dao.save(bean)) {
            setModel(bean.getModel());
            info("Node saved.");
        }
    }

    protected void doRemove() {
        try {
            final String name = bean.getModel().getNode().getName();
            JcrNodeModel nextModel = bean.getModel().getParentModel();
            if (dao.delete(bean)) {
                Node next = nextModel.getNode();
                if (next.hasNodes()) {
                    boolean found = false;
                    for (NodeIterator it = next.getNodes(); it.hasNext();) {
                        Node node = it.nextNode();
                        if (node.getName().compareTo(name) < 0) {
                            found = true;
                        } else if (found) {
                            break;
                        } else {
                            found = true;
                        }
                        next = node;
                    }
                }
                setModel(new JcrNodeModel(next));
                info(new StringResourceModel("node.removed", this, null, new Object[] { name }).getString());
            }
        } catch (RepositoryException e) {
            log.error("Failed to remove node, model = " + bean.getModel());
        }
    }

    public K getBean() {
        return bean;
    }

}
