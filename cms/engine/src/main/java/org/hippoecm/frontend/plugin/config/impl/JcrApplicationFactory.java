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
package org.hippoecm.frontend.plugin.config.impl;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IDetachable;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.config.IPluginConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrApplicationFactory implements IApplicationFactory, IDetachable {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(JcrApplicationFactory.class);

    private JcrNodeModel nodeModel;

    public JcrApplicationFactory(JcrNodeModel model) {
        this.nodeModel = model;
    }

    public IPluginConfigService getDefaultApplication() {
        try {
            Node applicationFolder = nodeModel.getNode();
            if (applicationFolder != null && applicationFolder.getNodes().hasNext()) {
                return getApplication(applicationFolder.getNodes().nextNode());
            } else {
                log.warn("No application nodes found");
            }
        } catch (RepositoryException e) {
            log.error("Failed to retrieve default application", e);
        }
        return null;
    }

    public IPluginConfigService getApplication(String name) {
        log.info("Starting application: " + name);
        try {
            Node applicationFolder = nodeModel.getNode();
            if (applicationFolder != null && applicationFolder.hasNode(name)) {
                return getApplication(applicationFolder.getNode(name));
            } else {
                log.info("No application {} found", name);
            }
        } catch (RepositoryException e) {
            log.error("Failed to retrieve application", e);
        }
        return null;
    }

    private IPluginConfigService getApplication(Node applicationNode) throws RepositoryException {
        if (applicationNode.hasNodes()) {
            Node clusterNode = applicationNode.getNodes().nextNode();
            return new JcrConfigServiceFactory(new JcrNodeModel(applicationNode), clusterNode.getName());
        } else {
            log.error("Application configuration '{}' contains no plugin cluster", applicationNode.getName());
        }
        return null;
    }

    public void detach() {
        nodeModel.detach();
    }
}
