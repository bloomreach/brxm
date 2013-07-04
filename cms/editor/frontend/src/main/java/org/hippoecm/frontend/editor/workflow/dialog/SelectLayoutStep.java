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
package org.hippoecm.frontend.editor.workflow.dialog;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.ResourceModel;
import org.hippoecm.frontend.editor.layout.ILayoutDescriptor;
import org.hippoecm.frontend.editor.layout.ILayoutProvider;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClassAppender;

public class SelectLayoutStep extends WizardStep {

    private static final long serialVersionUID = 1L;

    private final ILayoutProvider layoutProvider;
    private final IModel<String> layoutModel;

    public SelectLayoutStep(IModel<String> layoutModel, ILayoutProvider layouts) {
        super(new ResourceModel("select-layout-title"), new ResourceModel("select-layout-summary"));

        this.layoutModel = layoutModel;
        this.layoutProvider = layouts;

        setOutputMarkupId(true);

        add(new ListView<String>("layouts", (layoutProvider==null ? null : layoutProvider.getLayouts())) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<String> item) {
                final String layout = item.getModelObject();
                AjaxLink<Void> link = new AjaxLink<Void>("link") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        SelectLayoutStep.this.layoutModel.setObject(layout);
                        target.add(SelectLayoutStep.this);
                    }

                };
                ILayoutDescriptor descriptor = layoutProvider.getDescriptor(layout);
                link.add(new Image("preview", descriptor.getIcon()));
                link.add(new Label("layout", descriptor.getName()));
                item.add(link);

                item.add(new CssClassAppender(new LoadableDetachableModel<String>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected String load() {
                        if (layout.equals(SelectLayoutStep.this.layoutModel.getObject())) {
                            return "selected";
                        }
                        return null;
                    }
                }));
            }
        });
    }
}
