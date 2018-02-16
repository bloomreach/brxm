/*
 *  Copyright 2015-2018 Hippo B.V. (http://www.onehippo.com)
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

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClass;

public abstract class BreadcrumbWidget<T> extends GenericPanel<List<IModel<T>>> {

    private static final String CRUMB_ID = "crumb";

    private int maxNumberOfItems = 10;

    public BreadcrumbWidget(final String id, final IModel<List<IModel<T>>> model) {
        super(id, model);

        setOutputMarkupId(true);
        add(CssClass.append("hippo-breadcrumbs"));

        final BreadcrumbModel breadcrumbModel = new BreadcrumbModel();
        final ListView<Breadcrumb> breadCrumbs = new ListView<Breadcrumb>("crumbs", breadcrumbModel) {
            @Override
            protected void populateItem(final ListItem<Breadcrumb> item) {
                final Breadcrumb crumb = item.getModelObject();
                item.add(crumb);

                final String css = crumb.getCssClass();
                if (StringUtils.isNotEmpty(css)) {
                    item.add(CssClass.append(css));
                }
            }
        };
        breadCrumbs.setOutputMarkupId(true);
        add(breadCrumbs);
    }

    public void setMaxNumberOfItems(final int maxNumberOfItems) {
        this.maxNumberOfItems = maxNumberOfItems;
    }

    @Override
    protected void onDetach() {
        final List<IModel<T>> items = getItems();
        if (items != null) {
            items.forEach(IModel::detach);
        }
        super.onDetach();
    }

    protected List<IModel<T>> getItems() {
        return getModelObject();
    }

    protected Breadcrumb newLabel(final String id, final IModel<String> name, final IModel<T> model) {
        return new BreadcrumbLabel(id, name);
    }

    protected Breadcrumb newLink(final String id, final IModel<String> name, final IModel<T> model) {
        final OnClickHandler handler = target -> BreadcrumbWidget.this.onClick(model, target);
        return new BreadcrumbLink(id, name, handler);
    }

    protected Breadcrumb newSeparator(final String id) {
        return new Separator(id);
    }

    protected abstract void onClick(final IModel<T> model, final AjaxRequestTarget target);

    protected abstract IModel<String> getName(final IModel<T> model);

    protected interface OnClickHandler extends IClusterable {
        void onClick(AjaxRequestTarget target);
    }

    /**
     * The breadcrumb model returns a list of breadcrumb items that can be of three types: links, labels and separators.
     * By default, every entry in the original {@link #getItems} list is added as a link, apart from the last entry, which is added
     * as a label. All items have a separator breadcrumb item between them. This way overflow:hidden can be applied only
     * on the text in the breadcrumbs, leaving the separators visible at all times. This is required for IE10 and IE11
     * to get good text-clipping results.
     *
     * If the number of original items exceeds {@code maxNumberOfItems} the list will be clipped from the beginning.
     */
    private class BreadcrumbModel extends LoadableDetachableModel<List<Breadcrumb>> {
        @Override
        protected List<Breadcrumb> load() {
            final List<IModel<T>> items = getItems();
            final List<Breadcrumb> crumbsAndChevrons = new LinkedList<>();

            final int numberOfItems = items.size();
            final int delta = numberOfItems - maxNumberOfItems;
            final int startIndex = delta >= 0 ? delta : 0;

            for (int i = startIndex; i < numberOfItems; i++) {
                final IModel<T> model = items.get(i);
                final IModel<String> name = getName(model);
                if (i == numberOfItems - 1) {
                    // last item is a label
                    crumbsAndChevrons.add(newLabel(CRUMB_ID, name, model));
                } else {
                    crumbsAndChevrons.add(newLink(CRUMB_ID, name, model));
                    crumbsAndChevrons.add(newSeparator(CRUMB_ID));
                }
            }
            return crumbsAndChevrons;
        }
    }
}
