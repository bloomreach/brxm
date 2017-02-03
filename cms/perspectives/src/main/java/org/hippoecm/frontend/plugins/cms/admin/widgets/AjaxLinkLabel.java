/*
 *  Copyright 2008-2017 Hippo B.V. (http://www.onehippo.com)
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
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

public class AjaxLinkLabel extends Panel {

    public AjaxLinkLabel(final String id, final IModel<String> model) {
        super(id, model);

        final AjaxLink link = new AjaxLink("link") {
            @Override
            public void onClick(final AjaxRequestTarget target) {
                AjaxLinkLabel.this.onClick(target);
            }
        };
        link.add(new Label("label", model));
        add(link);
    }

    protected void onClick(final AjaxRequestTarget target) {
    }

}
