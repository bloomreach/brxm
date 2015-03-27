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

import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClass;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.frontend.skin.Icon;

public abstract class BreadcrumbWidget<T> extends Panel {

    private int maxNumberOfCrumbs = 15;

    public BreadcrumbWidget(final String id, final BreadcrumbModel<T> model) {
        super(id, model);

        setOutputMarkupId(true);
        add(CssClass.append("hippo-breadcrumbs"));

        ListView<Breadcrumb<T>> breadCrumbs = new ListView<Breadcrumb<T>>("crumbs", model) {

            @Override
            protected void populateItem(final ListItem<Breadcrumb<T>> item) {

                Breadcrumb<T> crumb = item.getModelObject();
                int numberOfCrumbs = model.getObject().size();
                boolean isFirst = item.getIndex() == 0;
                boolean isLast = item.getIndex() == (numberOfCrumbs - 1);

                AjaxLink<Breadcrumb<T>> link = new AjaxLink<Breadcrumb<T>>("link", item.getModel()) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        onClickBreadCrumb(crumb, target);
                    }
                };

                HippoIcon hippoIcon = HippoIcon.fromSprite("icon", Icon.CHEVRON_RIGHT, IconSize.M);
                hippoIcon.setVisible(!isLast);
                item.add(hippoIcon);

                final String name = crumb.getName();
                link.add(new Label("name", Model.of(name)));
                link.add(new AttributeAppender("title", name, " "));
                link.setEnabled(crumb.isEnabled());
                item.add(link);

                item.add(CssClass.append(crumb.isEnabled() ? "enabled" : "disabled"));
                if (isFirst) {
                    item.add(CssClass.append("first"));
                }
                if (isLast) {
                    item.add(CssClass.append("last"));
                }

                //CMS7-8008: Just show the last part of the breadcrumb
                item.setVisible(item.getIndex() >= (numberOfCrumbs - maxNumberOfCrumbs));
            }
        };
        breadCrumbs.setOutputMarkupId(true);
        add(breadCrumbs);
    }

    protected abstract void onClickBreadCrumb(final Breadcrumb<T> crumb, final AjaxRequestTarget target);

    @Override
    protected void onDetach() {
        List<Breadcrumb<T>> items = getItems();
        if (items != null) {
            items.forEach(Breadcrumb::detach);
        }
        super.onDetach();
    }

    @SuppressWarnings("unchecked")
    protected List<Breadcrumb<T>> getItems() {
        return (List<Breadcrumb<T>>) getDefaultModelObject();
    }
}
