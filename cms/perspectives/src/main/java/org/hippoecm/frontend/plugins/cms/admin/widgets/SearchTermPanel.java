/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.validator.StringValidator;
import org.hippoecm.frontend.plugins.cms.widgets.SubmittingTextField;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.skin.Icon;

public class SearchTermPanel extends Panel {

    private String searchTerm = StringUtils.EMPTY;
    private boolean searchActive = false;

    protected SearchTermPanel(final String id) {
        super(id);

        final Form form = new Form("search-form");
        form.setOutputMarkupId(true);
        add(form);

        final TextField<String> search = new SubmittingTextField("search-query", new PropertyModel<>(this, "searchTerm")) {
            @Override
            public void onEnter(final AjaxRequestTarget target) {
                super.onEnter(target);
                processSubmit(target, form, searchTerm);
            }
        };

        search.add(StringValidator.minimumLength(1));
        search.setRequired(false);
        search.add(new DefaultFocusBehavior());
        form.add(search);

        final AjaxSubmitLink browseLink = new AjaxSubmitLink("toggle") {
            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                // when searchActive this link is the remove link
                if (searchActive) {
                    searchTerm = StringUtils.EMPTY;
                }
                processSubmit(target, form, searchTerm);
            }
        };
        browseLink.add(createSearchIcon());
        form.add(browseLink);
    }

    public void processSubmit(final AjaxRequestTarget target, final Form<?> form, final String searchTerm) {
        searchActive = StringUtils.isNotBlank(searchTerm);
        target.add(form);
    }

    private Component createSearchIcon() {
        final IModel<Icon> iconModel = new LoadableDetachableModel<Icon>() {
            @Override
            protected Icon load() {
                if (StringUtils.isNotBlank(searchTerm)) {
                    return Icon.TIMES;
                } else {
                    return Icon.SEARCH;
                }
            }
        };
        return HippoIcon.fromSprite("search-icon", iconModel);
    }
}
