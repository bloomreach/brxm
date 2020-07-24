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

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.l10n.ResourceBundleModel;
import org.hippoecm.hst.platform.api.experiencepages.XPageLayout;

public class XPageLayoutContainer extends WebMarkupContainer {

    public static final String HIPPO_CMS_XPAGES = "hippo:cms.xpage";

    public XPageLayoutContainer(final String id, final IModel<XPageLayout> xPageLayoutModel
            , final IModel<List<XPageLayout>> xPageLayoutListModel) {
        super(id, xPageLayoutModel);
        final IModel<String> xPageLayoutLabelModel = ResourceBundleModel.of(HIPPO_CMS_XPAGES, "x-page-layout");
        final Label xPageLayoutLabel = new Label("xpage-layout-label", xPageLayoutLabelModel);
        add(xPageLayoutLabel);
        final DropDownChoice<XPageLayout> pageLayoutDropDownChoice = new DropDownChoice<>(
                "xpage-layout-drop-down-choice",
                xPageLayoutModel,
                xPageLayoutListModel,
                choiceRenderer);
        pageLayoutDropDownChoice.setRequired(true);
        pageLayoutDropDownChoice.setNullValid(false);
        pageLayoutDropDownChoice.setLabel(xPageLayoutLabelModel);
        add(pageLayoutDropDownChoice);
        setVisible(!xPageLayoutListModel.getObject().isEmpty());
    }

    IChoiceRenderer<XPageLayout> choiceRenderer =
            new IChoiceRenderer<XPageLayout>() {
                @Override
                public String getDisplayValue(final XPageLayout object) {
                    return object.getLabel();
                }

                @Override
                public String getIdValue(final XPageLayout object, final int index) {
                    return object.getKey();
                }

                @Override
                public XPageLayout getObject(final String id, final IModel<? extends List<? extends XPageLayout>> choices) {
                    return choices.getObject().
                            stream().
                            filter(xPageLayout -> xPageLayout.getKey().equals(id)).findFirst().
                            orElse(null);
                }
            };

}
