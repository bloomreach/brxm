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

package org.hippoecm.hst.plugins.frontend.editor.components;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.IRequestTarget;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.dialog.IDialogService.Dialog;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.hst.plugins.frontend.editor.BasicEditorPlugin;
import org.hippoecm.hst.plugins.frontend.editor.dao.ComponentDAO;
import org.hippoecm.hst.plugins.frontend.editor.dao.EditorDAO;
import org.hippoecm.hst.plugins.frontend.editor.dao.TemplateDAO;
import org.hippoecm.hst.plugins.frontend.editor.description.DescriptionPanel;
import org.hippoecm.hst.plugins.frontend.editor.domain.Component;
import org.hippoecm.hst.plugins.frontend.editor.domain.Template;
import org.hippoecm.hst.plugins.frontend.editor.domain.Component.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComponentEditorPlugin extends BasicEditorPlugin<Component> {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(ComponentEditorPlugin.class);

    private Fragment selected;
    private List<String> choices;

    public ComponentEditorPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        //TODO: test if all childnodes are created, if so disable add button

        form.add(new DescriptionPanel("componentDescription", form.getInnermostModel(), context, config));

        form.add(new AjaxCheckBox("reference") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                redraw();
            }
        });

        //Containers widget
        ListView containers = new ListView("parameters") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem item) {
                Parameter param = (Parameter) item.getModelObject();
                RequiredTextField keyField = new RequiredTextField("name", new PropertyModel(param, "name"));
                keyField.setOutputMarkupId(true);
                item.add(keyField);

                TextField value = new RequiredTextField("value", new PropertyModel(param, "value"));
                value.setOutputMarkupId(true);
                item.add(value);

                AjaxSubmitLink remove = new AjaxSubmitLink("remove") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form form) {
                        getBean().removeParameter(item.getIndex());//TEST
                        redraw();
                    }
                };
                remove.setDefaultFormProcessing(false);
                item.add(remove);
            }

        };
        containers.setReuseItems(true);
        form.add(containers);

        AjaxSubmitLink addParam = new AjaxSubmitLink("addParameter") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                getBean().addParameter();
                redraw();
            }

        };
        addParam.setDefaultFormProcessing(false);
        form.add(addParam);

        final List<Parameter> overrides = new ArrayList<Parameter>();
        final DropDownChoice ddo = new DropDownChoice("parameterOverrides", overrides) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return overrides.size() > 0;
            }
        };
        ddo.setNullValid(false);
        ddo.setOutputMarkupId(true);
        ddo.add(new AjaxFormComponentUpdatingBehavior("onChange") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                info("Override param");
            }
        });
        form.add(ddo);
    }

    private void checkState() {
        choices = getChoices();

        boolean set = choices != null && choices.size() > 0;
        if (addLink.isVisible() != set) {
            addLink.setVisible(set);
            IRequestTarget target = RequestCycle.get().getRequestTarget();
            if (target instanceof AjaxRequestTarget) {
                ((AjaxRequestTarget) target).addComponent(addLink);
            }
        }
    }

    private List<String> getChoices() {
        Component c = getBean();
        String templateName;
        if (c.isReference() && c.getReferenceName() != null) {
            Component refComponent = ((ComponentDAO) dao).resolveComponent(c);
            templateName = refComponent.getTemplate();
        } else {
            templateName = c.getTemplate();
        }

        JcrNodeModel template = new JcrNodeModel(hstContext.template.absolutePath(templateName));
        TemplateDAO tDao = new TemplateDAO(getPluginContext(), hstContext.template.getNamespace());
        Template t = tDao.load(template);

        List<String> choices = t.getContainers();
        if (choices != null) {
            Node parent = c.getModel().getNode();
            try {
                if (parent.hasNodes()) {
                    for (NodeIterator it = parent.getNodes(); it.hasNext();) {
                        String name = it.nextNode().getName();
                        if (choices.contains(name)) {
                            choices.remove(name);
                        }
                    }
                }
            } catch (RepositoryException e) {
                log.error("An error occured during filtering of existing child nodes", e);
            }
            if (choices.size() == 0) {
                addLink.setVisible(false);
            }
        }
        return choices;
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        setComponentForm();
        checkState();
    }

    private void setComponentForm() {
        Fragment f = getBean().isReference() ? new RefComponentFragment() : new ComponentFragment();
        if (selected == null || !selected.equals(f)) {
            form.addOrReplace(selected = f);
        }
    }

    class RefComponentFragment extends Fragment {
        private static final long serialVersionUID = 1L;

        public RefComponentFragment() {
            super("frag", "refComponent", ComponentEditorPlugin.this);

            final List<String> availableComponents = getReferenceableComponents();
            final DropDownChoice ddo = new DropDownChoice("referenceName", availableComponents, new IChoiceRenderer() {
                private static final long serialVersionUID = 1L;

                public Object getDisplayValue(Object object) {
                    //                    String name = (String) object;
                    //                    int split = name.indexOf('/');
                    //                    String prefix = name.substring(0, split);
                    //                    prefix = prefix.substring(prefix.indexOf(':') + 1);
                    //                    return prefix + ": " + name.substring(split + 1);
                    return object;
                }

                public String getIdValue(Object object, int index) {
                    return (String) object;
                }

            }) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean isVisible() {
                    return availableComponents.size() > 0;
                }
            };
            ddo.setNullValid(false);
            ddo.setOutputMarkupId(true);
            ddo.add(new AjaxFormComponentUpdatingBehavior("onChange") {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    info(ComponentEditorPlugin.this.getString("reference.changed"));
                }
            });

            add(ddo);
        }
    }

    class ComponentFragment extends Fragment {
        private static final long serialVersionUID = 1L;

        public ComponentFragment() {
            super("frag", "component", ComponentEditorPlugin.this);

            FormComponent fc;

            //Component Classname widget
            fc = new TextField("componentClassName");
            fc.setOutputMarkupId(true);
            add(fc);

            //Choose template widget
            List<String> templates = hstContext.template.getTemplatesAsList();

            final String originalTemplate = getBean().getTemplate();
            final DropDownChoice dc = new DropDownChoice("template", templates);
            dc.setNullValid(false);
            dc.setRequired(true);
            dc.setOutputMarkupId(true);
            dc.add(new AjaxFormComponentUpdatingBehavior("onChange") {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    if (!getBean().getTemplate().equals(originalTemplate)) {
                        info(ComponentEditorPlugin.this.getString("template.changed"));
                    }
                }
            });
            add(dc);
        }

    }

    @Override
    protected EditorDAO<Component> newDAO() {
        return new ComponentDAO(getPluginContext(), hstContext.component.getNamespace());
    }

    public List<String> getReferenceableComponents() {
        return hstContext.component.getReferenceables(true);
    }

    @Override
    protected Dialog newAddDialog() {
        return new AddRestrictedComponentDialog((ComponentDAO) dao, this, (JcrNodeModel) getModel(), choices);
    }

}
