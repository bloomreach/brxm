/*
 *  Copyright 2011-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.perspectives.common;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.internal.HtmlHeaderContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.resource.CssResourceReference;

public class ErrorMessagePanel extends Panel {

    private static final long serialVersionUID = 1L;
    private static final CssResourceReference ERROR_MESSAGE_STYLESHEET = new CssResourceReference(ErrorMessagePanel.class, "ErrorMessagePanel.css");

    public ErrorMessagePanel(String id) {
        this(id, null);
    }

    public ErrorMessagePanel(String id, IModel<String> resourceModel) {
        super(id);

        if (resourceModel != null) {
            add(new Label("siteStatusLabel", resourceModel));
        } else {
            add(new Label("siteStatusLabel", new ResourceModel("error.status.message", "This perspective could not be loaded. Please contact your systems administrator.")));
        }
    }

    @Override
    public void internalRenderHead(final HtmlHeaderContainer container) {
        container.getHeaderResponse().render(CssHeaderItem.forReference(ERROR_MESSAGE_STYLESHEET));
    }

}
