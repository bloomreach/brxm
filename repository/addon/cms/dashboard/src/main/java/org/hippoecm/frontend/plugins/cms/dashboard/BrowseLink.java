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
package org.hippoecm.frontend.plugins.cms.dashboard;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IViewService;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.ISO9075Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrowseLink extends Panel {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(BrowseLink.class);

    public BrowseLink(final IPluginContext context, final IPluginConfig config, String id, String docPath) {
        super(id, new Model(docPath));

        final JcrNodeModel variant = new JcrNodeModel(docPath);
        AjaxLink link = new AjaxLink("link", variant) {
            private static final long serialVersionUID = 1L;
            @Override
            public void onClick(AjaxRequestTarget target) {
                String linkTo = config.getString("link.to");
                IViewService viewService = context.getService(linkTo, IViewService.class);
                viewService.view(variant);

            }
        };
        add(link);

        try {
            Node handleNode = variant.getNode();
            while (!handleNode.isNodeType(HippoNodeType.NT_HANDLE) && !handleNode.getPath().equals("/")) {
                handleNode = handleNode.getParent();
            }
            String path = handleNode.getPath();
            path = ISO9075Helper.decodeLocalName(path);
            link.add(new Label("label", path));
        } catch (RepositoryException e) {
            log.error(e.getMessage(), e);
            link.add(new Label("label", e.getMessage()));
        }
    }

}
