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
import javax.jcr.RepositoryException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogLink;
import org.hippoecm.frontend.dialog.IDialogFactory;
import org.hippoecm.frontend.dialog.IDialogService.Dialog;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.hst.plugins.frontend.editor.BasicEditorPlugin;
import org.hippoecm.hst.plugins.frontend.editor.dao.ComponentDAO;
import org.hippoecm.hst.plugins.frontend.editor.dao.EditorDAO;
import org.hippoecm.hst.plugins.frontend.editor.dialogs.HstComponentPickerDialog;
import org.hippoecm.hst.plugins.frontend.editor.domain.Component;
import org.hippoecm.hst.plugins.frontend.editor.domain.Component.Parameter;
import org.hippoecm.hst.plugins.frontend.editor.validators.NodeUniqueValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComponentEditorPlugin extends BasicEditorPlugin<Component> {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(ComponentEditorPlugin.class);

    private Fragment selected;

    public ComponentEditorPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        AjaxCheckBox box = new AjaxCheckBox("reference") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                redraw();
            }

        };
        form.add(box);

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
                        bean.removeParameter(item.getIndex());//TEST
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
                bean.addParameter();
                redraw();
            }

        };
        addParam.setDefaultFormProcessing(false);
        form.add(addParam);

        final List<Parameter> overrides = new ArrayList<Parameter>();
        final DropDownChoice ddo = new DropDownChoice("parameterOverrides", overrides) {
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

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        setComponentForm();
    }

    private void setComponentForm() {
        Fragment f = bean.isReference() ? new RefComponentFragment() : new ComponentFragment();
        if (selected == null) {
            form.add(selected = f);
        } else if (!selected.equals(f)) {
            form.replace(selected = f);
        }
    }

    class RefComponentFragment extends Fragment {
        private static final long serialVersionUID = 1L;

        public RefComponentFragment() {
            super("frag", "refComponent", ComponentEditorPlugin.this);

            FormComponent fc;

            //Readonly name widget
            fc = new RequiredTextField("referenceName");
            fc.setOutputMarkupId(true);
            fc.setEnabled(false);
            //TODO: check if exists? add picker?
            //fc.add(new NodeUniqueValidator<Component>(ComponentEditorPlugin.this));
            add(fc);

            // Linkpicker
            final List<String> nodetypes = new ArrayList<String>();
            nodetypes.add("hst:component");

            IDialogFactory dialogFactory = new IDialogFactory() {
                private static final long serialVersionUID = 1L;

                public AbstractDialog createDialog() {
                    Model model = new Model(hstContext.component.absolutePath(bean.getReferenceName()));
                    return new HstComponentPickerDialog(getPluginContext(), getPluginConfig(), model, nodetypes) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected void saveNode(Node node) {
                            try {
                                bean.setReferenceName(hstContext.component.relativePath(node.getPath()));
                                redraw();
                            } catch (RepositoryException e) {
                                log.error(e.getMessage());
                            }
                        }
                    };
                }
            };

            DialogLink link = new DialogLink("referencePicker", new Model() {
                private static final long serialVersionUID = 1L;

                @Override
                public Object getObject() {
                    return "..";
                }
            }, dialogFactory, getDialogService());
            link.setOutputMarkupId(true);
            add(link);

        }

    }

    class ComponentFragment extends Fragment {
        private static final long serialVersionUID = 1L;

        public ComponentFragment() {
            super("frag", "component", ComponentEditorPlugin.this);

            FormComponent fc;

            //Readonly name widget
            fc = new RequiredTextField("name");
            fc.setOutputMarkupId(true);
            fc.add(new NodeUniqueValidator<Component>(ComponentEditorPlugin.this));
            fc.setEnabled(false);
            add(fc);

            //Component Classname widget
            fc = new TextField("componentClassName");
            fc.setOutputMarkupId(true);
            add(fc);

            //Server resource path widget
            fc = new TextField("serverResourcePath");
            fc.setOutputMarkupId(true);
            add(fc);

            //Choose template widget
            List<String> templates = hstContext.template.getTemplatesAsList();

            final String originalTemplate = bean.getTemplate();
            final DropDownChoice dc = new DropDownChoice("template", templates);
            dc.setNullValid(false);
            dc.setRequired(true);
            dc.setOutputMarkupId(true);
            dc.add(new AjaxFormComponentUpdatingBehavior("onChange") {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    if (!bean.getTemplate().equals(originalTemplate)) {
                        info("Your template has changed, saving this session will create new child components as specified by the templates containers. Existing child components will be removed if they don't match the container names.");
                    }
                }
            });
            add(dc);
        }

    }

    @Override
    protected EditorDAO<Component> newDAO() {
        return new ComponentDAO(getPluginContext(), getPluginConfig());
    }

    @Override
    protected Dialog newAddDialog() {
        return new AddComponentDialog(dao, this, (JcrNodeModel) getModel());
    }

}
