/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

/**
 * Renders a view of a rich text field. Subclasses must call {@link #addView} once with the view model to render.
 */
abstract class AbstractRichTextViewPanel extends Panel {

    protected static final String WICKET_ID_VIEW = "view";

    public AbstractRichTextViewPanel(final String id) {
        super(id);
    }

    protected void addView(IModel<String> model) {
        final HtmlContainer view = new HtmlContainer(WICKET_ID_VIEW, model);
        this.add(view);
    }

    private static class HtmlContainer extends WebMarkupContainer {

        private static final long serialVersionUID = 1L;

        HtmlContainer(final String id, final IModel<String> viewModel) {
            super(id, viewModel);
        }

        @Override
        public void onComponentTagBody(final MarkupStream markupStream, final ComponentTag openTag) {
            Object modelObject = getDefaultModelObject();
            final String text = modelObject != null ? modelObject.toString() : null;
            if (text != null) {
                replaceComponentTagBody(markupStream, openTag, text);
            } else {
                super.onComponentTagBody(markupStream, openTag);
            }
        }
    }


}
