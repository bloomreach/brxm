/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.hippoecm.frontend.plugins.standardworkflow.xpagelayout;

import java.util.List;
import java.util.Optional;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.model.SerializableSupplier;
import org.onehippo.cms7.services.hst.IXPageLayout;

public class XPageLayoutContainer extends WebMarkupContainer {

    public XPageLayoutContainer(final String id, final IModel<IXPageLayout> xPageLayoutModel
            , final SerializableSupplier<List<IXPageLayout>> xPageLayoutProvider) {
        super(id, xPageLayoutModel);
        final IModel<String> xPageLayoutLabelModel = new StringResourceModel("xpage-layout",
                this);
        final Label xPageLayoutLabel = new Label("xpage-layout-label", xPageLayoutLabelModel);
        add(xPageLayoutLabel);
        final XPageLayoutListModel XPageLayoutListModel = new XPageLayoutListModel(xPageLayoutProvider);
        final DropDownChoice<IXPageLayout> pageLayoutDropDownChoice = new DropDownChoice<>(
                "xpage-layout-drop-down-choice",
                xPageLayoutModel,
                XPageLayoutListModel,
                choiceRenderer);
        pageLayoutDropDownChoice.setRequired(true);
        pageLayoutDropDownChoice.setNullValid(false);
        pageLayoutDropDownChoice.setLabel(xPageLayoutLabelModel);
        add(pageLayoutDropDownChoice);
        setVisible(!xPageLayoutProvider.get().isEmpty());
    }

    IChoiceRenderer<IXPageLayout> choiceRenderer =
            new IChoiceRenderer<IXPageLayout>() {
                @Override
                public String getDisplayValue(final IXPageLayout object) {
                    return object.getLabel();
                }

                @Override
                public String getIdValue(final IXPageLayout object, final int index) {
                    return object.getKey();
                }

                @Override
                public IXPageLayout getObject(final String id, final IModel<? extends List<? extends IXPageLayout>> choices) {
                    final Optional<? extends IXPageLayout> first =
                            choices.getObject().stream().filter(iPageLayout -> iPageLayout.getKey().equals(id)).findFirst();
                    if (first.isPresent()) {
                        return first.get();
                    }
                    return null;
                }
            };

}
