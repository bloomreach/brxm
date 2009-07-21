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
package org.hippoecm.frontend.editor.workflow.dialog;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.wizard.Wizard;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.util.string.AppendingStringBuffer;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.editor.layout.ILayoutProvider;
import org.hippoecm.frontend.editor.workflow.action.Action;

public abstract class CreateTypeDialog extends Wizard implements IDialogService.Dialog {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private Action action;
    private IDialogService dialogService;

    public CreateTypeDialog(Action action, ILayoutProvider layouts) {
        super("content", false);

        this.action = action;

        setOutputMarkupId(true);
    }

    @Override
    protected Component newButtonBar(String id) {
        MarkupContainer container = (MarkupContainer) super.newButtonBar(id);
        container.visitChildren(Button.class, new IVisitor() {

            public Object component(final Component component) {
                component.add(new AjaxEventBehavior("onclick") {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected CharSequence getEventHandler() {
                        return new AppendingStringBuffer(super.getEventHandler()).append("; return false;");
                    }

                    @Override
                    protected void onEvent(AjaxRequestTarget target) {
                        ((Button) component).onSubmit();
                        target.addComponent(CreateTypeDialog.this);
                    }

                });

                return IVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
            }

        });

        return container;
    }

    @Override
    public void onCancel() {
        dialogService.close();
    }

    @Override
    public void onFinish() {
        action.execute();
        dialogService.close();
    }

    public IValueMap getProperties() {
        return new ValueMap("width=500,height=325");
    }

    public Component getComponent() {
        return this;
    }

    public void onClose() {
    }

    public void render(PluginRequestTarget target) {
    }

    public void setDialogService(IDialogService service) {
        dialogService = service;
    }

}