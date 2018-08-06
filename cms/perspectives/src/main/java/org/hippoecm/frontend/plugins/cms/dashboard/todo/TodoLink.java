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
package org.hippoecm.frontend.plugins.cms.dashboard.todo;

import java.text.DateFormat;
import java.util.Calendar;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.cms.dashboard.BrowseLinkTarget;
import org.hippoecm.frontend.plugins.standards.list.resolvers.TitleAttribute;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TodoLink extends Panel {

    private static final Logger log = LoggerFactory.getLogger(TodoLink.class);

    public TodoLink(final IPluginContext context, final IPluginConfig config,
                    final String id, final BrowseLinkTarget browseLinkTarget,
                    final IModel<String> userLabelModel, final IModel<String> operationLabelModel,
                    final IModel<Calendar> creationDateModel) {
        super(id);

        AjaxLink<Void> link = new AjaxLink<Void>("link") {

            @SuppressWarnings("unchecked")
            @Override
            public void onClick(AjaxRequestTarget target) {
                final JcrNodeModel nodeModel = browseLinkTarget.getBrowseModel();
                final String browserId = config.getString("browser.id");
                final IBrowseService browseService = context.getService(browserId, IBrowseService.class);
                if (browseService != null) {
                    browseService.browse(nodeModel);
                } else {
                    log.warn("no browse service found with id '{}', cannot browse to '{}'", browserId,
                            JcrUtils.getNodePathQuietly(nodeModel.getNode()));
                }
            }

            @Override
            public boolean isEnabled() {
                return browseLinkTarget.getBrowseModel() != null;
            }
        };
        add(link);
        link.add(TitleAttribute.set(new PropertyModel<>(browseLinkTarget, "displayPath")));

        Label userLabel = new Label("username", userLabelModel);
        link.add(userLabel);

        Label operationLabel = new Label("operation", operationLabelModel);
        operationLabel.setEscapeModelStrings(false);
        link.add(operationLabel);

        // Set the creation date or an empty string when no creation date is available
        final DateFormat dateFormatShort = DateFormat.getDateInstance(DateFormat.MEDIUM,
                getSession().getLocale());
        final String creationDate = creationDateModel != null && creationDateModel.getObject() != null ?
                dateFormatShort.format(creationDateModel.getObject().getTime()) : StringUtils.EMPTY;
        link.add(new Label("creationDate", new Model<>(creationDate)));

        Label documentLabel = new Label("document", "\"" + browseLinkTarget.getName() + "\"");
        link.add(documentLabel);
    }

}
