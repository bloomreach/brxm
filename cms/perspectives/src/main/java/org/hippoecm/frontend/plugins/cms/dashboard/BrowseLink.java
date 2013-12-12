/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.cms.dashboard;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrowseLink extends Panel {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(BrowseLink.class);

    public BrowseLink(final IPluginContext context, final IPluginConfig config, String id,
            final BrowseLinkTarget browseLinkTarget, IModel<String> labelModel) {
        super(id);

        AjaxLink<Void> link = new AjaxLink<Void>("link") {
            private static final long serialVersionUID = 1L;

            @SuppressWarnings("unchecked")
            @Override
            public void onClick(AjaxRequestTarget target) {
                final JcrNodeModel nodeModel = browseLinkTarget.getBrowseModel();
                final String browserId = config.getString("browser.id");
                final IBrowseService browseService = context.getService(browserId, IBrowseService.class);
                if (browseService != null) {
                    browseService.browse(nodeModel);
                } else {
                    log.warn("no browse service found with id '{}', cannot browse to '{}'", browserId, JcrUtils.getNodePathQuietly(nodeModel.getNode()));
                }
            }

            @Override
            public boolean isEnabled() {
                return browseLinkTarget.getBrowseModel() != null;
            }
        };
        add(link);

        Label linkLabel = new Label("label", labelModel);
        linkLabel.setEscapeModelStrings(false);
        link.add(linkLabel);

        link.add(new AttributeModifier("title", true, new PropertyModel(browseLinkTarget, "displayPath")));
    }


}
