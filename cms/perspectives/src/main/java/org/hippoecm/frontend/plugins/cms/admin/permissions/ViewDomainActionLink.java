/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.cms.admin.permissions;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.plugins.cms.admin.AdminBreadCrumbPanel;
import org.hippoecm.frontend.plugins.cms.admin.domains.Domain;

/**
 */
public class ViewDomainActionLink extends AjaxLink<Domain> {

    private final AdminBreadCrumbPanel panelToReplace;
    private final Domain domain;

    public ViewDomainActionLink(final String id, final AdminBreadCrumbPanel panelToReplace,
                                final IModel<Domain> domainIModel, final IModel<String> displayText) {
        super(id, domainIModel);
        this.panelToReplace = panelToReplace;
        this.domain = domainIModel.getObject();

        Label label = new Label("label", displayText);
        label.setRenderBodyOnly(true);
        add(label);
    }

    @Override
    public void onClick(final AjaxRequestTarget target) {
        /**
         * Get the originating parent to be able to do a replace of the panel.
         */
        final IBreadCrumbModel breadCrumbModel = panelToReplace.getBreadCrumbModel();
        SetPermissionsPanel setPermissionsPanel = new SetPermissionsPanel(
                panelToReplace.getId(), breadCrumbModel, new Model<Domain>(domain)
        );
        panelToReplace.replaceWith(setPermissionsPanel);
        // Reset the reference
        target.addComponent(setPermissionsPanel);
        panelToReplace.activate(setPermissionsPanel);
    }
}
