/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.frontend.translation;

import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.DataGridView;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.html.PackageResource;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.resources.PackagedResourceReference;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.string.Strings;
import org.apache.wicket.util.time.Duration;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin.WorkflowAction;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClassAppender;
import org.hippoecm.frontend.widgets.TextFieldWidget;

public final class TranslateDocumentDialog extends WorkflowAction.WorkflowDialog {
    private static final long serialVersionUID = 1L;

    private final TranslationWorkflowPlugin translationWorkflowPlugin;

    private IModel<String> title;
    private TextField<String> nameComponent;
    private TextField<String> uriComponent;
    private boolean uriModified;
    private List<FolderTranslation> folders;

    public TranslateDocumentDialog(TranslationWorkflowPlugin translationWorkflowPlugin, WorkflowAction action,
            IModel<String> title, List<FolderTranslation> folders) {
        action.super();
        this.translationWorkflowPlugin = translationWorkflowPlugin;
        this.title = title;
        this.folders = folders;

        final PropertyModel<String> nameModel = new PropertyModel<String>(action, "name");
        final PropertyModel<String> urlModel = new PropertyModel<String>(action, "url");

        String s1 = nameModel.getObject();
        String s2 = urlModel.getObject();
        uriModified = (s1 != s2) && (s1 == null || !s1.equals(s2));

        add(CSSPackageResource.getHeaderContribution(TranslateDocumentDialog.class, "translation-style.css"));
        
        nameComponent = new TextField<String>("name", nameModel);
        nameComponent.setRequired(true);
        nameComponent.setLabel(new StringResourceModel("name-label", this.translationWorkflowPlugin, null));
        nameComponent.add(new OnChangeAjaxBehavior() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                if (!uriModified) {
                    urlModel.setObject(TranslateDocumentDialog.this.translationWorkflowPlugin.getNodeNameCodec()
                            .encode(nameModel.getObject()));
                    target.addComponent(uriComponent);
                }
            }
        }.setThrottleDelay(Duration.milliseconds(500)));
        nameComponent.setOutputMarkupId(true);
        setFocus(nameComponent);
        add(nameComponent);

        add(uriComponent = new TextField<String>("uriinput", urlModel) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled() {
                return uriModified;
            }
        });

        uriComponent.add(new CssClassAppender(new AbstractReadOnlyModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                return uriModified ? "grayedin" : "grayedout";
            }
        }));
        uriComponent.setOutputMarkupId(true);

        AjaxLink<Boolean> uriAction = new AjaxLink<Boolean>("uriAction") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                uriModified = !uriModified;
                if (!uriModified) {
                    urlModel.setObject(Strings.isEmpty(nameModel.getObject()) ? ""
                            : TranslateDocumentDialog.this.translationWorkflowPlugin.getNodeNameCodec().encode(
                                    nameModel.getObject()));
                }
                target.addComponent(TranslateDocumentDialog.this.translationWorkflowPlugin);
            }
        };
        uriAction.add(new Label("uriActionLabel", new AbstractReadOnlyModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                return uriModified ? getString("url-reset") : getString("url-edit");
            }
        }));
        add(uriAction);

        ICellPopulator<FolderTranslation>[] populators = new ICellPopulator[2];
        populators[0] = new AbstractColumn<FolderTranslation>(new StringResourceModel("name", this, null)) {
            private static final long serialVersionUID = 1L;

            public void populateItem(Item<ICellPopulator<FolderTranslation>> cellItem, String componentId,
                    IModel<FolderTranslation> rowModel) {
                FolderTranslation ft = rowModel.getObject();
                IModel<String> nameModel = new PropertyModel<String>(rowModel, "name");
                if (ft.isMutable()) {
                    cellItem.add(new TextFieldWidget(componentId, nameModel));
                } else {
                    cellItem.add(new Label(componentId, nameModel));
                }
            }

        };
        populators[1] = new AbstractColumn<FolderTranslation>(new StringResourceModel("url", this, null)) {
            private static final long serialVersionUID = 1L;

            public void populateItem(Item<ICellPopulator<FolderTranslation>> cellItem, String componentId,
                    IModel<FolderTranslation> rowModel) {
                FolderTranslation ft = rowModel.getObject();
                IModel<String> urlModel = new PropertyModel<String>(rowModel, "url");
                if (ft.isMutable()) {
                    cellItem.add(new TextFieldWidget(componentId, urlModel));
                } else {
                    cellItem.add(new Label(componentId, urlModel));
                }
            }

        };
        add(new DataGridView<FolderTranslation>("folders", populators, new ListDataProvider<FolderTranslation>(folders)));
    }

    @Override
    protected void onDetach() {
        for (FolderTranslation ft : folders) {
            ft.detach();
        }
        super.onDetach();
    }

    @Override
    public IModel<String> getTitle() {
        return title;
    }

    @Override
    public IValueMap getProperties() {
        return new ValueMap("width=675,height=450").makeImmutable();
    }
}