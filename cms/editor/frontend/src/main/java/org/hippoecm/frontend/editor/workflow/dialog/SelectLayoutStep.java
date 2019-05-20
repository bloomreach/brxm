/*
 *  Copyright 2008-2019 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.validation.IFormValidator;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.ResourceModel;
import org.hippoecm.frontend.editor.layout.ILayoutDescriptor;
import org.hippoecm.frontend.editor.layout.ILayoutProvider;
import org.hippoecm.frontend.plugins.standards.image.CachingImage;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClass;

public class SelectLayoutStep extends WizardStep {

    private final ILayoutProvider layoutProvider;

    public SelectLayoutStep(final IModel<String> layoutModel, final ILayoutProvider layouts) {
        super(new ResourceModel("select-layout-title"), null);

        this.layoutProvider = layouts;

        setOutputMarkupId(true);

        add(new IFormValidator() {
            @Override
            public FormComponent<?>[] getDependentFormComponents() {
                return new FormComponent<?>[0];
            }

            @Override
            public void validate(final Form<?> form) {
                if (layoutModel.getObject() == null) {
                    error(getString("layoutstep.selection.empty"));
                } else {
                    clearErrors();
                }
            }
        });

        add(new ListView<String>("layouts", (layoutProvider == null ? null : layoutProvider.getLayouts())) {

            @Override
            protected void populateItem(ListItem<String> item) {
                final String layout = item.getModelObject();
                AjaxLink<Void> link = new AjaxLink<Void>("link") {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        clearErrors();
                        layoutModel.setObject(layout);
                        target.add(SelectLayoutStep.this);
                    }
                };
                ILayoutDescriptor descriptor = layoutProvider.getDescriptor(layout);
                link.add(new CachingImage("preview", descriptor.getIcon()));
                link.add(new Label("layout", descriptor.getName()));
                item.add(link);

                item.add(CssClass.append((new LoadableDetachableModel<String>() {
                    @Override
                    protected String load() {
                        return layout.equals(layoutModel.getObject()) ? "selected" : StringUtils.EMPTY;
                    }
                })));
            }
        });
    }

    private void clearErrors() {
        getFeedbackMessages().clear(FeedbackMessage::isError);
    }
}
