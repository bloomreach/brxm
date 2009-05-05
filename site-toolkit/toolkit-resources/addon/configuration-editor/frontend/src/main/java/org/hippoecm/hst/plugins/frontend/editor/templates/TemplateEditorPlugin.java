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

package org.hippoecm.hst.plugins.frontend.editor.templates;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.hippoecm.frontend.dialog.IDialogService.Dialog;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.hst.plugins.frontend.editor.BasicEditorPlugin;
import org.hippoecm.hst.plugins.frontend.editor.dao.EditorDAO;
import org.hippoecm.hst.plugins.frontend.editor.dao.TemplateDAO;
import org.hippoecm.hst.plugins.frontend.editor.domain.Template;
import org.hippoecm.hst.plugins.frontend.editor.validators.NodeUniqueValidator;

public class TemplateEditorPlugin extends BasicEditorPlugin<Template> {
    private static final long serialVersionUID = 1L;

    public TemplateEditorPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        //Readonly name widget
        FormComponent fc = new RequiredTextField("name");
        fc.setOutputMarkupId(true);
        fc.add(new NodeUniqueValidator<Template>(this));
        fc.setEnabled(false);
        form.add(fc);

        //Render path widget
        fc = new RequiredTextField("renderPath");
        fc.setOutputMarkupId(true);
        form.add(fc);

        //Containers widget
        ListView containers = new ListView("containers") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem item) {
                RequiredTextField rtf = new RequiredTextField("containerName", item.getModel());
                rtf.setOutputMarkupId(true);
                item.add(rtf);

                AjaxSubmitLink remove = new AjaxSubmitLink("removeContainer") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form form) {
                        bean.removeContainer(item.getIndex());
                        redraw();
                    }
                };
                remove.setDefaultFormProcessing(false);
                item.add(remove);
            }

        };
        containers.setReuseItems(true);
        form.add(containers);

        AjaxSubmitLink add = new AjaxSubmitLink("addContainer") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                bean.addContainer();
                redraw();
            }
        };
        add.setDefaultFormProcessing(false);
        form.add(add);
    }

    @Override
    protected EditorDAO<Template> newDAO() {
        return new TemplateDAO(getPluginContext(), getPluginConfig());
    }

    @Override
    protected Dialog newAddDialog() {
        JcrNodeModel model = (JcrNodeModel) getModel();
        return new AddTemplateDialog(dao, this, model.getParentModel());
    }

}
