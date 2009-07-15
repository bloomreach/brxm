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
import org.apache.wicket.Resource;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.wizard.Wizard;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.string.AppendingStringBuffer;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.editor.layout.ILayoutProvider;
import org.hippoecm.frontend.editor.workflow.RemodelWorkflowPlugin;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClassAppender;
import org.hippoecm.frontend.widgets.TextFieldWidget;

public class CreateCompoundTypeDialog extends Wizard implements IDialogService.Dialog {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    class TypeDetailStep extends WizardStep {
        private static final long serialVersionUID = 1L;

        TypeDetailStep(RemodelWorkflowPlugin.NewCompoundTypeAction action) {
            super(new ResourceModel("type-detail-title"), new ResourceModel("type-detail-summary"));
            add(new TextFieldWidget("name", new PropertyModel(action, "name")));
        }
    }

    class SelectLayoutStep extends WizardStep {
        private static final long serialVersionUID = 1L;

        SelectLayoutStep(final RemodelWorkflowPlugin.NewCompoundTypeAction action) {
            super(new ResourceModel("select-layout-title"), new ResourceModel("select-layout-summary"));

            setOutputMarkupId(true);

            add(new ListView("layouts", layoutProvider.getLayouts()) {

                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(ListItem item) {
                    final String layout = item.getModelObjectAsString();
                    AjaxLink link = new AjaxLink("link") {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            action.layout = layout;
                            target.addComponent(SelectLayoutStep.this);
                        }

                    };
                    link.add(new Image("preview", new Resource() {

                        private static final long serialVersionUID = 1L;

                        @Override
                        public IResourceStream getResourceStream() {
                            return layoutProvider.getDescriptor(layout).getIcon();
                        }

                    }));
                    String name = layout.substring(layout.lastIndexOf('.') + 1);
                    link.add(new Label("layout", name));
                    link.add(new CssClassAppender(new Model() {

                        private static final long serialVersionUID = 1L;
                        
                        @Override
                        public Object getObject() {
                            if (layout.equals(action.layout)) {
                                return "selected";
                            }
                            return null;
                        }
                    }));
                    item.add(link);
                }
            });
        }
    }

    private RemodelWorkflowPlugin.NewCompoundTypeAction action;
    private IDialogService dialogService;
    private ILayoutProvider layoutProvider;

    public CreateCompoundTypeDialog(RemodelWorkflowPlugin.NewCompoundTypeAction action, ILayoutProvider layouts) {
        super("content");

        this.action = action;
        this.layoutProvider = layouts;

        setOutputMarkupId(true);

        WizardModel wizardModel = new WizardModel();
        wizardModel.add(new TypeDetailStep(action));
        wizardModel.add(new SelectLayoutStep(action));
        init(wizardModel);
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
                        target.addComponent(CreateCompoundTypeDialog.this);
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

    public IModel getTitle() {
        return new StringResourceModel("new-compound-type", this, null);
    }

    public IValueMap getProperties() {
        return new ValueMap("width=380,height=250");
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