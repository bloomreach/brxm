/*
 *  Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.widgets.breadcrumb;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugins.standards.list.resolvers.TitleAttribute;
import org.hippoecm.frontend.widgets.breadcrumb.BreadcrumbWidget.OnClickHandler;

public class BreadcrumbLink extends Breadcrumb {

    public BreadcrumbLink(final String id, final IModel<String> name, final OnClickHandler onClickHandler) {
        super(id);

        setCssClass("breadcrumb-link");

        AjaxLink link = new AjaxLink("link") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                if (onClickHandler != null) {
                    onClickHandler.onClick(target);
                }
            }
        };
        link.add(new Label("name", name).setRenderBodyOnly(true));
        link.add(TitleAttribute.append(name));
        add(link);
    }
}
