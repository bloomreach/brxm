/*
 *  Copyright 2009-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.workflow.action;

import org.apache.wicket.Component;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.editor.NamespaceValidator;
import org.hippoecm.editor.repository.NamespaceWorkflow;
import org.hippoecm.editor.template.JcrTemplateStore;
import org.hippoecm.editor.type.JcrDraftLocator;
import org.hippoecm.frontend.dialog.IDialogService.Dialog;
import org.hippoecm.frontend.editor.layout.ILayoutProvider;
import org.hippoecm.frontend.editor.workflow.NamespaceWorkflowPlugin;
import org.hippoecm.frontend.editor.workflow.TemplateFactory;
import org.hippoecm.frontend.editor.workflow.dialog.CreateCompoundTypeDialog;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugins.standards.icon.HippoIconStack;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.frontend.skin.CmsIcon;
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.repository.api.Workflow;

public class NewCompoundTypeAction extends Action {

    private static final long serialVersionUID = 1L;

    private ILayoutProvider layoutProvider;

    public String name;
    public String layout;

    public NewCompoundTypeAction(NamespaceWorkflowPlugin plugin, ILayoutProvider layouts) {
        super(plugin, "new-compound-type", new StringResourceModel("new-compound-type", plugin));
        this.layoutProvider = layouts;
    }

    @Override
    protected Dialog createRequestDialog() {
        return new CreateCompoundTypeDialog(this, layoutProvider);
    }

    @Override
    protected String execute(Workflow wf) throws Exception {
        NamespaceValidator.checkName(name);

        if (layout == null) {
            throw new Exception("No layout specified");
        }

        // create type
        NamespaceWorkflow workflow = (NamespaceWorkflow) wf;
        workflow.addCompoundType(name);

        String prefix = (String) workflow.hints().get("prefix");

        // create layout
        // FIXME: should be managed by template engine
        final JcrDraftLocator typeStore = new JcrDraftLocator(prefix);
        JcrTemplateStore templateStore = new JcrTemplateStore(typeStore);
        IClusterConfig template = new TemplateFactory().createTemplate(layoutProvider.getDescriptor(layout));
        templateStore.save(template, typeStore.locate(prefix + ":" + name));

        openEditor(prefix + ":" + name);

        return null;
    }

    @Override
    protected Component getIcon(final String id) {
        HippoIconStack iconStack = new HippoIconStack(id, IconSize.M);
        iconStack.addFromSprite(Icon.FILE_COMPOUND);
        iconStack.addFromCms(CmsIcon.OVERLAY_PLUS, IconSize.M, HippoIconStack.Position.TOP_LEFT);
        return iconStack;
    }
}
