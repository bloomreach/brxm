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

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.i18n.model.NodeTranslator;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.NodeNameCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrowseLink extends Panel {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(BrowseLink.class);

    private String label = null;
    
    public BrowseLink(final IPluginContext context, final IPluginConfig config, String id, String docPath) {
        super(id, new Model(docPath));
        this.label = null;
        addLink(context, config, id, docPath);        
    }

    public BrowseLink(final IPluginContext context, final IPluginConfig config, String id, String docPath, String label) {
        super(id, new Model(docPath));
        this.label = label;
        addLink(context, config, id, docPath);        
    }

    private void addLink(final IPluginContext context, final IPluginConfig config, String id, String docPath) {
        final JcrNodeModel nodeModel = new JcrNodeModel(docPath);
        AjaxLink link = new AjaxLink("link", nodeModel) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                String browserId = config.getString("browser.id");
                IBrowseService browseService = context.getService(browserId, IBrowseService.class);
                if (browseService != null) {
                    browseService.browse(nodeModel);
                } else {
                    log.warn("no browser service found");
                }

                IRenderService browserRenderer = context.getService(browserId, IRenderService.class);
                if (browserRenderer != null) {
                    browserRenderer.focus(null);
                } else {
                    log.warn("no focus service found");
                }
            }
        };
        add(link);
    
        try {
            Node node = nodeModel.getNode();
            while (!node.isNodeType(HippoNodeType.NT_HANDLE) &&
                   !node.isNodeType("hippostd:folder") &&
                   !node.getPath().equals("/")) {
                node = node.getParent();
            }
            String[] elements = StringUtils.split(node.getPath(), '/');
            for (int i = 0; i < elements.length; i++) {
                elements[i] = NodeNameCodec.decode(elements[i]);
            }
            String path = StringUtils.join(elements, '/');
    
            if (label == null) {
                label = (String) new NodeTranslator(new JcrNodeModel(node)).getNodeName().getObject();
            }
            
            Label linkLabel = new Label("label", label);
            linkLabel.setEscapeModelStrings(false);
            link.add(linkLabel);
            link.add(new SimpleAttributeModifier("title", path));
    
        } catch (RepositoryException e) {
            log.error(e.getMessage(), e);
            link.add(new Label("label", e.getMessage()));
        }
    }
}
