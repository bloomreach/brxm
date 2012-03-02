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
package org.hippoecm.frontend.plugins.cms.admin.widgets;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

/**
 * The AjaxLinkLabelContainer is a container for an AjaxFallBackLink,
 * with attached to this link a label. Because it is a container a getter
 * is exposed to public in order to get the AjaxFallbackLink
 */
public abstract class AjaxLinkLabelContainer extends WebMarkupContainer{

    private AjaxFallbackLink ajaxFallbackLink;

    public AjaxLinkLabelContainer(final String id, final IModel labelTextModel) {
        super(id, labelTextModel);
        ajaxFallbackLink = new AjaxFallbackLink("link") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                AjaxLinkLabelContainer.this.onClick(target);
            }
        };
        add(ajaxFallbackLink);
        ajaxFallbackLink.add(new Label("label", labelTextModel));
    }

    public abstract void onClick(AjaxRequestTarget target);

    public AjaxFallbackLink getAjaxFallbackLink() {
        return ajaxFallbackLink;
    }
}
