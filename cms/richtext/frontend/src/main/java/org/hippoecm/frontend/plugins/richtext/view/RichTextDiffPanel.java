/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugins.richtext.view;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugins.richtext.StripScriptModel;
import org.hippoecm.frontend.plugins.standards.diff.HtmlDiffModel;

/**
 * Renders the difference between two rich text fields, including images and clickable links that open the referring
 * document. Elements that have been added to or removed from the base version of the model are marked with green and
 * red, respectively.
 */
public class RichTextDiffPanel extends AbstractRichTextDiffPanel {

    public RichTextDiffPanel(final String id,
                             final IModel<String> baseModel,
                             final IModel<String> currentModel) {
        super(id);

        final IModel<String> diffModel = createDiffModel(baseModel, currentModel);
        addView(diffModel);
    }

    private IModel<String> createDiffModel(final IModel<String> baseModel,
                                           final IModel<String> currentModel) {

        return new HtmlDiffModel(new StripScriptModel(baseModel), new StripScriptModel(currentModel));
    }

}
